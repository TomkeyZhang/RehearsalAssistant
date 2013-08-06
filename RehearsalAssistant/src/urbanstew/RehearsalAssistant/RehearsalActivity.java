package urbanstew.RehearsalAssistant;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

public class RehearsalActivity extends Activity
{	    
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        
        mSettingsMenuItem = menu.add(R.string.settings).setIcon(android.R.drawable.ic_menu_preferences);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	if(item == mSettingsMenuItem)
    	{
    	    startActivity(new Intent(getApplication(), SettingsActivity.class));
    		return true;
    	}
    	else
    		return false;
    }
    
	public void onPlaybackStarted()
	{
	} 
	    
    protected MenuItem mSettingsMenuItem;
}