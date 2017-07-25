package com.pfinders.android.SigCapture.KEY;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

public class LoginWrapper {
	public static final String TAG = "Signature Capture";
	private static final int LOGIN_ERROR_MESSAGE = 0;
	private static final int LOGIN_SUCCESS_MESSAGE = 1;
	private static final int LOGIN_LOGOUT_MESSAGE = 2;
	private static final int LOGIN_NO_USERID_OR_PASSWORD = 4;
	private static final int LOGIN_NO_USERID_OR_PASSWORD_LOGIN_FORM = 5;
	private static final int LOGIN_ERROR_NO_CONNECTIVITY = 6;
	private static final int TIMEOUT_ERROR_MESSAGE = 404;
    private static final int TIMEOUT_VALUE = 30000; //30 Second timeout, change timeout setting here and SignatureCaptureActivity
	
	private static final boolean isDebug = false;
    private String serverLoginURL;

    private Context _baseContext = null;
	private HashMap<Handler, Integer> _handlers = new HashMap<Handler, Integer>();
	private LoginTask loginTask;
    private boolean _isLoggedIn = false;
    private boolean _isSuppliedCredentials = false, _isSilentLogin = false;
    private ProgressDialog progressDialogLoggingIn;
    private AlertDialog alertDialog;

    private boolean _isOffline;
    private String _serverHostname;
    private String _userId, _userIdSupplied;
    private String _password, _passwordSupplied;
    
    private CookieManager cookieManager;
	DefaultHttpClient httpclient;
	HttpResponse response;
	HttpContext localContext;
	boolean _hasCookie = false; 

    public LoginWrapper(Context context) {
    	if(context == null) {
    		throw new NullPointerException();
    	}
    	
    	_baseContext = context;
    	
    	getPreferences();
    	
    	CookieSyncManager.createInstance(_baseContext);
    	cookieManager = CookieManager.getInstance();
    	
    	progressDialogLoggingIn = new ProgressDialog(_baseContext);
    	alertDialog = new AlertDialog.Builder(_baseContext).create();
    }
    
    /**
     * Attempts to login to specified host name with user provided login details
     */
    public void attemptLogin(boolean silentLogin) {
    	_isSuppliedCredentials = false;
    	_isSilentLogin = silentLogin;
    	
    	if(!isOnline(_baseContext)) {
    		sendMessageToHandlers(LOGIN_ERROR_NO_CONNECTIVITY);
    		applyWorkOfflinePreference(true);
    		return;
    	} else {
    		applyWorkOfflinePreference(false);
    	}
    	
    	getPreferences();
    	
    	if(!isUsernameAndPasswordValid(_userId, _password)) {
    		generateLoginAlertDialog(LOGIN_NO_USERID_OR_PASSWORD);
    		return;
    	}
    	
    	if(!_isOffline) { 
    		loginTask = new LoginTask();
			loginTask.execute();
    	}
    }
    
    /**
     * Overloaded login attempt with supplied userId and password
     */
    public void attemptLogin(String userId, String password, boolean silentLogin) {
    	_isSuppliedCredentials = true;
    	_isSilentLogin = silentLogin;
    	
    	_userIdSupplied = userId;
        _passwordSupplied = password;
    	
    	if(!isOnline(_baseContext)) {
    		sendMessageToHandlers(LOGIN_ERROR_NO_CONNECTIVITY);
    		applyWorkOfflinePreference(true);
    		return;
    	} else {
    		applyWorkOfflinePreference(false);
    	}
    	
    	getPreferences();
    	
    	if(!_isOffline && isUsernameAndPasswordValid(_userIdSupplied, _passwordSupplied)) { 
    		loginTask = new LoginTask();
			loginTask.execute(); 
    	}
    }
    
    private boolean isUsernameAndPasswordValid(String userId, String password) {
    	if(userId != null && userId.length() > 0) {
        	if(password != null && password.length() > 0) {
        		return true;
        	}	
        }
    	return false;
    }
    
    public void logout() {
    	//loginTask.cancel(true);
    	_isLoggedIn = false;
    	if(isOnline(_baseContext)) {
    		LogoutTask logoutTask = new LogoutTask();
    		logoutTask.execute();
    	} else {
    		removeCookies();
    		sendMessageToHandlers(LOGIN_LOGOUT_MESSAGE);
    	}
    }
    
    /**
     * Adds cookies to the perpetual state of application for reuse
     */
    private void setCookies(List<Cookie> cookies) {
        if (cookies != null) {
        	
        	if(isDebug) {
        		Log.d(TAG, "Cookie size: " + cookies.size());
        	}
        	
          for (Cookie cookie : cookies) { 
            String cookieString = cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain();
                        
            if(isDebug) {
            	Log.d(TAG, "Setting cookie: " + cookie.getDomain() + " " + cookie.getName() + " " + cookie.getValue() + " " + cookie.getExpiryDate());
            }
            
            cookieManager.setCookie(cookie.getDomain(), cookieString);
            CookieSyncManager.getInstance().sync();
          }
        }
	}
    
    /**
     * Synchronizes cookies with what is stored with the application 
     */
    public CookieStore getCookies() {
    	//CookieSyncManager.getInstance();
    	//cookieManager = CookieManager.getInstance();
    	cookieManager.removeExpiredCookie();
    	CookieStore cookieStore = new BasicCookieStore();
    	
    	if(cookieManager.getCookie(_serverHostname) != null) {
    		String[] cookieCrumbs = cookieManager.getCookie(_serverHostname).split("=");
    		Cookie recreatedCookie = new BasicClientCookie(cookieCrumbs[0], cookieCrumbs[1]);
    		((BasicClientCookie) recreatedCookie).setDomain(_serverHostname);
        	
    		if(isDebug) {
    			Log.d(TAG, recreatedCookie.getDomain() + " " + recreatedCookie.getName() + " " + recreatedCookie.getValue() + " " + recreatedCookie.getExpiryDate());
    		}
    		
        	cookieStore.addCookie(recreatedCookie);
        	cookieManager.setAcceptCookie(true);
    	}

    	return cookieStore;
    }
    
    public void removeCookies() {
    	//CookieSyncManager.getInstance();
    	//cookieManager = CookieManager.getInstance();
    	cookieManager.removeAllCookie();
    	CookieSyncManager.getInstance().sync();
    }
    
    public void dismissProgressDialog() {
    	progressDialogLoggingIn.dismiss();
    	progressDialogLoggingIn = null;
    }
    
	/**
     * Gets SharedPreferences and assigns them to local variables
     */
    private void getPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_baseContext);
        _isOffline = prefs.getBoolean("workOfflinePref", false);
        _serverHostname = prefs.getString("serverHostnamePref", "test.westpacsupply.com");
        _userId = prefs.getString("userIdPref", "");
        _password = prefs.getString("passwordPref", "");
        serverLoginURL = "http://" + _serverHostname + "/sigcap/login.php";
    }
    
    /**
     * Saves userId and password into SharedPreferences
     */
    public void applyCredentialPreferences(String userId, String password) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_baseContext);
		Editor editor = prefs.edit();
		editor.putString("userIdPref", userId);
		editor.putString("passwordPref", password);
		editor.commit();
    }
    
    /**
     * Gets credentials stored in preferences
     */
    public String[] getCredentialPreferences() {
    	String[] credentials = new String[2];
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_baseContext);
        credentials[0] = prefs.getString("userIdPref", "");
        credentials[1] = prefs.getString("passwordPref", "");
        
        return credentials;
    }
    
    /**
     * Saves work offline state into SharedPreferences
     */
    public void applyWorkOfflinePreference(boolean workOffline) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_baseContext);
		Editor editor = prefs.edit();
		editor.putBoolean("workOfflinePref", workOffline);
		editor.commit();
    }

    public boolean isLoggedIn() {
    	return _isLoggedIn;
    }
    
    public void setIsLoggedIn(boolean isLoggedIn) {
    	_isLoggedIn = isLoggedIn;
    }
    
    /**
     * Registers handler from parent Activity utilizing this class so a message can be sent each time OnReceive() is triggered
     * @param targetHandler Target handler to be added to handler hashmap object
     * @param what Code to be used when sending a message to the target handler
     */
    public void registerHandler(Handler targetHandler) {
        _handlers.put(targetHandler, 0);
    }

    /**
     * Unregisters handler from parent Activity 
     * @param targetHandler Target handler to be added to handler hashmap object
     */
    public void unregisterHandler(Handler targetHandler) {
        _handlers.remove(targetHandler);
    }
    



	 public void grabFirstCookie() {
	        DefaultHttpClient httpclient = HttpClientFactory.getThreadSafeClient();
	        HttpPost httppost = new HttpPost(serverLoginURL);

	        @SuppressWarnings("unused")
			String result = "";       

	        try {
	        	// Add your data
	            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
	            nameValuePairs.add(new BasicNameValuePair("userid", ""));
		        nameValuePairs.add(new BasicNameValuePair("password", ""));
	            nameValuePairs.add(new BasicNameValuePair("q", "1"));

	            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	        	
	            // Execute HTTP Post Request
	            HttpResponse response = httpclient.execute(httppost);

	            if (response == null) {
	            	result = "Error";
	            } else {
	                try {
	                    InputStream in = response.getEntity().getContent();
	                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	                    StringBuilder str = new StringBuilder();
	                    String line = null;
	                    while ((line = reader.readLine()) != null) {
	                        str.append(line + "\n");
	                    }
	                    in.close();
	                    result = str.toString();
	                    
	                    setCookies(httpclient.getCookieStore().getCookies());
	                } catch(Exception ex) {
	                    result = "Error";
	                }
	            }         
	        } catch (ClientProtocolException e) {
	        	result = "Error|Network error occured during upload";
	        } catch (IOException e) {
	        	result = "Error|Network timeout occured, please try again later";
	        }
	    } 
	 
   
   /**
    * AsyncTask wrapper that attempts to login to the server in a background thread
    */
   private class LoginTask extends AsyncTask<Void, Void, String> {
   	@Override
	protected void onPreExecute() {
   		if(!progressDialogLoggingIn.isShowing()) {
   			if(_isSilentLogin == false) {
   				progressDialogLoggingIn = ProgressDialog.show(_baseContext, "", "Logging in...", true, false);
   			}
   		}
   	}
   	
		@Override
		protected String doInBackground(Void... unused) {			
			// Create a new HttpClient and Post fields, return a pipe separated string 
	        httpclient = HttpClientFactory.getThreadSafeClient();
	        
	        
	        
	        CookieStore localCookieStore = getCookies();
	        
	        if(localCookieStore.getCookies().size() == 0) {
	        	grabFirstCookie();
	        }
	    
	        if(localCookieStore.getCookies().size() > 0) {
	        	httpclient.setCookieStore(localCookieStore);
	        	
	        	// Create local HTTP context
		        localContext = new BasicHttpContext(); 
		        // Bind custom cookie store to the local context
		        localContext.setAttribute(ClientContext.COOKIE_STORE, httpclient.getCookieStore());
		        
		        _hasCookie = true;
	        } 
	        
	        HttpPost httppost = new HttpPost(serverLoginURL);
	        httppost.setParams(httpclient.getParams());
	        
	        String result = "";       

	        try {
	            // Add your data
	            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
	            
	            if(_isSuppliedCredentials) {
	            	nameValuePairs.add(new BasicNameValuePair("userid", _userIdSupplied));
		            nameValuePairs.add(new BasicNameValuePair("password", _passwordSupplied));
	            } else {
	            	nameValuePairs.add(new BasicNameValuePair("userid", _userId));
		            nameValuePairs.add(new BasicNameValuePair("password", _password));
	            }
	            
	            nameValuePairs.add(new BasicNameValuePair("q", "1"));

	            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

	            // Execute HTTP Post Request utilizing the localContext and cookieStore initialized in the constructor
	            if(_hasCookie) {
	            	response = httpclient.execute(httppost, localContext);
	            } else {
	            	response = httpclient.execute(httppost);
	            }
	            
	            if (response == null) {
	            	result = "Error";
	            } else {
	                try {
	                    InputStream in = response.getEntity().getContent();
	                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	                    StringBuilder str = new StringBuilder();
	                    String line = null;
	                    while ((line = reader.readLine()) != null) {
	                        str.append(line + "\n");
	                    }
	                    in.close();
	                    result = str.toString();
	                } catch (SocketTimeoutException e ) {
	    	        	result = "Error|Network timeout occured, please try again later";
	                } catch(Exception ex) {
	                    result = "Error|Generic Exception";
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
		protected void onPostExecute(String result) {
			String[] results = result.split("\\|");
			
			progressDialogLoggingIn.dismiss();
			
			if(isDebug) {
				Log.d(TAG, results[0]); 
			}
			
		    if (results[0].trim().replaceAll("(\\r|\\n)", "").equals("N") || results[0].trim().contains("Error")) {
		    	// N, error logging in
		    	_isLoggedIn = false;
		    	
		    	applyWorkOfflinePreference(true);
		    	removeCookies();
		    	
		    	if(results[0].trim().contains("Error")) {
		    		sendMessageToHandlers(TIMEOUT_ERROR_MESSAGE);
		    	}
		    } else {
		    	// Y, logged in
		    	//Fix bug here where errors are attributed to being logged in
		    	_isLoggedIn = true;
		    	
	            // Set whatever cookies were saved in the execution of the login to non-volatile memory
	            setCookies(httpclient.getCookieStore().getCookies());
		    	
		    	applyWorkOfflinePreference(false);
	        }
		    
		    if(!results[0].trim().contains("Error")) {
		    	sendMessageToHandlers(_isLoggedIn ? LOGIN_SUCCESS_MESSAGE : LOGIN_ERROR_MESSAGE);
		    }
		}
		
		@Override
		protected void onCancelled() {
			progressDialogLoggingIn = null;
		}
	}
   
   /**
    * AsyncTask wrapper that attempts to logout from the server in a background thread
    */
   private class LogoutTask extends AsyncTask<Void, Void, String> {
   	@Override
	protected void onPreExecute() {
   		if(!progressDialogLoggingIn.isShowing()) {
   			progressDialogLoggingIn = ProgressDialog.show(_baseContext, "", "Logging out...", true, false);
   		}
   	}
   	
		@Override
		protected String doInBackground(Void... unused) {			
			// Create a new HttpClient and Post fields, return a pipe separated string 
	        httpclient = HttpClientFactory.getThreadSafeClient();
	        
	        CookieStore localCookieStore = getCookies();
	        if(localCookieStore.getCookies().size() > 0) {
	        	httpclient.setCookieStore(localCookieStore);
	        	
	        	// Create local HTTP context
		        localContext = new BasicHttpContext(); 
		        // Bind custom cookie store to the local context
		        localContext.setAttribute(ClientContext.COOKIE_STORE, httpclient.getCookieStore());
		        
		        _hasCookie = true;
	        } 
	        
	        HttpGet httpGet = new HttpGet(serverLoginURL + "?l=1&q=1");
	        httpGet.setParams(httpclient.getParams());
	        
	        String result = "";       

	        try {
	            // Execute HTTP Post Request utilizing the localContext and cookieStore initialized in the constructor
	            if(_hasCookie) {
	            	response = httpclient.execute(httpGet, localContext);
	            } else {
	            	response = httpclient.execute(httpGet);
	            }
	            
	            if (response == null) {
	            	result = "Error";
	            } else {
	                try {
	                    InputStream in = response.getEntity().getContent();
	                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	                    StringBuilder str = new StringBuilder();
	                    String line = null;
	                    while ((line = reader.readLine()) != null) {
	                        str.append(line + "\n");
	                    }
	                    in.close();
	                    result = str.toString();
	                } catch(Exception ex) {
	                    result = "Error|Generic Exception";
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
		protected void onPostExecute(String result) {
			String[] results = result.split("\\|");
			
			progressDialogLoggingIn.dismiss();
			
			if(isDebug) {
				Log.d(TAG, results[0]); 
			}
			
		    if (results[0].trim().replaceAll("(\\r|\\n)", "").equals("N") || results[0].trim().contains("Error")) {
		    	// N, error logging out
		    	_isLoggedIn = false;
		    	removeCookies();
		    	
		    	if(results[0].trim().contains("Error")) {
		    		sendMessageToHandlers(TIMEOUT_ERROR_MESSAGE);
		    	}
		    } else {
		    	// Y, logged in
		    	_isLoggedIn = false;
		    	
		    	removeCookies();
		    	
		    	applyWorkOfflinePreference(false);
	        }
		    
		    if(!results[0].trim().contains("Error")) {
		    	sendMessageToHandlers(LOGIN_LOGOUT_MESSAGE);
		    }
		}
		
		@Override
		protected void onCancelled() {
			progressDialogLoggingIn = null;
		}
	}
    
    /**
     * Creates login alert dialog window stating whether or not login was successful
     */
    public void generateLoginAlertDialog(int message) {
    	if(!alertDialog.isShowing()) {
    		if(_isSilentLogin == false || (_isSilentLogin && message == LOGIN_NO_USERID_OR_PASSWORD) ) {
		    	alertDialog = new AlertDialog.Builder(_baseContext).create();
		    	//alertDialog.setTitle("Login");
		    	
		    	if(message == LOGIN_LOGOUT_MESSAGE) {
		    		alertDialog.setMessage("Successfully logged out");
		    	}
		    	else if(message == LOGIN_ERROR_MESSAGE){
		    		alertDialog.setMessage("Invalid User ID and/or Password");
		    		
		    		/*alertDialog.setButton2("Go offline", new DialogInterface.OnClickListener() {  
			    	      public void onClick(DialogInterface dialog, int which) {  
			    	    	  applyWorkOfflinePreference(true);
			    	          return;
			    	      } });*/
		    	}
		    	else if(message == LOGIN_SUCCESS_MESSAGE) {
		    		alertDialog.setMessage("Successfully logged in");
		    		
		    	}
		    	else if(message == LOGIN_NO_USERID_OR_PASSWORD) {
    		    	alertDialog.setMessage("Please enter a User ID and Password in Settings");
		    	}
		    	else if(message == LOGIN_NO_USERID_OR_PASSWORD_LOGIN_FORM) {
    		    	alertDialog.setMessage("Please enter a User ID and Password");
		    	} 
		    	else if(message == LOGIN_ERROR_NO_CONNECTIVITY) {
		    		alertDialog.setMessage("Connection not detected, please try again later or press Work Offline");
		    	}
		    	else if(message == TIMEOUT_ERROR_MESSAGE) {
		    		alertDialog.setMessage("Network timeout occured, please try again later");
		    	}

		    	alertDialog.setButton("Ok", new DialogInterface.OnClickListener() {  
		    	      @Override
					public void onClick(DialogInterface dialog, int which) {  
		    	          return;
		    	      } });
		    	
		    	alertDialog.show();
    		}
    	}
    }
    
    /**
     * Sends message to all registered handlers
     * @param message Sends 0 for error logging in or 1 for logged in
     */
    private void sendMessageToHandlers(int message) {
    	//Notify any handlers added by the Activity using this class that a connectivity event has occured
		Iterator<Handler> handlerIterator = _handlers.keySet().iterator();
		while(handlerIterator.hasNext()) {
			Handler targetHandler = handlerIterator.next();
			Message messageToSend = Message.obtain(targetHandler, message);
			targetHandler.sendMessage(messageToSend);
		}
    }
    
    /**
     * Checks to see if the device is online
     * @param context 
     * @return true if connection is established, false if there is no connection
     */
	public boolean isOnline(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		
		if (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected()) {
			return true;
		} 
		else {
			return false; 
		}
	}
	
	public static class HttpClientFactory {
		private static DefaultHttpClient client;

	    public static DefaultHttpClient getThreadSafeClient() {
	  
	        if (client != null) {
	        	return client;
	        }

	        int timeoutConnection = TIMEOUT_VALUE; // 3 seconds till give up on establishing connection
		    int timeoutSocket = TIMEOUT_VALUE; // 5 seconds till waiting for data time out
		    
	        client = new DefaultHttpClient();
	        
	        ClientConnectionManager mgr = client.getConnectionManager();
	        
	        HttpParams params = client.getParams();
	        HttpConnectionParams.setConnectionTimeout(params, timeoutConnection);
		    HttpConnectionParams.setSoTimeout(params, timeoutSocket);
		    
		    params.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
	        client = new DefaultHttpClient(new ThreadSafeClientConnManager(params,  mgr.getSchemeRegistry()), params);
	        client.setParams(params);

	        return client;
	    } 
	}
}