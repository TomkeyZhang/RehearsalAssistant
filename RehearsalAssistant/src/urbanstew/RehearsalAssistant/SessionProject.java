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

import urbanstew.RehearsalAssistant.Rehearsal.Sessions;

import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/** The RehearsalAssistant Activity is the top-level activity.
 */
public class SessionProject extends ProjectBase implements View.OnClickListener
{	
    public static final int MENU_ITEM_PLAYBACK = Menu.FIRST;
    public static final int MENU_ITEM_RECORD = Menu.FIRST + 1;
    public static final int MENU_ITEM_DELETE = Menu.FIRST + 2;
    
    /** Called when the activity is first created.
     *  
     *  For now, provides access to the recording and playback activities.
     *  
     */
    public void onCreate(Bundle savedInstanceState)
    {
        setContentView(R.layout.main);

        super.onCreate(savedInstanceState);
        super.setSimpleProject(false);
        
        // Read sessions
        cursor = getContentResolver().query(Sessions.CONTENT_URI, sessionsProjection, Sessions.PROJECT_ID + "=" + projectId(), null,
                Sessions.DEFAULT_SORT_ORDER);
        
        Log.w("RehearsalAssistant", "Read " + cursor.getCount() + " " + Sessions.TABLE_NAME);
        
        // Map Sessions to ListView
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.runslist_item, cursor,
                new String[] { Sessions.TITLE }, new int[] { android.R.id.text1 });
        ListView list = (ListView)findViewById(R.id.run_list);
        list.setAdapter(adapter);
        
        // Setup list and the click listener
        ((Button)findViewById(R.id.new_run)).setOnClickListener(this);
        list.setOnCreateContextMenuListener(mCreateContextMenuListener);	
        list.setOnItemClickListener(mSelectedListener);
                
        list.setSelection(list.getCount()-1);
        
        adapter.registerDataSetObserver(new DataSetObserver()
        	{
        		public void onChanged()
        		{
        			reviseInstructions();
        		}
        	}
        	);

        reviseInstructions();
    }
    
    public void onDestroy()
    {
    	cursor.close();
    	super.onDestroy();
    }
    
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	if(!super.onOptionsItemSelected(item))
    		Request.notification(this, getString(R.string.help), getString(R.string.session_instructions));
		return true;
    }
    
    /** Called when the user selects an item in the list.
     *  
     *  Currently, starts the RehearsalPlayback activity.
     *  
     */
    AdapterView.OnItemClickListener mSelectedListener = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id)
        {
	    	Uri runUri = ContentUris.withAppendedId(Sessions.CONTENT_URI, id);
	    	startActivity(new Intent(Intent.ACTION_VIEW, runUri));
        }
    };
    
    View.OnCreateContextMenuListener mCreateContextMenuListener = new View.OnCreateContextMenuListener()
    {
		public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo)
		{
   			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo; 
   			//menu.add(Menu.NONE, MENU_ITEM_RENAME, 0, "rename");
   			cursor.moveToPosition(info.position);

   			menu.add(0, MENU_ITEM_PLAYBACK, 0, getString(R.string.session_playback));
   			String recordAction;
   			if(cursor.isNull(SESSIONS_START_TIME))
   				recordAction = getString(R.string.session_record);
   			else if(cursor.isNull(SESSIONS_END_TIME))
   				recordAction = getString(R.string.session_continue);
   			else
   				recordAction = getString(R.string.session_overwrite);
   				
			menu.add(0, MENU_ITEM_RECORD, 1, recordAction);
			menu.add(0, MENU_ITEM_DELETE, 2, getString(R.string.delete));
		}
    };
    
	public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e("Rehearsal Assistant", "bad menuInfo", e);
            return false;
        }

    	final Uri runUri = ContentUris.withAppendedId(Sessions.CONTENT_URI, info.id);

    	switch (item.getItemId()) {

            case MENU_ITEM_PLAYBACK: {
                // Delete the run that the context menu is for
            	startActivity(new Intent(Intent.ACTION_VIEW, runUri));
                return true;
            }
            case MENU_ITEM_RECORD:
            {
            	cursor.moveToPosition(info.position);
            	if(!cursor.isNull(SESSIONS_END_TIME))
            		Request.cancellable_confirmation
            		(
            			this,
            			getString(R.string.warning),
            			getString(R.string.warning_erase_previous_recordings),
            			new OnClickListener()
            			{
							public void onClick(DialogInterface arg0, int arg1)
							{
				        		startActivity(new Intent(Intent.ACTION_EDIT, runUri));
							}            				
            			}
            		);
            	else
            		startActivity(new Intent(Intent.ACTION_EDIT, runUri));
                return true;
            }
            case MENU_ITEM_DELETE: {
            	Request.cancellable_confirmation
        		(
        			this,
        			getString(R.string.warning),
        			getString(R.string.warning_erase_current_recordings),
        			new OnClickListener()
        			{
						public void onClick(DialogInterface arg0, int arg1)
						{
							getContentResolver().delete(runUri, null, null);
						}            				
        			}
        		);
                return true;
            }
        }
        return false;
	}

	public void onClick(View v)
	{
		if(v == findViewById(R.id.new_run))
			startActivity(new Intent(Intent.ACTION_INSERT, Sessions.CONTENT_URI).putExtra("project_id", projectId()));
	}
	
	void reviseInstructions()
	{
    	TextView noSessions = (TextView)findViewById(R.id.no_sessions);
        if(cursor.getCount() == 0)
        {
        	noSessions.setText(getString(R.string.session_no_session_instructions));
        	noSessions.setVisibility(View.VISIBLE);
        }
        else
        	noSessions.setVisibility(View.INVISIBLE);
	}
	Cursor cursor;
}