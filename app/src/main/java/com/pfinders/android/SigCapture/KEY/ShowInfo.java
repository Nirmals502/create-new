package com.pfinders.android.SigCapture.KEY;

import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

public class ShowInfo extends Activity {

	boolean workOfflinePref;
    String serverHostnamePref;
    boolean vibrateOnLookupPref;
    boolean driverModePref;
    String userIdPref;
    String passwordPref;
    String companyNamePref;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.showinfo);
        ActionBar bar = getActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#191919")));


        getPrefs();
        
        String versionName = "";
        PackageInfo packageInfo;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = "Version " + packageInfo.versionName;
        } catch (NameNotFoundException e) {
            //e.printStackTrace();
        }
            TextView tv = (TextView) findViewById(R.id.AppVersion);
            tv.setText(versionName);
    }
    
    private void getPrefs() {
        // load the applications preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        workOfflinePref = prefs.getBoolean("workOfflinePref", false);
        serverHostnamePref = prefs.getString("serverHostnamePref", "www.plimptonhills.com");
        userIdPref = prefs.getString("userIdPref", "");
        passwordPref = prefs.getString("passwordPref", "");
        vibrateOnLookupPref = prefs.getBoolean("vibrateOnLookupPref", true);
        driverModePref = prefs.getBoolean("driverModePref", false);
        companyNamePref = prefs.getString("companyNamePref", "WestPac Supply");
        
        TextView companyName = (TextView) findViewById(R.id.CompanyName1);
        companyName.setText(companyNamePref);
    }
}
