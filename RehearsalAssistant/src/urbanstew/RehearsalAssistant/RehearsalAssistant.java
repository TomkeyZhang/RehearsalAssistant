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

import urbanstew.RehearsalAssistant.Rehearsal.Projects;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

/** The RehearsalAssistant Activity is the top-level activity.
 */
public class RehearsalAssistant extends RehearsalActivity
{
	public static float currentVersion = 0.9f;
	
    /** Called when the activity is first created.
     *  
     *  For now, provides access to the recording and playback activities.
     *  
     */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        Intent intent;
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    	if(preferences.getBoolean("launch_with_last_project", true))
		{
	        // View the current project
	        AppDataAccess appData = new AppDataAccess(this);
	        intent =
	        	new Intent
	        	(
	        		Intent.ACTION_VIEW,
	        		ContentUris.withAppendedId(Projects.CONTENT_URI, appData.getCurrentProjectId())
	        	);
		}
    	else
    	{
    		// Open Project Manager
        	intent =
		       	new Intent
		       	(
		       		Intent.ACTION_VIEW,
		       		Projects.CONTENT_URI
		       	);
    	}
    	
        startActivity(intent);
        finish();
    }
    
    public static void checkSdCard(Context context)
    {
        String state = android.os.Environment.getExternalStorageState();
    	if(!state.equals(android.os.Environment.MEDIA_MOUNTED))
    	{
        	Request.notification(context,
            		context.getString(R.string.media_missing),
            		context.getString(R.string.media_missing_msg01) + " " + state + ").  " + context.getString(R.string.media_missing_msg02)
            	);
    	}
    }
}