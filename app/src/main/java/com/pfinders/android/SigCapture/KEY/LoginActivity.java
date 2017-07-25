package com.pfinders.android.SigCapture.KEY;

import java.io.File;
import java.lang.reflect.Field;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewConfiguration;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.PopupMenu;
import android.widget.TextView;

public class LoginActivity extends Activity {
	public static final String TAG = "Signature Capture";
	static final int LOGIN_ERROR_MESSAGE = 0;
	static final int LOGIN_SUCCESS_MESSAGE = 1;
	static final int LOGIN_LOGOUT_MESSAGE = 2;
	static final int LOGIN_NO_USERID_OR_PASSWORD = 4;
	static final int LOGIN_NO_USERID_OR_PASSWORD_LOGIN_FORM = 5;
	static final int LOGIN_ERROR_NO_CONNECTIVITY = 6;

	static final int LOGINFORM_CREATE = 3;
	static final int PREFERENCES_CREATE = 100;
	static final int RESULT_PREFS_CHANGED = 101;
	static final int TIMEOUT_ERROR_MESSAGE = 404;

	boolean workOfflinePref;
	String serverHostnamePref;
	boolean vibrateOnLookupPref;
	boolean driverModePref;
	String userIdPref;
	String passwordPref;
	String companyNamePref;

	private TextView _userId;
	private TextView _password;
	private TextView _companyName;
	private Button _loginButton;
	private CheckBox _saveCredentials;
	private AlertDialog.Builder confirmbox;

	private LoginWrapper loginWrapper;
	private Handler handleLoginChanges = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == LOGIN_ERROR_MESSAGE) {
				_loginButton.setEnabled(true);
				loginWrapper.generateLoginAlertDialog(LOGIN_ERROR_MESSAGE);
			} else if (msg.what == LOGIN_SUCCESS_MESSAGE) {
				if (_saveCredentials.isChecked()) {
					loginWrapper.applyCredentialPreferences(_userId.getText()
							.toString().trim(), _password.getText().toString()
							.trim());
				}

				Intent resultIntent = new Intent();
				setResult(Activity.RESULT_OK, resultIntent);
				loginWrapper.dismissProgressDialog();

				finish();
			} else if (msg.what == LOGIN_ERROR_NO_CONNECTIVITY) {
				_loginButton.setEnabled(true);
				loginWrapper
						.generateLoginAlertDialog(LOGIN_ERROR_NO_CONNECTIVITY);
			} else if (msg.what == TIMEOUT_ERROR_MESSAGE) {
				_loginButton.setEnabled(true);
				loginWrapper.generateLoginAlertDialog(TIMEOUT_ERROR_MESSAGE);
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loginform);

		_userId = (TextView) findViewById(R.id.userid);
		_password = (TextView) findViewById(R.id.password);
		_loginButton = (Button) findViewById(R.id.loginButton);
		_saveCredentials = (CheckBox) findViewById(R.id.checkBoxSavePreferences);
		_companyName = (TextView) findViewById(R.id.CompanyName1);
		ActionBar bar = getActionBar();
		bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#393B3B")));

		getPrefs();

		/*
		 * _userId.setOnKeyListener(new OnKeyListener() {
		 * 
		 * @Override public boolean onKey(View v, int keyCode, KeyEvent event) {
		 * if (keyCode == KeyEvent.KEYCODE_ENTER) { //Force close keyboard
		 * InputMethodManager inputMethodManager = (InputMethodManager)
		 * LoginActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
		 * inputMethodManager
		 * .hideSoftInputFromWindow(LoginActivity.this.getCurrentFocus
		 * ().getWindowToken(), 0); return true; } return true; } });
		 */

		/*
		 * _password.setOnKeyListener(new OnKeyListener() {
		 * 
		 * @Override public boolean onKey(View v, int keyCode, KeyEvent event) {
		 * if (keyCode == KeyEvent.KEYCODE_ENTER) { //Force close keyboard
		 * InputMethodManager inputMethodManager = (InputMethodManager)
		 * LoginActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
		 * inputMethodManager
		 * .hideSoftInputFromWindow(LoginActivity.this.getCurrentFocus
		 * ().getWindowToken(), 0); return true; } return true; } });
		 */
		//getOverflowMenu();
	}

//	private void getOverflowMenu() {
//
//		try {
//			ViewConfiguration config = ViewConfiguration.get(this);
//			Field menuKeyField = ViewConfiguration.class
//					.getDeclaredField("sHasPermanentMenuKey");
//			if (menuKeyField != null) {
//				menuKeyField.setAccessible(true);
//				menuKeyField.setBoolean(config, false);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

	@Override
	public void onStart() {
		super.onStart();
		loginWrapper = new LoginWrapper(this);
		loginWrapper.registerHandler(handleLoginChanges);

		String[] credentials = loginWrapper.getCredentialPreferences();
		if (credentials[0].length() > 0 && credentials[1].length() > 0) {
			_userId.setText(credentials[0]);
			_password.setText(credentials[1]);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// attach a menu layout to phone menu button
//		MenuInflater inflater = getMenuInflater();
//		inflater.inflate(R.menu.sigcap_login_menu, menu);
//		View menuItemView = findViewById(R.id.action_filters);
//		PopupMenu popupMenu = new PopupMenu(this, menuItemView);
//	    popupMenu.inflate(R.menu.sigcap_login_menu);
//	    popupMenu.show();
		getMenuInflater().inflate(R.menu.navigation, menu);
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
		case R.id.menu_info:
			Intent showInfoActivity = new Intent(getBaseContext(),
					ShowInfo.class);
			startActivity(showInfoActivity);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intentData) {
		if (requestCode == PREFERENCES_CREATE) {
			if ((resultCode == RESULT_OK && loginWrapper.isLoggedIn() == false)
					|| resultCode == RESULT_PREFS_CHANGED) {
				getPrefs();

				if (resultCode == RESULT_PREFS_CHANGED) {
					_userId.setText(userIdPref);
					_password.setText(passwordPref);
				}


			}
		}
	}

	private void getPrefs() {
		// load the applications preferences
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		workOfflinePref = prefs.getBoolean("workOfflinePref", false);
		serverHostnamePref = prefs.getString("serverHostnamePref",
				"www.plimptonhills.com");
		userIdPref = prefs.getString("userIdPref", "");
		passwordPref = prefs.getString("passwordPref", "");
		vibrateOnLookupPref = prefs.getBoolean("vibrateOnLookupPref", true);
		driverModePref = prefs.getBoolean("driverModePref", false);
		companyNamePref = prefs.getString("companyNamePref", "WestPac Supply");

		_companyName.setText(companyNamePref);
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

	public void login(View view) {
		if (_userId.getText().toString().length() > 0
				&& _password.getText().toString().length() > 0) {
			_loginButton.setEnabled(false);
			loginWrapper.attemptLogin(_userId.getText().toString().trim(),
					_password.getText().toString().trim(), false);
		} else {
			loginWrapper
					.generateLoginAlertDialog(LOGIN_NO_USERID_OR_PASSWORD_LOGIN_FORM);
		}
	}

	public void workOffline(View view) {
		loginWrapper.applyWorkOfflinePreference(true);
		Intent resultIntent = new Intent();
		setResult(Activity.RESULT_CANCELED, resultIntent);
		finish();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			moveTaskToBack(true);
			return false;
		} else if ((keyCode == KeyEvent.KEYCODE_ENTER)) {

			// Force close keyboard
			InputMethodManager inputMethodManager = (InputMethodManager) LoginActivity.this
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			inputMethodManager.hideSoftInputFromWindow(LoginActivity.this
					.getCurrentFocus().getWindowToken(), 0);
		}

		return super.onKeyDown(keyCode, event);
	}
}
