package com.pfinders.android.SigCapture.KEY;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	 private SharedPreferences prefs;
	 private boolean _preferencesHaveChanged;
	 static final int RESULT_PREFS_CHANGED = 101;
	 
        @Override
        protected void onCreate(Bundle savedInstanceState) {
               super.onCreate(savedInstanceState);
               addPreferencesFromResource(R.xml.preferences);

               _preferencesHaveChanged = false;
               
               prefs = PreferenceManager.getDefaultSharedPreferences(this);
               prefs.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            	Intent resultIntent = new Intent();
        		setResult(_preferencesHaveChanged ? RESULT_PREFS_CHANGED : Activity.RESULT_OK, resultIntent);
        		finish();
            }
            
            return super.onKeyDown(keyCode, event);
        }

        @Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        {
        	if(key.equals("userIdPref") || key.equals("passwordPref")) {
        		_preferencesHaveChanged = true;
        	}
        	else if(key.equals("workOfflinePref")) {
        		boolean workOfflineOff = sharedPreferences.getBoolean("workOfflinePref", false);
        		if(workOfflineOff == false) {
        			_preferencesHaveChanged = true;
        		}
        	}
        }
}
