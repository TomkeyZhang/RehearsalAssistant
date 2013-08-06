package urbanstew.RehearsalAssistant;

import android.os.Bundle;
import android.preference.PreferenceActivity;


public class SettingsActivity extends PreferenceActivity
{

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
                
        addPreferencesFromResource(R.xml.settings);
    }
    
}
