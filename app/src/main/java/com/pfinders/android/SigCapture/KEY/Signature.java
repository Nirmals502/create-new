package com.pfinders.android.SigCapture.KEY;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class Signature extends Activity {
	
	// Show a layout for capturing a signature with clear and accept buttons 
	private Panel sigPanel;
	private Button acceptButton;
	private Button clearButton; 
	private EditText signerName;
	private TextView messageAboveSig;
    private static String sdDir = Environment.getExternalStorageDirectory().toString();
    private static File APP_FILE_PATH = new File(sdDir + "/sigcapture/orders"); 
    private String filenamePrefix;
    private AlertDialog.Builder alertbox, SAalertBox, missingNameAlertBox;
    
    private boolean reprintPickTicket = false;
    private boolean SAorderComplete = false;

	boolean workOfflinePref;
    String serverHostnamePref;
    boolean vibrateOnLookupPref;
    boolean driverModePref;
    String userIdPref;
    String passwordPref;
    String printerQueuePref;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		ActionBar bar = getActionBar();
		bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#191919")));

		Bundle b = getIntent().getExtras();
        filenamePrefix = b.getString("orderNumber");
        RelativeLayout RelLayout = new RelativeLayout(this);
               
        clearButton = new Button(this);
        clearButton.setId(1);
        clearButton.setText(R.string.clear_button);
        RelativeLayout.LayoutParams clearParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        clearParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,-1);
        
        acceptButton = new Button(this);
        acceptButton.setId(2);
        acceptButton.setText(R.string.accept_button);
        RelativeLayout.LayoutParams acceptParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        acceptParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,-1);
        
        signerName = new EditText(this);
        signerName.setId(3);
        signerName.setHint("Signer Name");
        RelativeLayout.LayoutParams signerParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        signerParams.setMargins(20, 5, 20, 5); // left, top, right, bottom
        signerParams.addRule(RelativeLayout.BELOW,acceptButton.getId());
        
        messageAboveSig = new TextView(this);
        messageAboveSig.setText(R.string.sign_below);
        messageAboveSig.setId(4);
        RelativeLayout.LayoutParams messageParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        messageParams.addRule(RelativeLayout.CENTER_HORIZONTAL,-1);
        messageParams.addRule(RelativeLayout.BELOW,signerName.getId());
        
        sigPanel = new Panel(this);
        sigPanel.setId(5);
        RelativeLayout.LayoutParams panelParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        panelParams.addRule(RelativeLayout.BELOW,messageAboveSig.getId());
        panelParams.setMargins(20, 10, 20, 5); // left, top, right, bottom
 
        RelLayout.addView(sigPanel, panelParams);
        RelLayout.addView(clearButton, clearParams);
        RelLayout.addView(acceptButton, acceptParams);
        RelLayout.addView(signerName, signerParams);
        RelLayout.addView(messageAboveSig, messageParams);
        
  	    alertbox = new AlertDialog.Builder(this);
  	    alertbox.setMessage("Reprint Pick Ticket?");
  	    alertbox.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
  		  
            // do something when the button is clicked
            @Override
			public void onClick(DialogInterface arg0, int arg1) {
                reprintPickTicket = true;
  	            /*try {
	                FileWriter out = new FileWriter(new File(APP_FILE_PATH, filenamePrefix + ".sig"));
	                out.write(signerName.getText().toString());
	                out.write("\n");
	                if (reprintPickTicket) {
	                    out.write("reprintPickTicket|Y\n");
	                } else {
	        	        out.write("reprintPickTicket|N\n");
	                }
	                out.write("Printer|" + printerQueuePref + "\n");
	                out.write("UserID|" + userIdPref + "\n");
	                out.flush();
	                out.close();
	                out = null;
	                System.gc();
	            } catch (IOException e) {
	    	      // can't write to file 
	            }*/
  	            
    	        //Signature.this.setResult(RESULT_OK);
    	        //Signature.this.finish();
  	            SAalertBox.show();
            }
        });

        // set a negative/no button and create a listener
        alertbox.setNegativeButton("No", new DialogInterface.OnClickListener() {

            // do something when the button is clicked
            @Override
			public void onClick(DialogInterface arg0, int arg1) {
                reprintPickTicket = false;
  	            /*try {
	                FileWriter out = new FileWriter(new File(APP_FILE_PATH, filenamePrefix + ".sig"));
	                out.write(signerName.getText().toString());
	                out.write("\n");
	                if (reprintPickTicket) {
	                    out.write("reprintPickTicket|Y\n");
	                } else {
	        	        out.write("reprintPickTicket|N\n");
	                }
	                out.write("Printer|" + printerQueuePref + "\n");
	                out.write("UserID|" + userIdPref + "\n");
	                out.flush();
	                out.close();
	                out = null;
	                System.gc();
	            } catch (IOException e) {
	    	      // can't write to file 
	            }*/

    	        //Signature.this.setResult(RESULT_OK);
    	        //Signature.this.finish();
  	            SAalertBox.show();
            }
        });
        
        
        SAalertBox = new AlertDialog.Builder(this);
        SAalertBox.setMessage(R.string.SA_order_complete);
        SAalertBox.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
  		  
            // do something when the button is clicked
            @Override
			public void onClick(DialogInterface arg0, int arg1) {
                SAorderComplete = true;
  	            try {
	                FileWriter out = new FileWriter(new File(APP_FILE_PATH, filenamePrefix + ".sig"));
	                out.write(signerName.getText().toString());
	                out.write("\n");
	                if (reprintPickTicket) {
	                    out.write("reprintPickTicket|Y\n");
	                } else {
	        	        out.write("reprintPickTicket|N\n");
	                }
	                out.write("Printer|" + printerQueuePref + "\n");
	                out.write("UserID|" + userIdPref + "\n");
	                if (SAorderComplete) {
	  	                  out.write("SA|Y\n");
	  	            } else {
	  	        	      out.write("SA|N\n");
	  	            }
	                out.flush();
	                out.close();
	                out = null;
	                System.gc();
	            } catch (IOException e) {
	    	      // can't write to file 
	            }
  	            
    	        Signature.this.setResult(RESULT_OK);
    	        Signature.this.finish();
            }
        });

        // set a negative/no button and create a listener
        SAalertBox.setNegativeButton("No", new DialogInterface.OnClickListener() {

            // do something when the button is clicked
            @Override
			public void onClick(DialogInterface arg0, int arg1) {
                SAorderComplete = false;
  	            try {
	                FileWriter out = new FileWriter(new File(APP_FILE_PATH, filenamePrefix + ".sig"));
	                out.write(signerName.getText().toString());
	                out.write("\n");
	                if (reprintPickTicket) {
	                    out.write("reprintPickTicket|Y\n");
	                } else {
	        	        out.write("reprintPickTicket|N\n");
	                }
	                out.write("Printer|" + printerQueuePref + "\n");
	                out.write("UserID|" + userIdPref + "\n");
	                if (SAorderComplete) {
	  	                  out.write("SA|Y\n");
	  	              } else {
	  	        	      out.write("SA|N\n");
	  	            }
	                out.flush();
	                out.close();
	                out = null;
	                System.gc();
	            } catch (IOException e) {
	    	      // can't write to file 
	            }

    	        Signature.this.setResult(RESULT_OK);
    	        Signature.this.finish();
            }
        });
        
        
        acceptButton.setOnClickListener(new View.OnClickListener() {
          @Override
		public void onClick(View view) {        	  
        	  if(signerName.getText().length() > 0){
	              // save jpeg and text file and prompt for reprint
	        	  FileOutputStream fos = null;
	        	  try {
	        		  fos = new FileOutputStream(APP_FILE_PATH + "/" + filenamePrefix + ".jpg");
	        		  if (fos != null) {
	        			  sigPanel.saveBitmap(fos); // side effect: closes fos
	        		  }
	        	  } catch (Exception e) {
	        		  // can't write to file
	        	  }
	
	        	
	                  // driver doesn't want to be prompted to reprint a pick ticket
	        		  //reprintPickTicket = false;
	        		  //SAorderComplete = false;
	    	          try {
	    	              FileWriter out = new FileWriter(new File(APP_FILE_PATH, filenamePrefix + ".sig"));
	    	              out.write(signerName.getText().toString());
	    	              out.write("\n");
	    	              /*if (reprintPickTicket) {
	    	                  out.write("reprintPickTicket|Y\n");
	    	              } else {
	    	        	      out.write("reprintPickTicket|N\n");
	    	              }*/
	    	              out.write("Printer|" + printerQueuePref + "\n");
	  	                  out.write("UserID|" + userIdPref + "\n");
		  	              /*if (SAorderComplete) {
		  	                  out.write("SA|Y\n");
		  	              } else {
		  	        	      out.write("SA|N\n");
		  	              }*/
	    	              out.flush();
	    	              out.close();
	    	              out = null;
	    	              System.gc();
	    	          } catch (IOException e) {
	    	    	    // can't write to file 
	    	          }
	    	          
	    	          if(driverModePref) {
		    	          // show driver the extra fields
		    	          Intent extraFieldsActivity = new Intent(Signature.this.getBaseContext(), extraFields.class);
		    	          Bundle b = new Bundle();
		      		      b.putString("orderNumber", filenamePrefix);
		      		      extraFieldsActivity.putExtras(b);
		                  startActivity(extraFieldsActivity);
	    	          }
	                  
	        	      ((Activity) view.getContext()).setResult(RESULT_OK);
	        	      ((Activity) view.getContext()).finish();
	        	  
	          }
	          else {
	        	  missingNameAlertBox  = new AlertDialog.Builder(view.getContext());
	        	  missingNameAlertBox.setMessage("Please enter a Signer Name before continuing");
	        	  missingNameAlertBox.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	                  @Override
					public void onClick(DialogInterface arg0, int arg1) {
	
	                  }
	        	    });
	        	 missingNameAlertBox.show();
	          }
          }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
			public void onClick(View view) {
                // clear signature
            	sigPanel.clearPanel();
            }
        });
        setContentView(RelLayout);

    }
    
    @Override
    protected void onStart() {
        super.onStart();
        getPrefs();
    }
    
    @Override
	protected void onResume() {
        super.onResume();
        //sigPanel.onResumePanel();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        //sigPanel.onPausePanel();
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
        printerQueuePref = prefs.getString("printerQueuePref", "1");
    }
    
}
