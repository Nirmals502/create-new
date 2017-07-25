package com.pfinders.android.SigCapture.KEY;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class Panel extends SurfaceView implements SurfaceHolder.Callback {
	    private PanelThread _thread;
	    private Path path;
	    private Paint mPaint;
	    private ArrayList<Path> _graphics = new ArrayList<Path>(); 
	    private Bitmap _bitmap;
	    private boolean insideActionDown = false;
	    private float lastDownX;
	    private float lastDownY;
	    
	    public Panel(Context context) {
	        super(context);
	        mPaint = new Paint();
	        mPaint.setAntiAlias(true);
	        //mPaint.setDither(true);  
	        mPaint.setColor(Color.BLACK);
	        mPaint.setStyle(Paint.Style.STROKE);  
	        mPaint.setStrokeJoin(Paint.Join.ROUND);  
	        mPaint.setStrokeCap(Paint.Cap.ROUND);  
	        mPaint.setStrokeWidth(5f);  
	        getHolder().addCallback(this);
	        _thread = new PanelThread(getHolder(), this);
	        setFocusable(true);
	    }
	 
	    public void saveBitmap(FileOutputStream fos) {
	    	synchronized (_thread.getSurfaceHolder()) {
	    	    if (_bitmap != null) {
	    	        _bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
	    	        try {
	    	        	fos.flush();
						fos.close();
						fos = null;
						System.gc();
					} catch (IOException e) {
						// TODO Auto-generated catch block
					}
	    	    }
	    	}
	    }
	    
	    @Override  
	    public void onDraw(Canvas canvas) {  
	    	//draw lines on temp bitmap then assign to real canvas
	    	if (_bitmap == null) {
	    	    _bitmap = Bitmap.createBitmap(canvas.getWidth(),canvas.getHeight(),Bitmap.Config.ARGB_8888);
	    	}
	    	Canvas tempCanvas = new Canvas(_bitmap);
    		tempCanvas.drawColor(Color.WHITE); // clear it
	        for (Path path : _graphics) {    
	            tempCanvas.drawPath(path, mPaint);  
	        }
	        canvas.drawBitmap(_bitmap, canvas.getMatrix(), mPaint);
	    }      
	    
	    @Override
	    public boolean onTouchEvent(MotionEvent event) {
	    	
	    	synchronized (_thread.getSurfaceHolder()) {  
	    		int historySize = event.getHistorySize(); // events can stack up
			    if (event.getAction() == MotionEvent.ACTION_DOWN) {
			    	if (insideActionDown) {
			    		// New down with no up. This is strange but possible.
		    	        path.lineTo(event.getX(), event.getY());  
		    	        _graphics.add(path);
			    	}
			    	insideActionDown = true;
	    	        path = new Path();  
	    	        path.moveTo(event.getX(), event.getY());  
	    	        path.lineTo(event.getX(), event.getY());
	    	        lastDownX = event.getX();
	    	        lastDownY = event.getY();
	    	    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
	    	    	for (int i = 0; i < historySize; i++) {
	    	    		float historyX = event.getHistoricalX(i);
	    	    		float historyY = event.getHistoricalY(i);
	    	    		path.lineTo(historyX, historyY);
	    	    		_graphics.add(path);
		    	        path = new Path();
		    	        path.moveTo(historyX, historyY);
		    	        path.lineTo(historyX, historyY);
	    	    	}
	    	        path.lineTo(event.getX(), event.getY());
	    	        _graphics.add(path);
	    	        path = new Path();
	    	        path.moveTo(event.getX(), event.getY());
	    	        path.lineTo(event.getX(), event.getY());
	    	    } else if (event.getAction() == MotionEvent.ACTION_UP) {
	    	    	for (int i = 0; i < historySize; i++) {
	    	    		float historyX = event.getHistoricalX(i);
	    	    		float historyY = event.getHistoricalY(i);
	    	    		path.lineTo(historyX, historyY);
	    	    	}
	    	    	if (lastDownX == event.getX() && (lastDownY == event.getY())) {
	    	    		// draw a dot instead of a line
	    	    		path.lineTo(event.getX() + 1, event.getY() + 1);
	    	    	} else {
	    	            path.lineTo(event.getX(), event.getY());
	    	    	}
	    	        _graphics.add(path);
	    	        insideActionDown = false;
	    	    }  
	    	    return true;  
	    	}  
	    }
	    
	    @Override
	    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	        // TODO Auto-generated method stub
	    }
	 
	    @Override
	    public void surfaceCreated(SurfaceHolder holder) {
	        _thread.setRunning(true);
	        _thread.start();
	    }
	 
	    @Override
	    public void surfaceDestroyed(SurfaceHolder holder) {
	        boolean retry = true;
	        _thread.setRunning(false);
	        while (retry) {
	            try {
	                _thread.join();
	                retry = false;
	            } catch (InterruptedException e) {
	                // we will try it again and again...
	            }
	        }
	    }
	    
	    public void clearPanel() {
	    	synchronized (_thread.getSurfaceHolder()) {
	    	    _graphics.clear();
	    	}
	    }
	    
	    public void onResumePanel() {
	    	_thread = new PanelThread(getHolder(), this);
	    	_thread.setRunning(true);
	    	_thread.run();
	    }
	    	   
	    public void onPausePanel() {
	    	boolean retry = true;
	    	_thread.setRunning(false);
	    	while (retry) {
	    	    try {
	    	        _thread.join();
	    	        retry = false;
	    	    } catch (InterruptedException e) {
	    	        //e.printStackTrace();
	    	    }
	       }
	    }
	}


