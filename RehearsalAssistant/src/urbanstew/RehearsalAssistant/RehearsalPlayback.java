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


import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

/** The RehearsalPlayback Activity provides playback access for
 * 	annotations in a particular project.
 */
public class RehearsalPlayback extends RehearsalActivity
{
    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.playback);
        
        mSessionPlayback = new SessionPlayback(savedInstanceState, this, getIntent().getData());
        
        setTitle("Rehearsal Assistant - " + mSessionPlayback.sessionCursor().getString(SessionPlayback.SESSIONS_TITLE));
        mSessionPlayback.setOldTitle(getTitle());
        
        ((ListView)findViewById(R.id.annotation_list)).getAdapter()
    	.registerDataSetObserver(new DataSetObserver()
    	{
    		public void onChanged()
    		{
    			reviseInstructions();
    		}
    	}
    	);
    
        reviseInstructions();
    }
    
    public void onResume()
    {
    	mSessionPlayback.onResume();
    	super.onResume();
    }

    public void onPause()
    {
    	super.onPause();
    	mSessionPlayback.onPause();    	
    }

    public void onDestroy()
    {
    	mSessionPlayback.onDestroy();
    	super.onDestroy();
    }
    
    public void onConfigurationChanged(Configuration newConfig)
    {
    	super.onConfigurationChanged(newConfig);
    	mSessionPlayback.updateListIndication();
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
    
    public boolean onCreateOptionsMenu(Menu menu) {
    	mSessionPlayback.onCreateOptionsMenu(menu);
    	return super.onCreateOptionsMenu(menu);
    }
    
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	return
    		super.onOptionsItemSelected(item) ||
    		mSessionPlayback.onOptionsItemSelected(item);
    }
    
	public boolean onContextItemSelected(MenuItem item)
	{
		return mSessionPlayback.onContextItemSelected(item);
	}

	void reviseInstructions()
	{
    	TextView instructions = (TextView)findViewById(R.id.no_annotations);
        if(mSessionPlayback.annotationsCursor().getCount() == 0)
        {
        	instructions.setText(R.string.no_annotations);
        	instructions.setVisibility(View.VISIBLE);
        }
        else
        	instructions.setVisibility(View.INVISIBLE);
	}
	
    SessionPlayback mSessionPlayback;
}