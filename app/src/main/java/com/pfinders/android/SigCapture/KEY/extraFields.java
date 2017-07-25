package com.pfinders.android.SigCapture.KEY;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class extraFields extends Activity {
	private static String sdDir = Environment.getExternalStorageDirectory().toString();
	private static File APP_FILE_PATH = new File(sdDir + "/sigcapture/orders");
	private String filenamePrefix; // for matching .txt and .jpg files
    String str;
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
        setContentView(R.layout.extrafields);
        ActionBar bar = getActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#191919")));


        getPrefs();
        
        Bundle b = getIntent().getExtras();
        filenamePrefix = b.getString("orderNumber");
        if (savedInstanceState == null) {
            // we were just launched: set up app
            
        } else {
            // we are being restored: resume stuff 
        	//restoreState(savedInstanceState);
        }
        
        Button OKButton = (Button) findViewById(R.id.OKButton);
        OKButton.setOnClickListener(new View.OnClickListener() {
        	@Override
			public void onClick(View view) {
        		// write notes then exit
        		EditText driverNotes = (EditText) findViewById(R.id.driverNotes);
        		try {
  	                FileWriter out = new FileWriter(new File(APP_FILE_PATH, filenamePrefix + ".not"));
  	                out.write(driverNotes.getText().toString());
  	                out.write("\n");
  	                out.close();
  	            } catch (IOException e) {
  	    	      // can't write to file 
  	            }
        		((Activity) view.getContext()).finish();
           }
        });
	}
	
	private void getPrefs() {
        // load the applications preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        workOfflinePref = prefs.getBoolean("workOfflinePref", false);
        serverHostnamePref = prefs.getString("serverHostnamePref", "test.westpacsupply.com");
        userIdPref = prefs.getString("userIdPref", "");
        passwordPref = prefs.getString("passwordPref", "");
        vibrateOnLookupPref = prefs.getBoolean("vibrateOnLookupPref", true);
        driverModePref = prefs.getBoolean("driverModePref", false);
        companyNamePref = prefs.getString("companyNamePref", "WestPac Supply");
        
        TextView companyName = (TextView) findViewById(R.id.CompanyName);
        companyName.setText(companyNamePref);
    }
}
