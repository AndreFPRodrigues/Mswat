package mswat.controllers.touch;

import java.util.ArrayList;

/**
 * Process touch event received, identify type of touch
 * 
 * @author Andre Rodrigues
 * 
 */
public class TouchPatternRecognizer {
	
	// Touch types
	final int TOUCHED = 0;
	final int SLIDE = 1;
	final int LONGPRESS = 2;

	private ArrayList<TouchEvent> touches;
	
	/**
	 * Identify type of touch 
	 * @return
	 */
	public int identifyTouch() {
		return 0;
	}
	
	/**
	 * Register in array all touch positions until finger release
	 */
	public boolean store(int type, int code, int value){
		return false;
	}
}
