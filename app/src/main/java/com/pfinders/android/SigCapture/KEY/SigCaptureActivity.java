package com.pfinders.android.SigCapture.KEY;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import com.pfinders.android.SigCapture.KEY.LoginWrapper.HttpClientFactory;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class SigCaptureActivity extends Activity {
    /**
     * Called when the activity is first created.
     */

    public static final String TAG = "Signature Capture";
    static final int SIGNATURE_WRITTEN = 1;
    static final int POST_ERROR = 0;
    static final int POST_OK = 1;

    static final int LOGIN_ERROR_MESSAGE = 0;
    static final int LOGIN_SUCCESS_MESSAGE = 1;
    static final int LOGIN_LOGOUT_MESSAGE = 2;
    static final int LOGINFORM_CREATE = 3;
    static final int PREFERENCES_CREATE = 100;
    static final int RESULT_PREFS_CHANGED = 101;
    String CHECK_login = "";

    static final int TIMEOUT_ERROR_MESSAGE = 404;
    static final int TIMEOUT_VALUE = 30000; // 30 Second timeout, change timeout
    // setting here and in LoginWrapper

    private String serverURLGetOrder; // data lookups
    private String serverURLUpload; // file uploads
    private String serverURLDownload; // file downloads
    private static String sdDir = Environment.getExternalStorageDirectory()
            .toString();

    private static File APP_FILE_PATH = new File(sdDir + "/sigcapture/orders");
    // private LayoutInflater mInflater;
    private Vector<RowData> data;
    // private CustomAdapter adapter;
    private ArrayList<HashMap<String, String>> orderInformationList = new ArrayList<HashMap<String, String>>();
    ;
    private SimpleAdapter sa;

    private String filenamePrefix; // for matching .txt and .jpg files
    private ProgressDialog pd; // for loading please wait messages
    private AlertDialog.Builder confirmbox;
    private String currentOrderNumber = "";

    boolean workOfflinePref;
    String serverHostnamePref;
    boolean vibrateOnLookupPref;
    boolean driverModePref;
    String userIdPref;
    String passwordPref;
    String companyNamePref;

    DefaultHttpClient httpclient;
    HttpResponse response;
    HttpContext localContext;
    boolean _hasCookie = false;
    EditText orderNumber;

    private LoginWrapper loginWrapper;
    private boolean hasLookupOrderTask = false, hasUploadFileTask = false,
            hasUploadMultipleFileTask = false, hasDownloadDataTask = false;
    private Handler handleLoginChanges = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == LOGIN_ERROR_MESSAGE) {
                loginWrapper.setIsLoggedIn(false);
                loginWrapper.generateLoginAlertDialog(msg.what);
            } else if (msg.what == LOGIN_SUCCESS_MESSAGE) {
                loginWrapper.setIsLoggedIn(true);

                if (hasLookupOrderTask) {
                    // Online Lookup of order number to get client name
                    GetOrderInfoTask orderInfoTask = new GetOrderInfoTask();
                    orderInfoTask.execute(currentOrderNumber);
                    hasLookupOrderTask = false;
                } else if (hasUploadFileTask) {
                    UploadFileTask uploadFiletask = new UploadFileTask();
                    if (driverModePref) {
                        uploadFiletask.execute(filenamePrefix + ".txt",
                                filenamePrefix + ".jpg", filenamePrefix
                                        + ".not", filenamePrefix + ".sig");
                    } else {
                        uploadFiletask.execute(filenamePrefix + ".txt",
                                filenamePrefix + ".jpg", filenamePrefix
                                        + ".sig");
                    }
                    hasUploadFileTask = false;
                } else if (hasUploadMultipleFileTask) {
                    UploadAllFilesTask uploadAllFilesTask = new UploadAllFilesTask();
                    uploadAllFilesTask.execute();
                    hasUploadMultipleFileTask = false;
                } else if (hasDownloadDataTask) {
                    DownloadAllOrderRecordsTask downloadAllOrderRecordsTask = new DownloadAllOrderRecordsTask();
                    downloadAllOrderRecordsTask.execute();
                    hasDownloadDataTask = false;
                }
            } else if (msg.what == LOGIN_LOGOUT_MESSAGE) {
                createLoginFormActivity();
            } else if (msg.what == TIMEOUT_ERROR_MESSAGE) {
                loginWrapper.generateLoginAlertDialog(TIMEOUT_ERROR_MESSAGE);
            }
        }
    };

    private DatabaseWrapper databaseWrapper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ActionBar bar = getActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#191919")));

        ListView lv = (ListView) this.findViewById(R.id.orderList);
        // mInflater = (LayoutInflater)
        // getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        data = new Vector<RowData>();
        // adapter = new CustomAdapter(this, R.layout.orderlistitem, R.id.item,
        // data);
        sa = new SimpleAdapter(getApplicationContext(), orderInformationList,
                R.layout.orderinformation, new String[]{"left", "right"},
                new int[]{R.id.orderLeft, R.id.orderRight}) {
        };
        // lv.setAdapter(adapter);
        lv.setAdapter(sa);
        lv.setItemsCanFocus(true);
        lv.setTextFilterEnabled(true);

        loginWrapper = new LoginWrapper(this);
        databaseWrapper = new DatabaseWrapper(this);

        loginWrapper.registerHandler(handleLoginChanges);
        // progressDialogDownload = new ProgressDialog(this);
        // loginWrapper.attemptLogin(false);

        if (savedInstanceState == null) {
            // we were just launched: set up app

        } else {
            // we are being restored: resume stuff
            // restoreState(savedInstanceState);
        }
        makeSDCardFolder(APP_FILE_PATH);
        Button nextButton = (Button) findViewById(R.id.nextButton);
        Button clearButton = (Button) findViewById(R.id.clearButton);

        orderNumber = (EditText) findViewById(R.id.orderNumber);
        orderNumber.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    // Force close keyboard
                    InputMethodManager inputMethodManager = (InputMethodManager) SigCaptureActivity.this
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(
                            SigCaptureActivity.this.getCurrentFocus()
                                    .getWindowToken(), 0);

                    // add string to list and clear value, throw away enter key
                    EditText orderNumber = (EditText) findViewById(R.id.orderNumber);
                    String localCurrentOrderNumber = orderNumber.getText()
                            .toString();
                    orderNumber.setText("");
                    if (localCurrentOrderNumber.length() > 0) {
                        setCurrentOrderNumber(localCurrentOrderNumber);
                        getPrefs();

                        if (workOfflinePref
                                || loginWrapper.isOnline(getBaseContext()) == false) {
                            // Offline database lookup
                            String orderInformation = databaseWrapper
                                    .queryOrderInformation(currentOrderNumber);
                            if (orderInformation
                                    .equals("Error|Order not found")) {
                                Toast.makeText(getBaseContext(),
                                        "Order not found", Toast.LENGTH_LONG)
                                        .show();
                            } else {
                                RowData rd = new RowData(orderInformation
                                        .split(" ")[0], orderInformation
                                        .split(" ")[1]);
                                data.add(rd);

                                // adapter.notifyDataSetChanged();

                                String[] orderInfoSplit = orderInformation
                                        .split("\t");
                                ArrayList<String> left = new ArrayList<String>();
                                ArrayList<String> right = new ArrayList<String>();

                                for (String innerOrderInfo : orderInfoSplit) {
                                    StringTokenizer tokens = new StringTokenizer(
                                            innerOrderInfo, "^");
                                    if (tokens.hasMoreTokens()) {
                                        left.add(tokens.nextToken().toString());
                                    }

                                    if (tokens.hasMoreTokens()) {
                                        right.add(tokens.nextToken().toString());
                                    }

                                }

                                if (left.size() > right.size()) {
                                    for (int loopCounter = 0; loopCounter < left
                                            .size(); loopCounter++) {
                                        HashMap<String, String> orderInformationRow = new HashMap<String, String>();

                                        orderInformationRow.put("left",
                                                left.get(loopCounter));

                                        if (right.size() > loopCounter) {
                                            orderInformationRow.put("right",
                                                    right.get(loopCounter));
                                        }

                                        orderInformationList
                                                .add(orderInformationRow);
                                    }
                                } else if (right.size() > left.size()) {
                                    for (int loopCounter = 0; loopCounter < right
                                            .size(); loopCounter++) {
                                        HashMap<String, String> orderInformationRow = new HashMap<String, String>();
                                        if (left.size() > loopCounter) {
                                            orderInformationRow.put("left",
                                                    left.get(loopCounter));
                                        }
                                        orderInformationRow.put("right",
                                                right.get(loopCounter));
                                        orderInformationList
                                                .add(orderInformationRow);
                                    }
                                }
                                // orderInformationRow.clear();
                                sa.notifyDataSetChanged();

                                setOrderNumber();

                                if (vibrateOnLookupPref) {
                                    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                    vibe.vibrate(300);
                                }
                            }
                        } else if (workOfflinePref == false
                                && loginWrapper.isOnline(getBaseContext())) {
                            // Online Lookup of order number to get client name
                            GetOrderInfoTask orderInfoTask = new GetOrderInfoTask();
                            orderInfoTask.execute(currentOrderNumber);
                        }
                    }
                    return true;
                }
                return false;
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setOrderNumber(); // sets filenamePrefix
                if (filenamePrefix == null) {
                    Context context = getApplicationContext();
                    orderNumber.setText("");
                    Toast.makeText(context, "Enter order number first",
                            Toast.LENGTH_SHORT).show();
                } else {
                    // pop up signing pad activity
                    Intent panelActivity = new Intent(getBaseContext(),
                            Signature.class);
                    Bundle b = new Bundle();
                    b.putString("orderNumber", filenamePrefix);
                    panelActivity.putExtras(b);
                    startActivityForResult(panelActivity, SIGNATURE_WRITTEN);
                }
            }
        });
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                data.clear();
                // adapter.notifyDataSetChanged();
                orderInformationList.clear();
                sa.notifyDataSetChanged();

                setOrderNumber(); // filenamePrefix set to null
            }
        });
        // getOverflowMenu();
        SharedPreferences shared = getSharedPreferences("Signature_capture", MODE_PRIVATE);
        CHECK_login = (shared.getString("Check_login", "nodata"));
        if (CHECK_login.contentEquals("nodata")) {
            Intent loginActivity = new Intent(getBaseContext(), LoginActivity.class);
            startActivityForResult(loginActivity, LOGINFORM_CREATE);

        }

    }

    // private void getOverflowMenu() {
    //
    // try {
    // ViewConfiguration config = ViewConfiguration.get(this);
    // Field menuKeyField = ViewConfiguration.class
    // .getDeclaredField("sHasPermanentMenuKey");
    // if (menuKeyField != null) {
    // menuKeyField.setAccessible(true);
    // menuKeyField.setBoolean(config, false);
    // }
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }
    public void setCurrentOrderNumber(String currentOrderNumber) {
        if (currentOrderNumber.length() > 0) {
            this.currentOrderNumber = currentOrderNumber;
        }
    }

    @Override
    public void onBackPressed() {
        // TODO Auto-generated method stub
        super.onBackPressed();
        if (filenamePrefix == null) {
            Context context = getApplicationContext();
            orderNumber.setText("");
            // Toast.makeText(context, "Enter order number first",
            // Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        getPrefs();
    }

    @Override
    protected void onResume() {
        super.onResume();

        getPrefs();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Read values from the "savedInstanceState"-object and put them in your
        // textview
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save the values you need from your textview into "outState" object
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // attach a menu layout to phone menu button
        getMenuInflater().inflate(R.menu.navigation_sigcap_menu, menu);
        // MenuInflater inflater = getMenuInflater();
        // inflater.inflate(R.menu.sigcap_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent settingsActivity = new Intent(getBaseContext(),
                        Preferences.class);
                startActivityForResult(settingsActivity, PREFERENCES_CREATE);
                return true;
            case R.id.menu_login:
                getPrefs();
                loginWrapper.attemptLogin(false);
                return true;
            case R.id.menu_logout:
                loginWrapper.logout();
                SharedPreferences shared = getSharedPreferences("Signature_capture", MODE_PRIVATE);
                SharedPreferences.Editor editor = shared.edit();
                editor.clear();
                editor.commit();
                data.clear();
                // adapter.notifyDataSetChanged();
                orderInformationList.clear();
                sa.notifyDataSetChanged();

                setOrderNumber();
                return true;
            case R.id.menu_data:
                confirmbox = new AlertDialog.Builder(this);
                confirmbox
                        .setMessage("Would you like to update your offline records?");
                confirmbox.setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                // upload all files as sets
                                getPrefs();

                                if (workOfflinePref) {
                                    hasDownloadDataTask = true;
                                    loginWrapper.attemptLogin(false);
                                } else {
                                    DownloadAllOrderRecordsTask downloadAllOrderRecordsTask = new DownloadAllOrderRecordsTask();
                                    downloadAllOrderRecordsTask.execute();
                                }
                            }
                        });
                confirmbox.setNegativeButton("No",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {

                            }
                        });
                confirmbox.show();
                return true;
            case R.id.menu_upload:
                if (countSavedSignatures(APP_FILE_PATH) > 0) {
                    confirmbox = new AlertDialog.Builder(this);
                    confirmbox.setMessage("Upload "
                            + countSavedSignatures(APP_FILE_PATH) + " signatures?");
                    confirmbox.setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    // upload all files as sets
                                    getPrefs();
                                    int counter = countSavedSignatures(APP_FILE_PATH);
                                    SharedPreferences sp = getSharedPreferences(
                                            "my_preference", Activity.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sp.edit();
                                    editor.putInt("counter_value", counter);
                                    editor.commit();
                                    if (workOfflinePref) {
                                        hasUploadMultipleFileTask = true;
                                        loginWrapper.attemptLogin(false);
                                    } else {
                                        UploadAllFilesTask uploadAllFilesTask = new UploadAllFilesTask();
                                        uploadAllFilesTask.execute();
                                    }
                                }
                            });
                    confirmbox.setNegativeButton("No",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {

                                }
                            });
                    confirmbox.show();
                } else {
                    confirmbox = new AlertDialog.Builder(this);
                    // confirmbox.setMessage("No signatures to upload at this time");
                    SharedPreferences sp = getSharedPreferences("my_preference",
                            Activity.MODE_PRIVATE);

                    int counterr = sp.getInt("counter_value", 0);
                    if (counterr == 1) {
                        confirmbox.setMessage(counterr + " signature uploaded");
                    } else if (counterr == 0) {
                        confirmbox
                                .setMessage("No signatures to upload at this time");
                    } else {
                        confirmbox.setMessage(counterr + " signatures uploaded");
                    }
                    confirmbox.setPositiveButton("Ok",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {

                                }
                            });
                    confirmbox.show();
                }
                return true;
            case R.id.menu_info:
                Intent showInfoActivity = new Intent(getBaseContext(),
                        ShowInfo.class);
                startActivity(showInfoActivity);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void createLoginFormActivity() {
        Intent loginActivityOnLogout = new Intent(getBaseContext(),
                LoginActivity.class);
        startActivityForResult(loginActivityOnLogout, LOGINFORM_CREATE);
    }

    public void showInfo() {
        Context context = getApplicationContext();
        Toast.makeText(context, "added row", Toast.LENGTH_SHORT).show();
    }

    public boolean makeSDCardFolder(File sdPath) {
        try {
            if (!sdPath.exists()) {
                sdPath.mkdirs();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void setOrderNumber() {
        // return string of 1st order number for filename

        int i;
        String key;
        StringBuilder orderListBuilder = new StringBuilder();
        filenamePrefix = null;

        for (i = 0; i < data.size(); i++) {
            key = data.elementAt(i).getKey() + "\n";
            orderListBuilder.append(key);
        }
        if (data.size() > 0) {
            filenamePrefix = data.elementAt(0).getKey(); // save for later
        }
    }

    public void saveOrder() {
        // write order as .txt file

        int i;
        String rowInfo;
        StringBuilder orderListBuilder = new StringBuilder();
        String fileName;

        for (i = 0; i < data.size(); i++) {
            rowInfo = data.elementAt(i).getKey() + "|"
                    + data.elementAt(i).getValue() + "\n";
            orderListBuilder.append(rowInfo);
        }
        if (data.size() > 0) {
            fileName = filenamePrefix + ".txt";
            try {
                FileWriter out = new FileWriter(new File(APP_FILE_PATH,
                        fileName));
                out.write(orderListBuilder.toString());
                out.close();
                out = null;
                System.gc();
            } catch (IOException e) {
                Context context = getApplicationContext();
                Toast.makeText(context,
                        "Error: Cannot save data to " + fileName,
                        Toast.LENGTH_SHORT).show();
            }
        }

    }

    private class RowData {
        protected String mItem;
        protected String mValue;

        RowData(String item, String description) {
            mItem = item;
            mValue = description;
        }

        public String getKey() {
            return mItem;
        }

        public String getValue() {
            return mValue;
        }

        @Override
        public String toString() {
            return mItem + " " + mValue;
        }
    }

	/*
     * private class CustomAdapter extends ArrayAdapter<RowData> {
	 * 
	 * public CustomAdapter(Context context, int resource, int
	 * textViewResourceId, List<RowData> objects) { super(context, resource,
	 * textViewResourceId, objects);
	 * 
	 * }
	 * 
	 * @Override public View getView(int position, View convertView, ViewGroup
	 * parent) { ViewHolder holder = null;
	 * 
	 * //widgets displayed by each item in your list TextView item = null;
	 * TextView description = null;
	 * 
	 * //data from your adapter RowData rowData= getItem(position);
	 * 
	 * 
	 * //we want to reuse already constructed row views... if (null ==
	 * convertView) { convertView = mInflater.inflate(R.layout.orderlistitem,
	 * null); holder = new ViewHolder(convertView); convertView.setTag(holder);
	 * } // holder = (ViewHolder) convertView.getTag(); item = holder.getItem();
	 * item.setText(rowData.mItem);
	 * 
	 * description = holder.getDescription();
	 * description.setText(rowData.mValue);
	 * 
	 * return convertView; } }
	 * 
	 * /** Wrapper for row data.
	 */
    /*
	 * private class ViewHolder { private View mRow; private TextView
	 * description = null; private TextView item = null;
	 * 
	 * public ViewHolder(View row) { mRow = row; }
	 * 
	 * public TextView getDescription() { if(null == description){ description =
	 * (TextView) mRow.findViewById(R.id.description); } return description; }
	 * 
	 * public TextView getItem() { if(null == item){ item = (TextView)
	 * mRow.findViewById(R.id.item); } return item; } }
	 */

    private void getPrefs() {
        // load the applications preferences
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());
        workOfflinePref = prefs.getBoolean("workOfflinePref", false);
        serverHostnamePref = prefs.getString("serverHostnamePref",
                "test.westpacsupply.com");
        userIdPref = prefs.getString("userIdPref", "");
        passwordPref = prefs.getString("passwordPref", "");
        vibrateOnLookupPref = prefs.getBoolean("vibrateOnLookupPref", true);
        driverModePref = prefs.getBoolean("driverModePref", false);
        companyNamePref = prefs.getString("companyNamePref", "WestPac Supply");
        serverURLGetOrder = "http://" + serverHostnamePref
                + "/sigcap/lookup.php";
        serverURLUpload = "http://" + serverHostnamePref + "/sigcap/load.php";
        serverURLDownload = "http://" + serverHostnamePref
                + "/sigcap/download.php";

        TextView txtCompanyName = (TextView) findViewById(R.id.CompanyName);
        txtCompanyName.setText(companyNamePref);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intentData) {
        if (requestCode == SIGNATURE_WRITTEN) {
            if (resultCode == RESULT_OK) {
                saveOrder(); // as .txt file
                if (workOfflinePref || !loginWrapper.isOnline(getBaseContext())) {
                    // Offline mode: files alread saved on sdcard, clear data
                    // for next order number
                    data.clear();
                    orderInformationList.clear();
                    // adapter.notifyDataSetChanged();
                    sa.notifyDataSetChanged();
                    setOrderNumber(); // sets filenamePrefix to null
                } else {
                    // Online mode: http file upload a set of files now
                    UploadFileTask uploadFiletask = new UploadFileTask();
                    if (driverModePref) {
                        uploadFiletask.execute(filenamePrefix + ".txt",
                                filenamePrefix + ".jpg", filenamePrefix
                                        + ".not", filenamePrefix + ".sig");
                    } else {
                        uploadFiletask.execute(filenamePrefix + ".txt",
                                filenamePrefix + ".jpg", filenamePrefix
                                        + ".sig");
                    }
                }
            }

        } else if (requestCode == LOGINFORM_CREATE) {
            if (resultCode == RESULT_OK) {
                loginWrapper.setIsLoggedIn(true);
                SharedPreferences shared = getSharedPreferences("Signature_capture", MODE_PRIVATE);
                SharedPreferences.Editor editor = shared.edit();
                editor.putString("Check_login", "Login");

                editor.commit();
                getPrefs();
                // loginWrapper.generateLoginAlertDialog(LOGIN_SUCCESS_MESSAGE);

                // Check if there are saved signatures that need to be uploaded
				/*
				 * if(countSavedSignatures(APP_FILE_PATH) > 0) {
				 * uploadAllFilesTask uploadAllFilesTask = new
				 * uploadAllFilesTask(); uploadAllFilesTask.execute(); }
				 */
            }
        } else if (requestCode == PREFERENCES_CREATE) {
            if ((resultCode == RESULT_OK && loginWrapper.isLoggedIn() == false)
                    || resultCode == RESULT_PREFS_CHANGED) {
                getPrefs();

                if (workOfflinePref == false) {
                    loginWrapper.attemptLogin(false);
                }
            }
        }
    }

    public Integer countSavedSignatures(File sigsDir) {
        if (!sigsDir.isDirectory()) {
            return 0;
        }
        Integer txtCounter = 0;
        File[] sigsList = sigsDir.listFiles();
        for (int i = 0; i < sigsList.length; i++) {
            if (sigsList[i].getName().endsWith(".txt")) {
                txtCounter++;
            }
        }
        return txtCounter;
    }

    public String postData(String url, HashMap<String, String> dataPairs) {
        // Create a new HttpClient and Post fields, return a pipe separated
        // string
        DefaultHttpClient httpclient = HttpClientFactory.getThreadSafeClient();
        HttpPost httppost = new HttpPost(url);

        CookieStore localCookieStore = loginWrapper.getCookies();
        if (localCookieStore.getCookies().size() > 0) {
            httpclient.setCookieStore(localCookieStore);
        }

        // Create local HTTP context
        HttpContext localContext = new BasicHttpContext();
        // Bind custom cookie store to the local context
        localContext.setAttribute(ClientContext.COOKIE_STORE,
                httpclient.getCookieStore());

        String result = "";

        try {
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            if (dataPairs.containsKey("order")) {
                nameValuePairs.add(new BasicNameValuePair("order", dataPairs
                        .get("order")));
            }
            // nameValuePairs.add(new BasicNameValuePair("password",
            // "abcdefg12345"));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost, localContext);

            if (response == null) {
                result = "Error";
            } else {
                try {
                    InputStream in = response.getEntity().getContent();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(in));
                    StringBuilder str = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        str.append(line + "\n");
                    }
                    in.close();
                    result = str.toString();
                } catch (Exception ex) {
                    result = "Error";
                }
            }

        } catch (ClientProtocolException e) {
            result = "Error|Network error occured during upload";
        } catch (IOException e) {
            result = "Error|Network timeout occured, please try again later";
        }
        // do something with response
        return result;
    }

    private class GetOrderInfoTask extends AsyncTask<String, Void, String> {
        // POST order number to web page and return customer name
        // If no error then its a valid order number, add to list
        @Override
        protected void onPreExecute() {
            pd = ProgressDialog.show(SigCaptureActivity.this, "",
                    "Please wait...", true);
        }

        @Override
        protected String doInBackground(String... orders) {
            String response = "";
            if (orders.length == 1) {
                HashMap<String, String> keyValuePairs = new HashMap<String, String>();
                keyValuePairs.put("order", orders[0]);
                response = postData(serverURLGetOrder, keyValuePairs);
            } else {
                for (String order : orders) {
                    HashMap<String, String> keyValuePairs = new HashMap<String, String>();
                    keyValuePairs.put("order", order);
                    response = postData(serverURLGetOrder, keyValuePairs);
                }
            }
            return response;
        }

        @Override
        protected void onPostExecute(String results) {
            pd.dismiss();
            Log.d(TAG, results); // Herb debug
            String[] info = results.split("\\|");
            if (info[0].equals("Error")) {
                if (info[1].contains("NLI")) {
                    loginWrapper.logout();
                    // createLoginFormActivity();
                } else if (info[1]
                        .contains("Network error occured during upload")) {
                    loginWrapper.logout();
                    // createLoginFormActivity();
                } else {
                    // order number does not exist
                    Context context = getApplicationContext();
                    Toast.makeText(context, info[1], Toast.LENGTH_SHORT).show();
                    if (vibrateOnLookupPref) {
                        // Vibrator vibe = (Vibrator)
                        // getSystemService(Context.VIBRATOR_SERVICE);
                        // vibe.vibrate(200);
                    }
                }
            } else {
                // OK, update list on screen

                RowData rd = new RowData(info[0], info[1]);
                data.add(rd);
                // adapter.notifyDataSetChanged();

                String[] orderInfoSplit = info[1].split("\t");
                ArrayList<String> left = new ArrayList<String>();
                ArrayList<String> right = new ArrayList<String>();

                for (String innerOrderInfo : orderInfoSplit) {
                    StringTokenizer tokens = new StringTokenizer(
                            innerOrderInfo, "^");
                    if (tokens.hasMoreTokens()) {
                        left.add(tokens.nextToken().toString());
                    }

                    if (tokens.hasMoreTokens()) {
                        right.add(tokens.nextToken().toString());
                    }

                }

                if (left.size() > right.size()) {
                    for (int loopCounter = 0; loopCounter < left.size(); loopCounter++) {
                        HashMap<String, String> orderInformationRow = new HashMap<String, String>();

                        orderInformationRow.put("left", left.get(loopCounter));

                        if (right.size() > loopCounter) {
                            orderInformationRow.put("right",
                                    right.get(loopCounter));
                        }

                        orderInformationList.add(orderInformationRow);
                    }
                } else if (right.size() > left.size()) {
                    for (int loopCounter = 0; loopCounter < right.size(); loopCounter++) {
                        HashMap<String, String> orderInformationRow = new HashMap<String, String>();
                        if (left.size() > loopCounter) {
                            orderInformationRow.put("left",
                                    left.get(loopCounter));
                        }
                        orderInformationRow
                                .put("right", right.get(loopCounter));
                        orderInformationList.add(orderInformationRow);
                    }
                }
                // orderInformationRow.clear();
                sa.notifyDataSetChanged();

                setOrderNumber(); // sets filenamePrefix
                if (vibrateOnLookupPref) {
                    // one vibrate is good
                    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    vibe.vibrate(300);
                }
            }
        }
    }

    private class DownloadAllOrderRecordsTask extends
            AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            // progressDialogDownload.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pd = ProgressDialog.show(SigCaptureActivity.this, "",
                    "Please wait...", true);
            // progressDialogDownload.setProgress(0);
            // progressDialogDownload.setMax(150);
            // progressDialogDownload.setMessage("Please wait...");
            // progressDialogDownload.show();
        }

        @Override
        protected String doInBackground(Void... unused) {
            // Create a new HttpClient and Post fields, return a pipe separated
            // string
            DefaultHttpClient httpclient = HttpClientFactory
                    .getThreadSafeClient();

            CookieStore localCookieStore = loginWrapper.getCookies();
            if (localCookieStore.getCookies().size() > 0) {
                httpclient.setCookieStore(localCookieStore);

                // Create local HTTP context
                localContext = new BasicHttpContext();
                // Bind custom cookie store to the local context
                localContext.setAttribute(ClientContext.COOKIE_STORE,
                        httpclient.getCookieStore());

                _hasCookie = true;
            }

            HttpGet httpGet = new HttpGet(serverURLDownload);

            String result = "";

            try {
                // Execute HTTP Post Request utilizing the localContext and
                // cookieStore initialized in the constructor
                if (_hasCookie) {
                    response = httpclient.execute(httpGet, localContext);
                } else {
                    response = httpclient.execute(httpGet);
                }

                if (response == null) {
                    result = "Error";
                } else {
                    try {
                        InputStream in = response.getEntity().getContent();
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(in));
                        StringBuilder str = new StringBuilder();
                        String line = null;
                        while ((line = reader.readLine()) != null) {

                            str.append(line + "\n");

                        }
                        in.close();
                        result = str.toString();
                    } catch (Exception ex) {
                        result = "Error: Generic Exception";
                    }
                }
            } catch (ClientProtocolException e) {
                result = "Error|Network error occured during upload";
            } catch (IOException e) {
                result = "Error|Network timeout occured, please try again later";
            }

            return result;
        }

        @Override
        protected void onPostExecute(String results) {
            pd.dismiss();
            String[] info = results.split("\\|");
            if (info[0].equals("Error")) {
                if (info[1].contains("NLI")) {
                    loginWrapper.logout();
                    // createLoginFormActivity();
                } else if (info[1]
                        .contains("Network error occured during upload")) {
                    loginWrapper.logout();
                    // createLoginFormActivity();
                } else {
                    // Other download error occured
                    Context context = getApplicationContext();
                    Toast.makeText(context, info[1], Toast.LENGTH_SHORT).show();
                }
            } else {
                // Load records into local database
				/*
				 * if(results.length() > 200) { Toast.makeText(getBaseContext(),
				 * results.substring(0, 200), Toast.LENGTH_SHORT).show(); } else
				 * { Toast.makeText(getBaseContext(), results,
				 * Toast.LENGTH_SHORT).show(); }
				 */
                databaseWrapper.loadRecordsFromWeb(results);
            }
        }
    }

    private class UploadFileTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            pd = ProgressDialog.show(SigCaptureActivity.this, "",
                    "Saving, Please wait", true);
        }

        @Override
        protected String doInBackground(String... files) {
            String response = "";

            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";

            for (String filename : files) {

                try {
                    URL url = new URL(serverURLUpload);
                    BufferedInputStream fileBuffer = new BufferedInputStream(
                            new FileInputStream(APP_FILE_PATH + "/" + filename));
                    HttpURLConnection conn = (HttpURLConnection) url
                            .openConnection();

                    conn.setConnectTimeout(TIMEOUT_VALUE);
                    conn.setReadTimeout(TIMEOUT_VALUE);

                    CookieStore localCookieStore = loginWrapper.getCookies();

                    if (localCookieStore.getCookies().size() > 0) {
                        for (Cookie cookie : localCookieStore.getCookies()) {
                            conn.setRequestProperty("Cookie", cookie.getName()
                                    + "=" + cookie.getValue());
                        }
                    }

                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);

                    try {
                        // Use a post method.
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type",
                                "multipart/form-data;boundary=" + boundary);

                        DataOutputStream dos = new DataOutputStream(
                                conn.getOutputStream());

                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"thefile\";filename=\""
                                + filename + "\"" + lineEnd);
                        dos.writeBytes(lineEnd);

                        // create a buffer of maximum size

                        int bytesAvailable = fileBuffer.available();
                        int maxBufferSize = 1024;
                        int bufferSize = Math
                                .min(bytesAvailable, maxBufferSize);
                        byte[] buffer = new byte[bufferSize];

                        // read file and write it into form...

                        int bytesRead = fileBuffer.read(buffer, 0, bufferSize);

                        while (bytesRead > 0) {
                            dos.write(buffer, 0, bufferSize);
                            bytesAvailable = fileBuffer.available();
                            bufferSize = Math
                                    .min(bytesAvailable, maxBufferSize);
                            bytesRead = fileBuffer.read(buffer, 0, bufferSize);
                        }

                        // send multipart form data necesssary after file
                        // data...

                        dos.writeBytes(lineEnd);
                        dos.writeBytes(twoHyphens + boundary + twoHyphens
                                + lineEnd);

                        // close streams
                        fileBuffer.close();
                        dos.flush();

                        InputStream is = conn.getInputStream();
                        // retrieve the response from server
                        int ch;

                        StringBuffer b = new StringBuffer();
                        while ((ch = is.read()) != -1) {
                            b.append((char) ch);
                        }
                        response += b.toString();
                        dos.close();
                    } catch (SocketTimeoutException e) {
                        response = "Error|Network timeout occured, please try again later";
                    }
                } catch (FileNotFoundException e) {
                    response = "Error|File not found to upload";
                } catch (MalformedURLException ex) {
                    response = "Error|Bad URL to upload to";
                } catch (IOException ioe) {
                    response = "Error|Network error occured during upload";
                }
            }
            return response;
        }

        @Override
        protected void onPostExecute(String results) {
            pd.dismiss();
            Context context = getApplicationContext();
            String[] info = results.split("\\|");

            if (info[0].equals("Error")) {
                // failed to upload one or more files
                if (info[1].contains("NLI")) {
                    loginWrapper.logout();
                    // createLoginFormActivity();
                } else if (info[1]
                        .contains("Network error occured during upload")) {
                    loginWrapper.logout();
                    // createLoginFormActivity();
                } else {
                    Toast.makeText(context, info[1], Toast.LENGTH_SHORT).show();
                }
            } else {

                if (driverModePref) {
                    if (results.equals("OKOKOKOK")) {
                        // 4 files uploaded, so delete local copies
                        File txt = new File(APP_FILE_PATH, filenamePrefix
                                + ".txt");
                        File jpg = new File(APP_FILE_PATH, filenamePrefix
                                + ".jpg");
                        File sig = new File(APP_FILE_PATH, filenamePrefix
                                + ".sig");
                        File note = new File(APP_FILE_PATH, filenamePrefix
                                + ".not");
                        txt.delete();
                        jpg.delete();
                        sig.delete();
                        note.delete();
                    }
                } else {
                    if (results.equals("OKOKOK")) {
                        // 3 files uploaded, so delete local copies
                        File txt = new File(APP_FILE_PATH, filenamePrefix
                                + ".txt");
                        File jpg = new File(APP_FILE_PATH, filenamePrefix
                                + ".jpg");
                        File sig = new File(APP_FILE_PATH, filenamePrefix
                                + ".sig");
                        if (!txt.delete()) {
                            Toast.makeText(
                                    context,
                                    "failed deleting " + filenamePrefix
                                            + ".txt", Toast.LENGTH_SHORT)
                                    .show();
                        }
                        jpg.delete();
                        sig.delete();

                    }
                }
                data.clear();
                orderInformationList.clear();
                // adapter.notifyDataSetChanged();
                sa.notifyDataSetChanged();
                setOrderNumber(); // sets filenamePrefix to null
            }
        }
    }

    private class UploadAllFilesTask extends AsyncTask<Void, Integer, String> {
        // group files together: txt, jpg, not, sig. Note: sig must be last to
        // upload
        // in the set to trigger activity on website properly.
        private ArrayList<String> orderFiles = new ArrayList<String>();
        // private ArrayList<String> uploadStatus = new ArrayList<String>();
        private int sigTotalCount = 0, sigCounter = 0;

        @Override
        protected void onPreExecute() {
            pd = ProgressDialog.show(SigCaptureActivity.this, "",
                    "Uploading signature(s) 1 out of "
                            + countSavedSignatures(APP_FILE_PATH), true);
        }

        @Override
        protected String doInBackground(Void... unused) {
            String response = "";

            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";

            if (APP_FILE_PATH.isDirectory()) {
                String fname, orderPrefix;
                File[] sigsList = APP_FILE_PATH.listFiles();
                for (int i = 0; i < sigsList.length; i++) {
                    fname = sigsList[i].getName();
                    if (fname.endsWith(".txt")) {
                        // beginning of a set of files found, check for entire
                        // set
                        orderPrefix = fname.substring(0, fname.length() - 4);
                        File jpg = new File(APP_FILE_PATH, orderPrefix + ".jpg");
                        File sig = new File(APP_FILE_PATH, orderPrefix + ".sig");
                        File note = new File(APP_FILE_PATH, orderPrefix
                                + ".not");
                        if ((jpg.exists()) && (sig.exists())) {
                            // found enough files for this set to be saved in
                            // list for upload
                            orderFiles.add(sigsList[i].getName()); // txt file
                            orderFiles.add(jpg.getName());
                            if (note.exists()) {
                                orderFiles.add(note.getName());
                            }
                            orderFiles.add(sig.getName());
                            sigTotalCount++;
                        }
                    }
                }
                for (int i = 0; i < orderFiles.size(); i++) {
                    // after upload write status to array

                    try {
                        // String upResponse = "";
                        URL url = new URL(serverURLUpload);
                        BufferedInputStream fileBuffer = new BufferedInputStream(
                                new FileInputStream(APP_FILE_PATH + "/"
                                        + orderFiles.get(i).toString()));
                        HttpURLConnection conn = (HttpURLConnection) url
                                .openConnection();

                        conn.setConnectTimeout(TIMEOUT_VALUE);
                        conn.setReadTimeout(TIMEOUT_VALUE);

                        CookieStore localCookieStore = loginWrapper
                                .getCookies();

                        if (localCookieStore.getCookies().size() > 0) {
                            for (Cookie cookie : localCookieStore.getCookies()) {
                                conn.setRequestProperty(
                                        "Cookie",
                                        cookie.getName() + "="
                                                + cookie.getValue());
                            }
                        }

                        conn.setDoInput(true);
                        conn.setDoOutput(true);
                        conn.setUseCaches(false);

                        try {
                            // Use a post method.
                            conn.setRequestMethod("POST");
                            conn.setRequestProperty("Content-Type",
                                    "multipart/form-data;boundary=" + boundary);

                            DataOutputStream dos = new DataOutputStream(
                                    conn.getOutputStream());

                            dos.writeBytes(twoHyphens + boundary + lineEnd);
                            dos.writeBytes("Content-Disposition: form-data; name=\"thefile\";filename=\""
                                    + orderFiles.get(i).toString()
                                    + "\""
                                    + lineEnd);
                            dos.writeBytes(lineEnd);

                            // create a buffer of maximum size

                            int bytesAvailable = fileBuffer.available();
                            int maxBufferSize = 1024;
                            int bufferSize = Math.min(bytesAvailable,
                                    maxBufferSize);
                            byte[] buffer = new byte[bufferSize];

                            // read file and write it into form...

                            int bytesRead = fileBuffer.read(buffer, 0,
                                    bufferSize);

                            while (bytesRead > 0) {
                                dos.write(buffer, 0, bufferSize);
                                bytesAvailable = fileBuffer.available();
                                bufferSize = Math.min(bytesAvailable,
                                        maxBufferSize);
                                bytesRead = fileBuffer.read(buffer, 0,
                                        bufferSize);
                            }

                            // send multipart form data necesssary after file
                            // data...

                            dos.writeBytes(lineEnd);
                            dos.writeBytes(twoHyphens + boundary + twoHyphens
                                    + lineEnd);

                            // close streams
                            fileBuffer.close();
                            dos.flush();

                            InputStream is = conn.getInputStream();
                            // retrieve the response from server
                            int ch;

                            StringBuffer b = new StringBuffer();
                            while ((ch = is.read()) != -1) {
                                b.append((char) ch);
                            }
                            response += b.toString();
                            dos.close();
                            // uploadStatus.add(i, upResponse);
                            // Log.d(TAG, upResponse);

                            if (orderFiles.get(i).toString().contains(".txt")) {
                                sigCounter++;
                                publishProgress(sigCounter, sigTotalCount);
                            }
                        } catch (SocketTimeoutException e) {
                            // Log.d("SigCapture", "Timeout occured");
                            response = "Error|Network timeout occured, please try again later";
                        }
                    } catch (FileNotFoundException e) {
                        // String upResponse = "Error|File not found to upload";
                        response = "Error|File not found to upload";
                        return response;
                        // uploadStatus.add(i, upResponse);
                    } catch (MalformedURLException ex) {
                        // String upResponse = "Error|Bad URL to upload to";
                        response = "Error|Bad URL to upload to";
                        return response;
                        // uploadStatus.add(i, upResponse);
                    } catch (IOException ioe) {
                        // String upResponse =
                        // "Error|Network error occured during upload";
                        response = "Error|Network error occured during upload";
                        return response;
                        // uploadStatus.add(i, upResponse);
                    }

                    // Log.d(TAG, response + "-results");

                    Context context = getApplicationContext();
                    String[] info = response.split("\\|");

                    if (info[0].equals("Error")) {
                        // failed to upload one or more files
                        if (info[1].contains("NLI")) {
                            return "Error|NLI";
                        } else {
                            Toast.makeText(context, info[1], Toast.LENGTH_SHORT)
                                    .show();
                        }
                    } else {
                        filenamePrefix = orderFiles
                                .get(i)
                                .toString()
                                .substring(
                                        0,
                                        orderFiles.get(i).toString().length() - 4);
                        if (driverModePref) {
                            if (response.equals("OKOKOKOK")) {
                                // 4 files uploaded, so delete local copies
                                File txt = new File(APP_FILE_PATH,
                                        filenamePrefix + ".txt");
                                File jpg = new File(APP_FILE_PATH,
                                        filenamePrefix + ".jpg");
                                File sig = new File(APP_FILE_PATH,
                                        filenamePrefix + ".sig");
                                File note = new File(APP_FILE_PATH,
                                        filenamePrefix + ".not");
                                txt.delete();
                                jpg.delete();
                                sig.delete();
                                note.delete();
                                response = "";
                            }
                        } else {
                            if (response.equals("OKOKOK")) {
                                // 3 files uploaded, so delete local copies
                                File txt = new File(APP_FILE_PATH,
                                        filenamePrefix + ".txt");
                                File jpg = new File(APP_FILE_PATH,
                                        filenamePrefix + ".jpg");
                                File sig = new File(APP_FILE_PATH,
                                        filenamePrefix + ".sig");
                                if (!txt.delete()) {
                                    Toast.makeText(
                                            context,
                                            "failed deleting " + filenamePrefix
                                                    + ".txt",
                                            Toast.LENGTH_SHORT).show();
                                }
                                jpg.delete();
                                sig.delete();
                                response = "";
                            }
                        }
                    }
                }
            }
            return response;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // super.onProgressUpdate(values);
            pd.setMessage("Uploading signature(s) " + values[0] + " out of "
                    + values[1]);
        }

        @Override
        protected void onPostExecute(String results) {
            pd.dismiss();

            Context context = getApplicationContext();
            String[] info = results.split("\\|");

            if (info[0].equals("Error")) {
                // failed to upload one or more files
                if (info[1].contains("NLI")) {
                    loginWrapper.logout();
                    // createLoginFormActivity();
                } else if (info[1]
                        .contains("Network error occured during upload")) {
                    loginWrapper.logout();
                    // createLoginFormActivity();
                } else {
                    Toast.makeText(context, info[1], Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}