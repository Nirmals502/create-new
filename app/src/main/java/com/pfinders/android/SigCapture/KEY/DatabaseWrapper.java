package com.pfinders.android.SigCapture.KEY;

import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;

public class DatabaseWrapper extends SQLiteOpenHelper {
	public static final String DB_NAME = "sigcap.db";
	public static final int DB_VERSION = 1;
	
	// Database tables
	public static final String ORDER_RECORDS_TABLE = "order_records";
	public static final String XREF_TABLE = "xref";
	
	// Order records table column names
    private static final String ORDER_NUMBER = "order_number";
    private static final String BARCODE_NUMBER = "barcode_number";
    private static final String CUSTOMER_NUMBER = "customer_number";
    private static final String CUSTOMER_NAME = "customer_name";
    private static final String ORDER_DATE = "order_date";
    
	// Xref records table column names
    private static final String XREF_BARCODE_NUMBER = "barcode_number";
    private static final String XREF_ORDER_NUMBER = "order_number";
    
    // Column positions 
    private static final int OrderNumPosition = 0;
    private static final int BarcodeNumPosition = 1;
    private static final int CustomerNumPosition = 2;
    private static final int CustomerNamePosition = 3;
    private static final int OrderDatePosition = 4;
    
    private ProgressDialog progressLoadingRecords;
    private Context _baseContext;
    private SQLiteDatabase _database;
    private String[] _recordsFromWeb;
    private int _totalRecordCount = 0;
    

	public DatabaseWrapper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		_baseContext = context; 
		_database = this.getWritableDatabase();
	}

	/**
	 * Grabs order information from scanned number whether it is a barcode number or order number
	 */
	public String queryOrderInformation(String scannedOrderNumber) {
		String actualOrderNumber = getXref(scannedOrderNumber);

		if(actualOrderNumber != null) {
			String orderInformation = getOrder(actualOrderNumber);
			
			if(orderInformation != null) {
				 return orderInformation;
			}
		} else { 
			String orderInformation = getOrder(scannedOrderNumber);
			
			if(orderInformation != null) {
				 return orderInformation;
			}
		}
		
		return "Error|Order not found";
	}
	
	/**
	 * Executes LoadRecordsTask on seperate thread 
	 */
	public void loadRecordsFromWeb(String records) {
		_recordsFromWeb = records.split("\n");
		_totalRecordCount = _recordsFromWeb.length;
		
		_database = this.getWritableDatabase();
		LoadRecordsTask loadRecordsTask = new LoadRecordsTask();
		loadRecordsTask.execute(records);
	}
	

	/**
	 * Adds order to ORDER_RECORDS table in database
	 */
	private void addOrder(String orderNumber, String barcodeNumber, String customNumber, String customerName, String orderDate) {
		_database = this.getWritableDatabase();
	 
	    ContentValues values = new ContentValues();
	    
	    values.put(ORDER_NUMBER, orderNumber); 
	    
	    if(barcodeNumber.length()>0) {
	    	values.put(BARCODE_NUMBER, barcodeNumber); 
	    }
	    
	    values.put(CUSTOMER_NUMBER, customNumber); 
	    values.put(CUSTOMER_NAME, customerName); 
	    values.put(ORDER_DATE, orderDate); 
	 
	    // Inserting Row
	    _database.insertOrThrow(ORDER_RECORDS_TABLE, null, values);
	}
	
	/**
	 * Adds Xref information to XREF table in database
	 */
	private void addXref(String barcodeNumber, String orderNumber) {
		_database = this.getWritableDatabase();
		ContentValues values = new ContentValues();
	
		values.put(BARCODE_NUMBER, barcodeNumber); 
		values.put(ORDER_NUMBER, orderNumber); 
			    
		// Inserting Row
		_database.insertOrThrow(XREF_TABLE, null, values);
	}
	
	/**
	 * Gets order number associated with a barcode if it exists
	 */
	public String getXref(String barcodeId) {
		SQLiteDatabase db = this.getReadableDatabase();

		String xrefSQLSelect = "SELECT * FROM " + XREF_TABLE + " WHERE " + BARCODE_NUMBER + " = '" + barcodeId + "'";
	    Cursor cursor = db.rawQuery(xrefSQLSelect, null);
	    
	    if (cursor != null) {
	        if(cursor.moveToFirst()) {
	        	return cursor.getString(1);
	        } else {
	        	return null;
	        }
	    }
	
	    return null;
	}
	
	/**
	 * Gets order information associates with an order number
	 */
	public String getOrder(String orderId) {
	    SQLiteDatabase db = this.getReadableDatabase();
	    
	    String orderSQLSelect = "SELECT * FROM " + ORDER_RECORDS_TABLE + " WHERE " + ORDER_NUMBER + " = '" + orderId + "'";
	    Cursor cursor = db.rawQuery(orderSQLSelect, null);
	    
	    if (cursor != null) { 
	    	if(cursor.moveToFirst()) {
	    		return cursor.getString(0) + " " + cursor.getString(3);
	    	} else {
	    		return null;
	    	}
	    }
	
	    return null;
	}
	
	/**
	 * Gets all records in xref table
	 */
	public List<String> getAllXrefs() {
	    List<String> xrefList = new ArrayList<String>();
	    // Select All Query
	    String selectQuery = "SELECT  * FROM " + XREF_TABLE;
	 
	    SQLiteDatabase db = this.getWritableDatabase();
	    Cursor cursor = db.rawQuery(selectQuery, null);
	 
	    // looping through all rows and adding to list
	    if (cursor.moveToFirst()) {
	        do {
	            xrefList.add("Barcode Number: " + cursor.getString(0) + " Order Number: " + cursor.getString(1));
	        } while (cursor.moveToNext());
	    }
	 
	    // return contact list
	    return xrefList;
	}
	
	/**
	 * Gets all records in order_record table
	 */
	public List<String> getAllOrders() {
	    List<String> orderList = new ArrayList<String>();
	    // Select All Query
	    String selectQuery = "SELECT  * FROM " + ORDER_RECORDS_TABLE;
	 
	    SQLiteDatabase db = this.getWritableDatabase();
	    Cursor cursor = db.rawQuery(selectQuery, null);
	 
	    // looping through all rows and adding to list
	    if (cursor.moveToFirst()) {
	        do {
	        	orderList.add("Order Number:[" + cursor.getString(0) + "] Barcode Number:[" + cursor.getString(1) + "] Customer Number:[" + cursor.getString(2) +
	        			"] Customer Name:[" + cursor.getString(3) + "] Order Date:[" + cursor.getString(4) + "]");
	        } while (cursor.moveToNext());
	    }
	 
	    // return the list
	    return orderList;
	}
	
	/**
	 * Gets number of records in xref table
	 */
	public int getXrefCount() {
        String countQuery = "SELECT  * FROM " + XREF_TABLE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int xrefCount = cursor.getCount();
        cursor.close();

        return xrefCount;
    }
	
	/**
	 * Gets number of records in order_records table
	 */
	public int getOrderCount() {
        String countQuery = "SELECT  * FROM " + ORDER_RECORDS_TABLE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int orderCount = cursor.getCount();
        cursor.close();

        return orderCount;
    }

	/**
	 * Clears both tables in preparation for new data
	 */
	public void clearTables() {
	    _database.delete(XREF_TABLE, null, null);
	    _database.delete(ORDER_RECORDS_TABLE, null, null); 
	}

	@Override
	public void onCreate(SQLiteDatabase db) { 
		 String CREATE_ORDER_RECORDS_TABLE = "CREATE TABLE " + ORDER_RECORDS_TABLE + "("
	                +  ORDER_NUMBER + " TEXT PRIMARY KEY," + BARCODE_NUMBER + " TEXT,"
	                + CUSTOMER_NUMBER + " TEXT," + CUSTOMER_NAME + " TEXT," + ORDER_DATE + " DATE" + ")";
		 
	     db.execSQL(CREATE_ORDER_RECORDS_TABLE);
	        
	     String CREATE_XREF_TABLE = "CREATE TABLE " + XREF_TABLE + "("
	                +  XREF_BARCODE_NUMBER + " TEXT PRIMARY KEY," + XREF_ORDER_NUMBER + " TEXT" + ")";
	     db.execSQL(CREATE_XREF_TABLE);       
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + ORDER_RECORDS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + XREF_TABLE);
 
        // Create tables again
        onCreate(db);
	}
	
	/**
    * AsyncTask wrapper that loads records into the database via background thread
    */
    private class LoadRecordsTask extends AsyncTask<String, Integer, Void> {
    	int recordCounter = 0;
    	
    	@Override
		protected void onPreExecute() {
    		progressLoadingRecords = new ProgressDialog(_baseContext);
    		progressLoadingRecords.setMessage("Loading records..."); 
    		progressLoadingRecords.setMax(_totalRecordCount);
    		progressLoadingRecords.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    	    progressLoadingRecords.setCancelable(false);
    	    progressLoadingRecords.show();
    	}
    	
		@Override
		protected Void doInBackground(String... records) {		
			clearTables();

			_database.beginTransaction();
			for(String innerRow : _recordsFromWeb) {
				String[] innerRowPieces = innerRow.split("\\|");
				
			    try {
					if(innerRowPieces[BarcodeNumPosition].length() > 0) {
						addXref(innerRowPieces[BarcodeNumPosition], innerRowPieces[OrderNumPosition]);
					}
					addOrder(innerRowPieces[OrderNumPosition], innerRowPieces[BarcodeNumPosition], innerRowPieces[CustomerNumPosition], innerRowPieces[CustomerNamePosition], innerRowPieces[OrderDatePosition]);
					
					publishProgress(recordCounter);
					recordCounter++;
			    } catch(SQLiteException ex) {
			    	//Log.d("TestBarcode", "Duplicate record encountered");
			    } catch(Exception ex) {
			    	// General exception
			    }
			}
			_database.setTransactionSuccessful();
			_database.endTransaction();
			_database.close();
			_recordsFromWeb = null;
			
			return null;
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) 
		{
			progressLoadingRecords.setProgress(values[0]);
		}

		@Override
		protected void onPostExecute(Void unused) {
			progressLoadingRecords.dismiss();
		}
	}
}
