package urbanstew.RehearsalAssistant;

import urbanstew.RehearsalAssistant.Rehearsal.Projects;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;


/**
 * @author Stjepan Rajko
 *
 * Handles opening of projects - delegates to appropriate Project activity depending on the project type.
 * 
 */
public class ProjectOpener extends Activity
{    
    /** Called when the activity is first created.
     */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // Get the project id and make sure we have the correct data Uri
        Uri data = getIntent().getData();
        
        long project_id;
        if(getIntent().getAction().equals("urbanstew.RehearsalAssistant.simple_mode"))
        {
        	project_id = new AppDataAccess(this).getRecorderWidgetProjectId();
        	data = ContentUris.withAppendedId(Projects.CONTENT_URI, project_id);
        }
        else
        	project_id = Long.valueOf(data.getPathSegments().get(1));

        // Find out what kind of project this is
        String[] projectsProjection =
        {
        	Projects._ID,
            Projects.TYPE
        };
        
        Cursor projectsCursor =
        	getContentResolver().query
        	(
        		ContentUris.withAppendedId(Projects.CONTENT_URI, project_id),
        		projectsProjection, 
        		null, null, Projects.DEFAULT_SORT_ORDER
        	);
        
        if(projectsCursor.getCount() == 0)
        {
        	// if the project does not exist, start the Project Manager
        	startActivity
		    (
		       	new Intent
		       	(
		       		Intent.ACTION_VIEW,
		       		Projects.CONTENT_URI
		       	)
		    );
        }
        else
        {
        	// otherwise, start the appropriate activity
	        projectsCursor.moveToFirst();
	        
	        if(projectsCursor.getLong(1) == Projects.TYPE_SESSION)
	            startActivity(new Intent(Intent.ACTION_VIEW, data, getApplication(), SessionProject.class));
	        else
	        	startActivity(new Intent(Intent.ACTION_VIEW, data, getApplication(), SimpleProject.class));
        }
        
        projectsCursor.close();
        // finish
        finish();
    }
}