package mswat.touch;

import java.util.ArrayList;

import mswat.core.CoreController;

import android.content.Context;
import android.content.Intent;
import android.text.method.Touch;
import android.util.Log;

/**
 * Process touch event received, identify type of touch
 * 
 * Feed all the touch positions via store(), when it returns true a type of
 * touch has finished. Use identifyTouch() to get the type of touch iden
 * 
 * @author Andre Rodrigues
 * 
 */
public class TPRTab2 extends TouchRecognizer {

	private final String LT = "Tab2";
	private int slot = 0;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(LT, "received TPR");

		// Triggered when the service starts
		if (intent.getAction().equals("mswat_tpr")
				&& intent.getExtras().get("touchRecog").equals("tab2")) {
			Log.d(LT, "register TPR");
			CoreController.registerActivateTouch(this);

		}

	}

	/**
	 * Identify type of touch (slide/touch/long press)
	 * 
	 * @return
	 */

	private int identifyTouch() {
		double distance = 0;
		if (touches.size() < 5) {
			touches.clear();
			if ((longTouchTime - lastTouch) < -LONGPRESS_THRESHOLD)
				return LONGPRESS;
			return TOUCHED;
		} else {
			for (int i = 1; i < touches.size(); i++) {
				distance = Math.sqrt((Math.pow((touches.get(i).getY() - touches
						.get(i - 1).getY()), 2) + Math.pow((touches.get(i)
						.getX() - touches.get(i - 1).getX()), 2)));
				if (distance > 50) {
					slideXorigin = touches.get(0).getX();
					slideYorigin = touches.get(0).getY();
					touches.clear();
					return SLIDE;
				}
			}
			touches.clear();
			return TOUCHED;
		}
	}

	/**
	 * Register in array all touch positions until finger release Detects single
	 * touch
	 * 
	 * @return (-1) -> no touch identified yet (0) -> touch identified (1) ->
	 *         slide identified (2) -> LongPress identified
	 */
	@Override
	public int identifyOnRelease(int type, int code, int value, int timestamp) {
		// Log.d(LT, "t:" + type + " c:" + code + " v:" + value + " time:"
		// + timestamp);
		if (code == TRACKING_ID) {
			identifier = value;
		} else if (code == PRESSURE)
			pressure = value;
		else if (code == TOUCH_MAJOR)
			touchMajor = value;
		else if (code == TOUCH_MINOR)
			touchMinor = value;
		if (code == POSITION_X)
			lastX = value;
		else if (code == POSITION_Y)
			lastY = value;
		else if (code == SYN_REPORT && value == 0
				&& lastEventCode != TRACKING_ID) {
			TouchEvent p = new TouchEvent(lastX, lastY, timestamp, pressure,
					touchMajor, touchMinor, identifier);
			longTouchTime = timestamp;
			touches.add(p);
		}
		if (code == TRACKING_ID && value == -1 && (lastEventCode == SYN_REPORT)
				&& touches.size() > 0) {
			lastEventCode = -1;
			// prevents double tap
			if ((lastTouch - timestamp) < -doubleTapThreshold) {

				lastTouch = timestamp;
				return identifyTouch();
			} else
				return -1;
		}
		lastEventCode = code;
		return -1;
	}

	/**
	 * Register in array all touch positions until finger release
	 * 
	 * @return (-1) -> no touch identified yet (0) -> down identified (1) ->
	 *         move identified (2) -> up identified
	 */
	@Override
	public int identifyOnChange(int type, int code, int value, int timestamp) {
		switch (code) {
		case POSITION_X:
			lastX = value;
			break;
		case POSITION_Y:

			lastY = value;
			break;
		case PRESSURE:
			pressure = value;
			break;
		case TOUCH_MAJOR:
			touchMajor = value;
			break;
		case TOUCH_MINOR:
			touchMinor = value;
			break;
		case TRACKING_ID:

			identifier = value;
			break;
		case MT_SLOT:

			if (value > 0)
				numberTouches++;
			slot = value;
			break;
		case SYN_REPORT:

			TouchEvent p;
			if (identifier > 0) {
				checkResetTouches();
				if(slot>id_touches.size()-1){
					id_touches.add(identifier);
				}else
					id_touches.set(slot, identifier);
				p = new TouchEvent(lastX, lastY, timestamp, pressure,
						touchMajor, touchMinor, identifier);
				touches.add(p);
				time= timestamp;
				identifier = -identifier;
				numberTouches++;
				return DOWN;    
			} else {
				if (identifier != -1) {

					p = new TouchEvent(lastX, lastY, timestamp, pressure,
							touchMajor, touchMinor, -identifier);
					touches.add(p);
					if (checkIfMoved(p)) {
						if (slot >= 0 && id_touches.size() > slot) {
							identifier = -id_touches.get(slot);
						}
						return MOVE;
					}
				} else {
					if (slot >= 0 && id_touches.size() > slot) {
						identifier = id_touches.get(slot);
						id_touches.set(slot, -1);
					}

					if (numberTouches == 1)
						touches.clear();
					else
						numberTouches--;

					return UP;
				}
			}

		}

		// Log.d(LT, "t:" + type + " c:" + code + " v:" + value);
		return -1;
	} 

	private void checkResetTouches() {
		boolean reset = true;
		for (int i = 0; i < id_touches.size(); i++) {
			if (id_touches.get(i) != -1) {
				reset = false;
				break;
			}
		}
		if (reset)
			id_touches.clear();
	}

	private void clearTouchesFromId(int idFingerUp2) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getIdentifier() {
		if (identifier < 0)
			return -identifier;
		return identifier;
	}
}
