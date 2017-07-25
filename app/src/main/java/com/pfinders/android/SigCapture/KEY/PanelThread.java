package com.pfinders.android.SigCapture.KEY;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

class PanelThread extends Thread {
    private SurfaceHolder _surfaceHolder;
    private Panel _panel;
    private boolean _run = false;
 
    public PanelThread(SurfaceHolder surfaceHolder, Panel panel) {
        _surfaceHolder = surfaceHolder;
        _panel = panel; 
    }
 
    public void setRunning(boolean run) {
        _run = run;
    }
    
    public SurfaceHolder getSurfaceHolder() {
        return _surfaceHolder;
    }
 
    @Override
    public void run() {
        Canvas c;
        while (_run) {
            c = null;
            try {
                c = _surfaceHolder.lockCanvas(null); // returns a canvas for editing 
                synchronized (_surfaceHolder) {
                    _panel.onDraw(c); // draw all the lines
                	 
                }
            } catch(Exception Ex) {
            	// do nothing
            }finally {
                // do this in a finally so that if an exception is thrown
                // during the above, we don't leave the Surface in an
                // inconsistent state
                if (c != null) {
                    _surfaceHolder.unlockCanvasAndPost(c);
                }
            }
        }
    }
}
