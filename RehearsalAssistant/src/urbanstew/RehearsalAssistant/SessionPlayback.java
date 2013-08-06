/*
 *  Author:
 *      Stjepan Rajko
 *      urbanSTEW
 *
 *  Copyright 2008,2009 Stjepan Rajko.
 *
 *  This file is part of the Android version of Rehearsal Assistant.
 *
 *  Rehearsal Assistant is free software: you can redistribute it
 *  and/or modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 3 of the License,
 *  or (at your option) any later version.
 *
 *  Rehearsal Assistant is distributed in the hope that it will be
 *  useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Rehearsal Assistant.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package urbanstew.RehearsalAssistant;

import java.util.Date;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import urbanstew.RehearsalAssistant.Rehearsal.Annotations;
import urbanstew.RehearsalAssistant.Rehearsal.Sessions;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;

/** The RehearsalPlayback Activity provides playback access for
 * 	annotations in a particular project.
 */
public class SessionPlayback
{
	private static final int ANNOTATIONS_ID = 0;
	private static final int ANNOTATIONS_START_TIME = 1;
	private static final int ANNOTATIONS_END_TIME = 2;
	private static final int ANNOTATIONS_FILE_NAME = 3;
	private static final int ANNOTATIONS_LABEL = 4;
	private static final int ANNOTATIONS_VIEWED = 5;
	
	static final int SESSIONS_ID = 0;
	static final int SESSIONS_TITLE = 1;
	static final int SESSIONS_START_TIME = 2;
	static final int SESSIONS_END_TIME = 3;
	
    /** Called when the activity is first created. */
    public SessionPlayback(Bundle savedInstanceState, RehearsalActivity activity, Uri uri)
    {
    	mActivity = activity;
    	mActivity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    	
        String[] projection =
        {
        	Annotations._ID,
        	Annotations.START_TIME,
        	Annotations.END_TIME,
        	Annotations.FILE_NAME,
        	Annotations.LABEL,
        	Annotations.VIEWED
        };
        String[] sessionProjection =
        {
        	Sessions._ID,
        	Sessions.TITLE,
        	Sessions.START_TIME,
        	Sessions.END_TIME
        };

        String session_id = uri.getPathSegments().get(1);

        ContentResolver resolver = activity.getContentResolver();
        mSessionCursor = resolver.query(Sessions.CONTENT_URI, sessionProjection, Sessions._ID + "=" + session_id, null, Sessions.DEFAULT_SORT_ORDER);
        mSessionCursor.moveToFirst();
        
        if(mSessionCursor.getLong(SESSIONS_START_TIME) != 0)
        {
        	formatter = new SimpleDateFormat("HH:mm:ss");
        	formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        	mSessionTiming = true;
        }
        else
        {
        	formatter = DateFormat.getDateTimeInstance();
        	mSessionTiming = false;
        }
        
        mAnnotationsCursor = resolver.query(Annotations.CONTENT_URI, projection, Annotations.SESSION_ID + "=" + session_id + " AND " + Annotations.END_TIME + " IS NOT NULL", null,
                Annotations.DEFAULT_SORT_ORDER);
        Log.w("RehearsalAssistant", "Read " + mAnnotationsCursor.getCount() + " annotations.");

        mListAdapter = new SimpleCursorAdapter(activity.getApplication(), R.layout.annotationslist_item, mAnnotationsCursor,
                new String[] { Annotations.START_TIME, Annotations.LABEL }, new int[] { android.R.id.text1, android.R.id.text2 });
        
        mListAdapter.setViewBinder(new ViewBinder()
        {
			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex)
			{
				TextView v = (TextView)view;
				if(v.getId() == android.R.id.text2)
					return false;
				v.setText(formatter.format(new Date(cursor.getLong(ANNOTATIONS_START_TIME))),
						/*makeAnnotationText(cursor),*/ TextView.BufferType.SPANNABLE);
				if(cursor.getInt(ANNOTATIONS_VIEWED) == 0)
				{
					v.setTextAppearance(mActivity.getApplicationContext(), android.R.attr.textAppearanceLarge);
					Spannable str = (Spannable) v.getText();
					str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				return true;
			}
        });
        mListView = (IndicatingListView)mActivity.findViewById(R.id.annotation_list);
        mListView.setAdapter(mListAdapter);
        mListView.setOnItemClickListener(mSelectedListener);
        mListView.setOnCreateContextMenuListener(mCreateContextMenuListener);
        mAnnotationsCursor.registerContentObserver(new ContentObserver(mHandler)
        {
        	public void onChange(boolean selfChange)
        	{
        		updateListIndication();
        	}
        });

        AudioManager audioManager = (AudioManager) mActivity.getApplication().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0)
      		Toast.makeText(mActivity, R.string.warning_mute, Toast.LENGTH_LONG).show();
        
        mCurrentTime = (TextView) mActivity.findViewById(R.id.playback_time);
        mPlayTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		// construct playback dialog
        LayoutInflater factory = LayoutInflater.from(mActivity);
        View playbackView = factory.inflate(R.layout.alert_playback_dialog, null);
        mPlaybackDialog = new AlertDialog.Builder(mActivity)
            .setView(playbackView)
            .setPositiveButton
            (
            	"Close",
            	new DialogInterface.OnClickListener()
            	{
            		public void onClick(DialogInterface dialog, int whichButton)
            		{
            			if(mPlayer!=null)
            				mPlayer.stop();

            			onPlayItemLostFocus();
                	}
            	}
            )
            .setOnCancelListener(new DialogInterface.OnCancelListener()
            {
				public void onCancel(DialogInterface dialog)
				{
					onPlayItemLostFocus();					
				}
            })
            .create();
        mPlaybackDialog.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mPlayPauseButton = (ImageButton)playbackView.findViewById(R.id.playback_pause);
        mPlaybackCurrentTime = (TextView)playbackView.findViewById(R.id.playback_current_time);
        mPlaybackFileSize = (TextView)playbackView.findViewById(R.id.playback_file_size);
        mPlaybackDuration  = (TextView)playbackView.findViewById(R.id.playback_length);
        mSeekBar = (SeekBar)playbackView.findViewById(R.id.playback_seek);
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

			public void onProgressChanged(SeekBar bar, int progress, boolean fromUser)
			{
				if(fromUser && mPlayer != null)
						mPlayer.seekTo(mPlayer.getDuration() * progress / 1024);				
			}

			public void onStartTrackingTouch(SeekBar arg0)
			{
				if(mPlayer.isPlaying())
				{
					mPlayer.pause();
			    	setPlayPauseButton(android.R.drawable.ic_media_play);
				}
			}

			public void onStopTrackingTouch(SeekBar bar)
			{
				mPlayer.start();
		    	setPlayPauseButton(android.R.drawable.ic_media_pause);
			}
        
        });
        playbackView.findViewById(R.id.playback_pause).setOnClickListener
        (
        	new OnClickListener()
        	{
				public void onClick(View view)
				{
					if(mPlayer.isPlaying())
					{
						mPlayer.pause();
				    	setPlayPauseButton(android.R.drawable.ic_media_play);
					}
					else
					{
						mPlayer.start();
				    	setPlayPauseButton(android.R.drawable.ic_media_pause);
					}
				}
        	}
        );
        playbackView.findViewById(R.id.playback_next).setOnClickListener
        (
        	new OnClickListener()
        	{
				public void onClick(View view)
				{
					if(mAnnotationsCursor.getCount() > mPlayingPosition + 1)
						playItem(mPlayingPosition + 1);
				}
        	}
        );
        playbackView.findViewById(R.id.playback_previous).setOnClickListener
        (
        	new OnClickListener()
        	{
				public void onClick(View view)
				{
					if((mAnnotationsCursor.getCount() > mPlayingPosition - 1) && mPlayingPosition > 0)
						playItem(mPlayingPosition - 1);
				}
        	}
        );
        mOldTitle = mActivity.getTitle();
    }

    public void onPause()
    {
    	mCurrentTimeTask.cancel();
    }
    
    public void onResume()
    {
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
    	mPlaybackPanelEnabled = preferences.getBoolean("playback_panel_enabled", true);
    	mPlaybackPanelDisappears = preferences.getBoolean("playback_panel_disappears", false);
    	mEmailDetail = preferences.getBoolean("email_detail", true);
    	mEmailTo = preferences.getString("email_to", null);
    	mEmailSubject = preferences.getString("email_subject", null);
    	mConfirmIndividualDeletion = preferences.getBoolean("confirm_individual_deletion", true);
    	
    	mCurrentTimeTask = new TimerTask()
    	{
    		public void run()
    		{
    			mActivity.runOnUiThread(new Runnable()
    			{
    				public void run()
    				{
    					if(mPlayer != null && mPlayer.isPlaying())
    					{
    						updateProgressDisplay();
    					}
    				}
    			});                                
    		}
    	};
		mTimer.scheduleAtFixedRate(
				mCurrentTimeTask,
				0,
				100);
    }
    public void onDestroy()
    {
    	mTimer.cancel();
    	mAnnotationsCursor.close();
    	mSessionCursor.close();
    	if(mAnnotationLabelDialog != null && mAnnotationLabelDialog.isShowing())
    		mAnnotationLabelDialog.dismiss();
    }
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
		// restore label edit dialog if needed
		if(savedInstanceState.getBoolean("annotationLabelDialogShown"))
		{
			displayAnnotationLabelDialog
				(
					savedInstanceState.getString("annotationLabelDialogText"),
					savedInstanceState.getLong("annotationLabelDialogShownId")
				);
		}
    }

    protected void onSaveInstanceState(Bundle outState)
    {
    	if(mAnnotationLabelDialog != null && mAnnotationLabelDialog.isShowing())
    	{
    		outState.putBoolean("annotationLabelDialogShown", true);
    		outState.putString
    			(
    				"annotationLabelDialogText",
    				((EditText)mAnnotationLabelDialog.findViewById(R.id.annotation_label_text)).getText().toString()
    			);
    		outState.putLong("annotationLabelDialogShownId", mAnnotationLabelId);
    	}
    }
    
    public boolean onCreateOptionsMenu(Menu menu)
    {
      	menu.add("E-Mail Session").setIcon(android.R.drawable.ic_dialog_email);
        return true;
    }
    
    public void stopPlayback()
    {
    	if(mPlayer != null && mPlayer.isPlaying())
    	{
    		mPlayer.stop();
    		onPlayCompletion();
    	}
    }
    
    public void setOldTitle(CharSequence title)
    {
    	mOldTitle = title;
    }
    
    public void updateListIndication()
    {
        if(mPlaybackDialog != null && mPlaybackDialog.isShowing())
        {
        	if(mPlayingPosition == -1)
        		return;
        	
        	mListView.setIndication(mPlayingPosition);
        }
    }
    
    String makeAnnotationText(Cursor cursor)
    {
		String text = formatter.format(new Date(cursor.getLong(ANNOTATIONS_START_TIME)));
		String label = cursor.getString(ANNOTATIONS_LABEL);
		if(label.length()>0)
			text += " " + cursor.getString(ANNOTATIONS_LABEL);
		return text;
    }
    
    void onPlayItemLostFocus()
    {
		mPlayingPosition = -1;
		mActivity.setTitle(mOldTitle);
		if(mPlaybackPanelEnabled)
			mListView.clearIndication();
    }
    boolean createSessionArchive(String archiveFilename)
    {
        byte[] buffer = new byte[1024];
        
        try
        {
            ZipOutputStream archive = new ZipOutputStream(new FileOutputStream(archiveFilename));
        
            for(mAnnotationsCursor.moveToFirst(); !mAnnotationsCursor.isAfterLast(); mAnnotationsCursor.moveToNext())
            {
                FileInputStream in = new FileInputStream(mAnnotationsCursor.getString(ANNOTATIONS_FILE_NAME));
                archive.putNextEntry(new ZipEntry("audio" + (mAnnotationsCursor.getPosition() + 1) + ".3gpp"));
        
                int length;
                while ((length = in.read(buffer)) > 0)
                	archive.write(buffer, 0, length);
        
                archive.closeEntry();
                in.close();
            }
        
            // Complete the ZIP file
            archive.close();
        } catch (IOException e)
        {
    		Toast.makeText(mActivity, mActivity.getString(R.string.error_zip) + " " + e.getMessage(), Toast.LENGTH_SHORT).show();
        	return false;
        }
        return true;
    }
    String annotationTextInfo(String label)
    {
        String text = label + " " + (mAnnotationsCursor.getPosition() + 1) + "\n";
        text += " " +mActivity.getString(R.string.label) + " " + mAnnotationsCursor.getString(ANNOTATIONS_LABEL) + "\n";
        text += " " +mActivity.getString(R.string.start_time) + " " + formatter.format(new Date(mAnnotationsCursor.getLong(ANNOTATIONS_START_TIME))) + "\n";
        text += " " +mActivity.getString(R.string.end_time) + " " + formatter.format(new Date(mAnnotationsCursor.getLong(ANNOTATIONS_END_TIME))) + "\n";
        text += " " +mActivity.getString(R.string.filename) + " " + mAnnotationsCursor.getString(ANNOTATIONS_FILE_NAME) + "\n\n";

        return text;
    }
    void sendEmail(boolean wholeSession)
    {
        Intent emailSession = new Intent(Intent.ACTION_SEND);
        if(mEmailSubject == null || mEmailSubject.length()==0)
	        if(wholeSession)
	        	emailSession.putExtra(Intent.EXTRA_SUBJECT, mActivity.getString(R.string.rehearsal_assistant_session) +" \"" + mSessionCursor.getString(1) + "\"");
	        else
	        	emailSession.putExtra(Intent.EXTRA_SUBJECT, mActivity.getString(R.string.rehearsal_assistant_recording)+ " \"" + formatter.format(new Date(mAnnotationsCursor.getLong(ANNOTATIONS_START_TIME))) + "\"");
        else
        	emailSession.putExtra(Intent.EXTRA_SUBJECT, mEmailSubject);        	
        
    	String messageText = new String();
    	if(wholeSession && mEmailDetail)
    	{
	    	messageText += mActivity.getString(R.string.session_title) + " " + mSessionCursor.getString(SESSIONS_TITLE) + "\n";
	    	messageText += mActivity.getString(R.string.session_start_time) + " " + DateFormat.getDateTimeInstance().format(new Date(mSessionCursor.getLong(SESSIONS_START_TIME))) + "\n";
	    	messageText += mActivity.getString(R.string.session_end_time) + " " + DateFormat.getDateTimeInstance().format(new Date(mSessionCursor.getLong(SESSIONS_END_TIME))) + "\n\n";
    	}
    	if(wholeSession)
    	{
            String archiveFilename = Environment.getExternalStorageDirectory().getAbsolutePath() + "/urbanstew.RehearsalAssistant/session.zip";
            // If there are no annotations we don't need an archive
            if(mAnnotationsCursor.getCount() == 0 || createSessionArchive(archiveFilename))
            {    	    	
    	    	// If there are no annotations, say so.
    	    	if(mAnnotationsCursor.getCount() == 0)
    	    	{
    	    		messageText += mActivity.getString(R.string.no_annotations) + "\n";
    	    		emailSession.setType("message/rfc822");
    	    	}
    	    	else // otherwise, attach the file.
    	    	{
    		    	emailSession.putExtra(Intent.EXTRA_STREAM, Uri.parse ("file://" + archiveFilename));
    		    	emailSession.setType("application/zip");
    	    	}
            }
        	// Add annotation information
            if(mEmailDetail)
	            for(mAnnotationsCursor.moveToFirst(); !mAnnotationsCursor.isAfterLast(); mAnnotationsCursor.moveToNext())
	            	messageText += annotationTextInfo(mActivity.getString(R.string.annotation));
    	}
    	else
    	{
	    	emailSession.putExtra(Intent.EXTRA_STREAM, Uri.parse ("file://" + mAnnotationsCursor.getString(ANNOTATIONS_FILE_NAME)));
	    	// TODO: this can sometimes be audio/wav
	    	emailSession.setType("audio/3gpp");

    		if(mEmailDetail)
    			messageText += annotationTextInfo(mActivity.getString(R.string.recording));
    	}
    	messageText += "\n\n" + mActivity.getString(R.string.recorded_with_rehearsal_assistant);
        emailSession.putExtra(Intent.EXTRA_TEXT, messageText);
    	
        if(mEmailTo != null)
        	emailSession.putExtra(Intent.EXTRA_EMAIL, mEmailTo.split(","));
      	emailSession = Intent.createChooser(emailSession, mActivity.getString(R.string.e_mail));
      	
      	try
      	{
      		mActivity.startActivity(emailSession);
      	}
      	catch (ActivityNotFoundException e)
      	{
      		Toast.makeText(mActivity, mActivity.getString(R.string.error_send) + " " + e.getMessage(), Toast.LENGTH_SHORT).show();
      	}
    }
    
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	sendEmail(true);
		return true;
    }
    
    public static final int MENU_ITEM_PLAYBACK = Menu.FIRST;
    public static final int MENU_ITEM_LABEL = Menu.FIRST+1;
    public static final int MENU_ITEM_EMAIL = Menu.FIRST+2;
    public static final int MENU_ITEM_DELETE = Menu.FIRST+3;
    public static final int MENU_ITEM_EDIT = Menu.FIRST+4;

    View.OnCreateContextMenuListener mCreateContextMenuListener = new View.OnCreateContextMenuListener()
    {
		public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo)
		{
			menu.add(Menu.NONE, MENU_ITEM_PLAYBACK, 0, mActivity.getString(R.string.play));
			menu.add(Menu.NONE, MENU_ITEM_LABEL, 1, mActivity.getString(R.string.edit_label));
			menu.add(Menu.NONE, MENU_ITEM_EMAIL, 2, mActivity.getString(R.string.e_mail));
			menu.add(Menu.NONE, MENU_ITEM_DELETE, 3, mActivity.getString(R.string.delete));
			menu.add(Menu.NONE, MENU_ITEM_EDIT, 3, mActivity.getString(R.string.open_ringdroid));
		}
    	
    };
    
    void displayAnnotationLabelDialog(String content, long id)
    {
    	mAnnotationLabelId = id;
        LayoutInflater factory = LayoutInflater.from(mActivity);
        final View textEntryView = factory.inflate(R.layout.alert_annotation_label_entry, null);
        mAnnotationLabelDialog = new AlertDialog.Builder(mActivity)
            .setView(textEntryView)
            .setPositiveButton(mActivity.getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	EditText label = (EditText)mAnnotationLabelDialog.findViewById(R.id.annotation_label_text);

                	ContentValues values = new ContentValues();
                	String labelText = label.getText().toString();
                	values.put(Annotations.LABEL, labelText);
                	ContentResolver resolver = mActivity.getContentResolver();
                	Uri renamedAnnotationUri = ContentUris.withAppendedId(Annotations.CONTENT_URI,mAnnotationLabelId);
                	resolver.update(renamedAnnotationUri, values, null, null);
                	
                    String[] projection =
                    {
                    	Annotations._ID,
                    	Annotations.FILE_NAME
                    };
                	Cursor renamedAnnotationCursor = resolver.query(renamedAnnotationUri, projection, null, null,
                            Annotations.DEFAULT_SORT_ORDER);
                	if(renamedAnnotationCursor.getCount() >= 1)
                	{
                		renamedAnnotationCursor.moveToFirst();
                    	String oldFileName = renamedAnnotationCursor.getString(1);
                    	File oldFile = new File(oldFileName);
                    	String newFileName =
                    		oldFile.getParent()
                    		+ "/audio_" + mAnnotationLabelId
                    		+ "_" + labelText.replaceAll(" ", "_").replaceAll("[^a-zA-Z0-9_]", "")
                    		+ oldFileName.substring(oldFileName.length()-4);

                    	Log.d(SessionPlayback.class.getName(), "renaming file " + oldFileName + " to " + newFileName);
                    	File newFile = new File(newFileName);
                    	boolean success = oldFile.renameTo(newFile);
                    	if(success)
                    	{
                    		ContentValues renamedValues = new ContentValues();
                    		renamedValues.put(Annotations.FILE_NAME, newFileName);
                        	resolver.update(renamedAnnotationUri, renamedValues, null, null);
                    	}
                	}
                	renamedAnnotationCursor.close();
                	
            		mAnnotationLabelDialog = null;
                }
            })
            .setNegativeButton(mActivity.getString(R.string.cancel), null)
            .create();
        mAnnotationLabelDialog.show();
    	EditText label = (EditText)mAnnotationLabelDialog.findViewById(R.id.annotation_label_text);
    	label.setText(content);
    }

	public boolean onContextItemSelected(MenuItem item)
	{
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e("Rehearsal Assistant", "bad menuInfo", e);
            return false;
        }
        final AdapterView.AdapterContextMenuInfo final_info = info;
        switch(item.getItemId())
        {
        case MENU_ITEM_PLAYBACK:
        	playItem(info.position);
        	break;
        case MENU_ITEM_LABEL:
        	mAnnotationsCursor.moveToPosition(info.position);
        	displayAnnotationLabelDialog(mAnnotationsCursor.getString(ANNOTATIONS_LABEL), mAnnotationsCursor.getLong(ANNOTATIONS_ID));
        	break;
        case MENU_ITEM_EMAIL:
        	mAnnotationsCursor.moveToPosition(info.position);
        	sendEmail(false);
        	break;
        case MENU_ITEM_DELETE:
        	DialogInterface.OnClickListener delete = new DialogInterface.OnClickListener()
    		{
				public void onClick(DialogInterface dialog, int which)
				{
		        	mAnnotationsCursor.moveToPosition(final_info.position);
		        	mActivity.getContentResolver().delete
		        	(
		        		ContentUris.withAppendedId(Annotations.CONTENT_URI, mAnnotationsCursor.getLong(ANNOTATIONS_ID)),
		        		null,
		        		null
		        	);
				}
    		};
    		
    		if(mConfirmIndividualDeletion)        	
	        	Request.cancellable_confirmation
	        	(
	        		mActivity,
	        		mActivity.getString(R.string.warning),
	        		mActivity.getString(R.string.warning_erase_recording),
	        		delete
	        	);
    		else
    			delete.onClick(null, 0);
        	break;
        case MENU_ITEM_EDIT:
    		try
        	{
    			Intent intent = 
	            	new Intent
	            	(
	            		Intent.ACTION_EDIT,
	            		Uri.parse(mAnnotationsCursor.getString(ANNOTATIONS_FILE_NAME))
	            	);
    			intent.setComponent(new ComponentName("com.ringdroid", "com.ringdroid.RingdroidEditActivity"));
    			
	            mActivity.startActivity(intent);
        	} catch (ActivityNotFoundException e)
        	{
          		Request.confirmation
          		(
          			mActivity,
          			mActivity.getString(R.string.ringdroid_not_installed),
          			mActivity.getString(R.string.ringdroid_dl_confirm),
          			new DialogInterface.OnClickListener()
          			{
						public void onClick(DialogInterface dialog, int which)
						{
							try
							{
					            mActivity.startActivity
					            (
					            	new Intent
					            	(
					            		Intent.ACTION_VIEW,
					            		Uri.parse("market://search?q=pname:com.ringdroid")
					            	)
					            );
							} catch (ActivityNotFoundException e)
							{
					      		Toast.makeText(mActivity, R.string.ringdroid_dl_error, Toast.LENGTH_SHORT).show();
							}
						}
          			}
          		);
        	}
        }
        return true;
	}

	void setPlayPauseButton(int id)
	{
		mPlayPauseButton.setImageDrawable(mActivity.getResources().getDrawable(id));		
	}
	
    void displayPlaybackDialog()
    {
    	if(!mPlaybackDialog.isShowing())
    		setPlayPauseButton(android.R.drawable.ic_media_pause);
        mPlaybackDialog.show();
    }

    OnCompletionListener mCompletionListener = new OnCompletionListener()
    {
		public void onCompletion(MediaPlayer mp)
		{
			onPlayCompletion();
		}
    };
    
    void onPlayCompletion()
    {
    	updateProgressDisplay();
    	if(mPlaybackPanelEnabled)
    		setPlayPauseButton(android.R.drawable.ic_media_play);
    	if(mPlaybackPanelDisappears)
    	{
			mPlayingPosition = -1;
    		mPlaybackDialog.dismiss();
    		onPlayItemLostFocus();
    	}
    	else if(!mPlaybackPanelEnabled)
    	{
    		onPlayItemLostFocus();
    	}
    }
    
	void playItem(int position)
	{
		mActivity.onPlaybackStarted();
		mPlayingPosition = position;
		mAnnotationsCursor.moveToPosition(position);
		mActiveAnnotationStartTime = mAnnotationsCursor.getLong(ANNOTATIONS_START_TIME);
		
		if(mAnnotationsCursor.getInt(ANNOTATIONS_VIEWED) == 0)
		{
			ContentValues values = new ContentValues();
	    	values.put(Annotations.VIEWED, true);
	    	mActivity.getContentResolver().update(ContentUris.withAppendedId(Annotations.CONTENT_URI,mAnnotationsCursor.getLong(ANNOTATIONS_ID)), values, null, null);
		}
		
		String state = android.os.Environment.getExternalStorageState();
    	if(!state.equals(android.os.Environment.MEDIA_MOUNTED)
    			&& !state.equals(android.os.Environment.MEDIA_MOUNTED_READ_ONLY))
    	{
        	Request.notification(mActivity,
            		mActivity.getString(R.string.media_missing),
            		mActivity.getString(R.string.media_missing_msg01) + " " + state + ").  " + mActivity.getString(R.string.media_missing_msg02)
            	);
        	return;
    	}
    	
    	if(mPlayer != null)
    	{
    		mPlayer.stop();
    		mPlayer.release();
    	}
        try
        {
        	mPlayer = new MediaPlayer();
        	mPlayer.setOnCompletionListener(mCompletionListener);
        	try
        	{
        		mPlayer.setDataSource(mAnnotationsCursor.getString(ANNOTATIONS_FILE_NAME));        		
        	}
        	catch (IllegalArgumentException e)
        	{
        		Toast.makeText(mActivity, mActivity.getString(R.string.file_not_found) + ": " + mAnnotationsCursor.getString(ANNOTATIONS_FILE_NAME), Toast.LENGTH_LONG).show();
        		mPlayer.release();
        		mPlayer = null;
        		return;
        	}
        	mPlayer.prepare();
        	mPlayer.start();
        	if(mPlaybackPanelEnabled)
        	{
        		displayPlaybackDialog();
            	setPlayPauseButton(android.R.drawable.ic_media_pause);
        		mListView.setIndication(position);
        		
        		File audio = new File(mAnnotationsCursor.getString(ANNOTATIONS_FILE_NAME));
        		mPlaybackFileSize.setText(String.valueOf(audio.length()/1024) + " kB");
        		mPlaybackDuration.setText(mPlayTimeFormatter.format(mPlayer.getDuration()+ (mSessionTiming ? mActiveAnnotationStartTime : 0)));
        	}
        	mActivity.setTitle(mActivity.getString(R.string.app_name)+ " - " + makeAnnotationText(mAnnotationsCursor));
        	updateProgressDisplay();
        }
        catch(java.io.IOException e)
        {
        	if(e.getMessage()!=null)
        		Toast.makeText(mActivity, e.getMessage(),
        				Toast.LENGTH_SHORT).show();

        }

        Intent intent = new Intent("urbanstew.RehearsalAssistant.NetPlugin.playbackStarted");
        
        intent.putExtra("START_TIME", (float)(mAnnotationsCursor.getLong(ANNOTATIONS_START_TIME) / 1000.0f));
        intent.putExtra("END_TIME", (float)(mAnnotationsCursor.getLong(ANNOTATIONS_END_TIME) / 1000.0f));
        
        mActivity.sendBroadcast(intent);
	}
    /** Called when the user selects a list item. */
    AdapterView.OnItemClickListener mSelectedListener = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id)
        {
			playItem(position);
        }
    };
    
    void updateProgressDisplay()
    {
    	String timeText;
		timeText = mPlayTimeFormatter.format(mPlayer.getCurrentPosition() + (mSessionTiming ? mActiveAnnotationStartTime : 0));
		mCurrentTime.setText(timeText);
		mPlaybackCurrentTime.setText(timeText);

		mSeekBar.setProgress((1024 * mPlayer.getCurrentPosition()) / mPlayer.getDuration());
    }
    
    Timer mTimer = new Timer();
    TimerTask mCurrentTimeTask;
	
	public Cursor annotationsCursor()
	{	return mAnnotationsCursor; }

	public Cursor sessionCursor()
	{	return mSessionCursor; }
	
	DateFormat playTimeFormatter()
	{	return mPlayTimeFormatter; }
	
	RehearsalActivity mActivity;
	
    TextView mCurrentTime;
    
    // Playback dialog views
    SeekBar mSeekBar;
    TextView mPlaybackCurrentTime, mPlaybackDuration, mPlaybackFileSize;
    
    IndicatingListView mListView;
    SimpleCursorAdapter mListAdapter;
    
    Cursor mAnnotationsCursor;
    Cursor mSessionCursor;
    MediaPlayer mPlayer = null;
    int mPlayingPosition = -1;
    List<String> mStrings = new LinkedList<String>();
    ArrayAdapter<String> listAdapter;
    
    SimpleDateFormat mPlayTimeFormatter = new SimpleDateFormat("HH:mm:ss");
    DateFormat formatter;
    
    long mActiveAnnotationStartTime = 0;
    AlertDialog mAnnotationLabelDialog = null;
    long mAnnotationLabelId;
    
    AlertDialog mPlaybackDialog = null;
    ImageButton mPlayPauseButton;
    boolean mSessionTiming;
    
    boolean mPlaybackPanelEnabled, mPlaybackPanelDisappears, mEmailDetail, mConfirmIndividualDeletion;
    String mEmailTo, mEmailSubject;
    
    Handler mHandler = new Handler();
    CharSequence mOldTitle;
}
