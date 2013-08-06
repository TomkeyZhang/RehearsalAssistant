package urbanstew.RehearsalAssistant;

import urbanstew.RehearsalAssistant.Rehearsal.Projects;
import urbanstew.RehearsalAssistant.Rehearsal.Sessions;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class ProjectBase extends RehearsalActivity
{	
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
      	
        mAppData = new AppDataAccess(this);

       	mProjectId = Long.valueOf(getIntent().getData().getPathSegments().get(1));
        
        mAppData.setCurrentProjectId(mProjectId);

        String[] projectsProjection =
        {
        	Projects._ID,
        	Projects.TITLE
        };

        Cursor projectCursor =
        	getContentResolver().query
        	(
        			ContentUris.withAppendedId(Projects.CONTENT_URI, mProjectId),
        			projectsProjection, null, null, Projects.DEFAULT_SORT_ORDER
        	);
        if(projectCursor.getCount() == 0)
        {
        	Toast.makeText(this, R.string.error_project_does_not_exist, Toast.LENGTH_LONG).show();
            projectCursor.close();
        	finish();
        	return;
        }

        projectCursor.moveToFirst();
        setTitle("Rehearsal Assistant - " + projectCursor.getString(1));
        projectCursor.close();
        
        // Display license if this is the first time running this version.
        float visitedVersion = mAppData.getVisitedVersion();
        if (visitedVersion < RehearsalAssistant.currentVersion)
        {
            Request.notification(this, this.getString(R.string.uncompressed_recording), this.getString(R.string.uncompressed_recording2));

            AlertDialog.Builder dialog = new AlertDialog.Builder(this)
        	.setTitle(getString(R.string.upload_to_soundcloud))
        	.setMessage(getString(R.string.soundcloud_droid))
        	.setPositiveButton
        	(
        		getString(R.string.download_it),
        		new DialogInterface.OnClickListener()
        		{
        		    public void onClick(DialogInterface dialog, int whichButton)
        		    {
        				try
        				{
        					startActivity
        			        (
        			        	new Intent
        			        	(
        			        		Intent.ACTION_VIEW,
        			        		Uri.parse("market://search?q=pname:org.urbanstew.soundclouddroid")
        			        	).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        			        );
        				} catch (ActivityNotFoundException e)
        				{
        					try
        					{
	        					startActivity
	        			        (
	        			        	new Intent
	        			        	(
	        			        		Intent.ACTION_VIEW,
	        			        		Uri.parse("http://urbanstew.org/soundclouddroid/")
	        			        	)
	        			        );
        					}
        					catch (ActivityNotFoundException e2)
        					{
        					}
        				}
        		    }
        		}
        	)
        	.setNegativeButton
        	(
        		getString(R.string.dont_download),
        		null
        	);
            dialog.show();
        	
            if(visitedVersion >= 0.5f)
            	mAppData.setVisitedVersion(RehearsalAssistant.currentVersion);
        }
        if (visitedVersion < 0.5f)
        {
    		Request.confirmation
    		(
				this,
				getString(R.string.license2),
				getString(R.string.license),
				new DialogInterface.OnClickListener()
	    		{
	    		    public void onClick(DialogInterface dialog, int whichButton)
	    		    {
	    		    	mAppData.setVisitedVersion(RehearsalAssistant.currentVersion);
	    		    }
	    		}
	    	);
        }
    }
        
    protected void onNewIntent(Intent intent)
    {
    	super.onNewIntent(intent);
    	finish();
    	startActivity(intent);
    }

	protected void setSimpleProject(boolean simpleMode)
	{
		mSimpleMode = simpleMode;
	}

    public boolean onCreateOptionsMenu(Menu menu)
    {
        mHelpMenuItem = menu.add(R.string.help).setIcon(android.R.drawable.ic_menu_help);
        String switchText;
        switchText = this.getString(R.string.project_manager);
        mSwitchMenuItem = menu.add(switchText).setIcon(android.R.drawable.ic_menu_more);
        super.onCreateOptionsMenu(menu);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	if(!super.onOptionsItemSelected(item))
    	{
			if(item == mSwitchMenuItem)
			{		        
				startActivity
		        (
		        	new Intent
		        	(
		        		Intent.ACTION_VIEW,
		        		Projects.CONTENT_URI
		        	)
		        );
		        
		        finish();

				return true;
			}
			return false;
    	}
    	return true;
    }
        
    protected long projectId()
    {
    	return mProjectId;
    }
    
    long mProjectId;
    
    protected MenuItem mHelpMenuItem, mSwitchMenuItem; 
    
    protected static final int SESSIONS_ID = 0;
    protected static final int SESSIONS_TITLE = 1;
    protected static final int SESSIONS_START_TIME = 2;
    protected static final int SESSIONS_END_TIME = 3;
    
    protected static final String[] sessionsProjection = new String[]
	{
	      Sessions._ID, // 0
	      Sessions.TITLE, // 1
	      Sessions.START_TIME,
	      Sessions.END_TIME
	};
    
    AppDataAccess mAppData;
    
    boolean mSimpleMode;
}
