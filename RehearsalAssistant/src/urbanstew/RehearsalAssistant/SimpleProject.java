package urbanstew.RehearsalAssistant;

import java.util.Timer;
import java.util.TimerTask;

import urbanstew.RehearsalAssistant.Rehearsal.Sessions;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.urbanstew.VolumeEnvelopeView;

public class SimpleProject extends ProjectBase
{
	static long getSessionId(ContentResolver resolver, long projectId)
	{
        // a simple project must have exactly one session
        Cursor cursor = resolver.query(Sessions.CONTENT_URI, sessionsProjection, Sessions.PROJECT_ID + "=" + projectId, null,
                Sessions.DEFAULT_SORT_ORDER);
        // add the session if it is not there
        if(cursor.getCount() < 1)
        {
        	Log.d("Rehearsal Assistant", "Inserting Session for Memo Project ID: " + projectId);
        	ContentValues values = new ContentValues();
        	values.put(Sessions.PROJECT_ID, projectId);
        	values.put(Sessions.TITLE, "Simple Session");
      		values.put(Sessions.START_TIME, 0);
      		resolver.insert(Sessions.CONTENT_URI, values);
        	cursor.requery();
        }
        long sessionId;
        if(cursor.getCount() < 1)
        {
        	Log.w("Rehearsal Assistant", "Can't create session for memo project ID: " + projectId);
        	sessionId=-1;
        }
        else
        {
        	cursor.moveToFirst();
        	sessionId = cursor.getLong(SESSIONS_ID);
        }
        cursor.close();
        return sessionId;
	}
    public void onCreate(Bundle savedInstanceState)
    {
        setContentView(R.layout.simple);

        super.onCreate(savedInstanceState);
        super.setSimpleProject(true);
        
        mRecordButton = (ImageButton) findViewById(R.id.button);
        mRecordButton.setOnClickListener(mClickListener);
        mCurrentTime = (TextView) findViewById(R.id.playback_time);
        mEnvelopeView = (VolumeEnvelopeView) findViewById(R.id.volume_envelope);
        
        mSessionId = getSessionId(getContentResolver(), projectId());
        if(mSessionId < 0)
        {
    		Toast.makeText(this, R.string.memo_error, Toast.LENGTH_LONG).show();
        	finish();
        }
        mSessionPlayback = new SessionPlayback(savedInstanceState, this, ContentUris.withAppendedId(Sessions.CONTENT_URI, mSessionId));
        scrollToEndOfList();
                
        bindService(new Intent(IRecordService.class.getName()),
                mServiceConnection, Context.BIND_AUTO_CREATE);
        
        ((ListView)findViewById(R.id.annotation_list)).getAdapter()
        	.registerDataSetObserver(new DataSetObserver()
        	{
        		public void onChanged()
        		{
        			reviseInstructions();
        			if(mUpdateListSelection)
        				scrollToEndOfList();
        			mUpdateListSelection = false;
        		}
        	}
        	);

        reviseInstructions();
    }
            
    public void onResume()
    {
    	super.onResume();
    	mSessionPlayback.onResume();
    	try
    	{
    		updateInterface();
    	} catch (RemoteException e)
    	{}
    	
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    	mVolumeEnvelopeEnabled = preferences.getBoolean("recording_waveform", true);
    	
		if(mRecordService != null)
			try
			{
				mRecordService.setSession(mSessionId);
			} catch (RemoteException e)
			{
			}

    	mCurrentTimeTask = new TimerTask()
    		{
    			public void run()
    			{
    				SimpleProject.this.runOnUiThread(new Runnable()
    				{
    					public void run()
    					{
    						if(mRecordService != null)
    				        try
    				        {
    							if(mRecordService.getState() == RecordService.State.RECORDING.ordinal())
    							{
    								mCurrentTime.setText(mSessionPlayback.playTimeFormatter().format(mRecordService.getTimeInRecording()));
    								if(mVolumeEnvelopeEnabled)
    									mEnvelopeView.setNewVolume(mRecordService.getMaxAmplitude());
    								return;
    							}
    				    	} catch (RemoteException e)
    				    	{
    				    	}
    				    	if(mVolumeEnvelopeEnabled)
    				    		mEnvelopeView.clearVolume();
    					}
    				});              
    			}
    		};
		mTimer.scheduleAtFixedRate(
				mCurrentTimeTask,
				0,
				100);
    }

    public void onPause()
    {
    	mCurrentTimeTask.cancel();
    	mSessionPlayback.onPause();
    	
    	super.onPause();
    }

    public void onDestroy()
    {
    	mTimer.cancel();
    	unbindService(mServiceConnection);
    	mSessionPlayback.onDestroy();

    	super.onDestroy();
    }
    
    
    
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
    	mSessionPlayback.onRestoreInstanceState(savedInstanceState);
    }

    protected void onSaveInstanceState(Bundle outState)
    {
    	super.onSaveInstanceState(outState);
    	mSessionPlayback.onSaveInstanceState(outState);
    }
    
    public void onConfigurationChanged(Configuration newConfig)
    {
    	super.onConfigurationChanged(newConfig);
    	mSessionPlayback.updateListIndication();
    }
    
    public boolean onCreateOptionsMenu(Menu menu)
    {
        mSessionPlayback.onCreateOptionsMenu(menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	if(!super.onOptionsItemSelected(item))
    	{
			if(item == mHelpMenuItem)
			{
				Request.notification(this, getString(R.string.help), getString(R.string.simple_instructions));
				return true;
			}
			else
				return mSessionPlayback.onOptionsItemSelected(item);
    	}
    	return true;
    }
    
    public void onPlaybackStarted()
    {
		if(mRecordService == null)
			return;

		try
		{
	    	if(mRecordService.getState() == RecordService.State.RECORDING.ordinal())
	    	{
	    		mRecordService.toggleRecording(mSessionId);
	    		updateInterface();
	    	}
		} catch (RemoteException e)
		{}
    }
	public boolean onContextItemSelected(MenuItem item)
	{
		return mSessionPlayback.onContextItemSelected(item);
	}

	void reviseInstructions()
	{
    	TextView noAnnotations = (TextView)findViewById(R.id.no_annotations);
        if(mSessionPlayback.annotationsCursor().getCount() == 0)
        {
        	noAnnotations.setText(getString(R.string.simple_no_annotations_instructions));
        	noAnnotations.setVisibility(View.VISIBLE);
        }
        else
        	noAnnotations.setVisibility(View.INVISIBLE);
	}

	void scrollToEndOfList()
	{
        ListView list = (ListView)findViewById(R.id.annotation_list);
        list.setSelection(list.getCount()-1);
    }
	
    void updateInterface() throws RemoteException
    {
		if(mRecordService == null)
			return;

    	if(mRecordService.getState() == RecordService.State.STARTED.ordinal())
    	{
    		mRecordButton.setImageResource(R.drawable.media_record);
        	
        	mUpdateListSelection = true;
        	runOnUiThread(new Runnable()
    		{
    			public void run()
    			{
    		    	scrollToEndOfList();
    			}
    		});
        }
    	else
    		mRecordButton.setImageResource(R.drawable.media_recording);

    }
    /** Called when the button is pushed */
    View.OnClickListener mClickListener = new View.OnClickListener()
    {
        public void onClick(View v)
        {
        	if(mRecordService == null)
        		return;
	        try
	        {
	        	mSessionPlayback.stopPlayback();
	        	mRecordService.toggleRecording(mSessionId);
	        	updateInterface();
	    	} catch (RemoteException e)
	    	{
	    		
	    	}
        }
    };
    
    TimerTask mCurrentTimeTask;
	
    /**
     * Class for interacting with the secondary interface of the service.
     */
    private ServiceConnection mServiceConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
        	mRecordService = IRecordService.Stub.asInterface(service);
        	try
        	{
        		mRecordService.setSession(mSessionId);
        		updateInterface();
                mRecordButton.setClickable(true);
        	} catch (RemoteException e)
        	{}
        }

        public void onServiceDisconnected(ComponentName className) {
        	mRecordService = null;
            mRecordButton.setClickable(false);
        }
    };

    IRecordService mRecordService = null;

    TextView mCurrentTime;
    VolumeEnvelopeView mEnvelopeView;
    
    Timer mTimer = new Timer();
    
    ImageButton mRecordButton;
    
    long mSessionId;
    
    SessionPlayback mSessionPlayback;
    
    boolean mUpdateListSelection = false;
    boolean mVolumeEnvelopeEnabled;

}
