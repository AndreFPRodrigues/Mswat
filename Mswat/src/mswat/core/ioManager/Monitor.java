package mswat.core.ioManager;

import java.util.ArrayList;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import mswat.core.CoreController;
import mswat.core.activityManager.HierarchicalService;
import mswat.core.ioManager.Events.InputDevice;
import mswat.core.logger.Logger;
import mswat.touch.TouchPatternRecognizer;

public class Monitor {

	// debugging tag
	private final String LT = "Monitor";

	// List of internal devices (touch, accelerometer)
	ArrayList<InputDevice> dev;

	// Core controller
	CoreController cc;

	Logger logger;

	private int touchIndex;

	private TouchPatternRecognizer tpr = new TouchPatternRecognizer();

	boolean monitoring[];
	boolean logging;

	/**
	 * Initialises list of devices 
	 * Initialises Logger if logging is true
	 * 
	 * @param logging
	 */
	public Monitor(boolean logging, HierarchicalService hs) {
		Events ev = new Events();
		dev = ev.Init();
		monitoring = new boolean[dev.size()];
		this.logging = logging;
		autoSetTouch();
		if (logging) {
			logger = new Logger(hs);
			monitorTouch();
		}
		
	}
	
	/**
	 * Log keystroke if logger is enable
	 * @param keypressed
	 */
	public void registerKeystroke(String keypressed
			) {
		if(logging)
			logger.registerTouch("Previous Keystroke: " +keypressed);

	}

	/**
	 * Blocks or unblock the device in index position
	 * 
	 * @param b
	 *            - true to block, false to unblock
	 * @param index
	 *            - device index
	 */
	public void setBlock(int index, boolean b) {
		dev.get(index).takeOver(b);
	}

	/**
	 * Inject event into the device in the position index
	 * 
	 * @param index
	 *            - device index
	 * @param type
	 * @param code
	 * @param value
	 * @return if successful
	 */
	public void inject(int index, int type, int code, int value) {
		dev.get(index).send(index, type, code, value);
	}

	/**
	 * Starts or stops monitoring the device in position index
	 * 
	 * @param index
	 *            - device index
	 * @param b
	 *            - true to monitor, false to stop monitoring
	 */
	public void monitorDevice(final int index, final boolean state) {
		if (state != monitoring[index]) {
			monitoring[index] = state;
			if (monitoring[index]) {
				Thread b = new Thread(new Runnable() {

					public void run() {
						Looper.prepare();
						InputDevice idev = dev.get(index);
						while (monitoring[index]) {
							if (idev.getOpen() && idev.getPollingEvent() == 0) {
								if (index == touchIndex) {
									int type;
									if ((type = tpr.store(
											idev.getSuccessfulPollingType(),
											idev.getSuccessfulPollingCode(),
											idev.getSuccessfulPollingValue(),
											idev.getTimeStamp())) != -1) {
										String message = touchMessage(type);

										if (logging) {
											logger.registerTouch(message);
										}

										CoreController.touchMessage(type, message);
									}

								} else {
									CoreController.monitorMessages(
											idev.getName(),
											idev.getSuccessfulPollingType(),
											idev.getSuccessfulPollingCode(),
											idev.getSuccessfulPollingValue(),
											idev.getTimeStamp());
								}
							}
						}
					}
				});
				b.start();
			}
		}

	}

	/**
	 * Returns a String array of the internal devices
	 * 
	 * @return String [] - internal devices names
	 */
	public String[] getDevices() {
		String[] s = new String[dev.size()];
		for (int i = 0; i < dev.size(); i++) {

			s[i] = dev.get(i).getName();
			Log.d(LT, "Devices: " + s[i]);
		}
		return s;
	}

	/**
	 * Stop all monitoring
	 */
	public void stop() {
		for (int i = 0; i < dev.size(); i++) {
			setBlock(i, false);
			monitoring[i] = false;
		}

	}

	/**
	 * Setup index for touchscreen device
	 */
	public void setupTocuh(int index) {
		touchIndex = index;
	}
	
	/**
	 * Starts monitoring the touch device
	 * @return index of the touch device if successful -1 if not
	 */
	public int monitorTouch() {
		if(touchIndex!=-1){
			monitorDevice(touchIndex, true);
			return touchIndex;
		}else
			return -1;

	}
	/**
	 *Sets the touchIndex with the index value from the touchscreen if the 
	 *touchscreen is labeled with either "input" or "touch"
	 */
	private void autoSetTouch() {
		touchIndex = -1;
		for (int i = 0; i < dev.size(); i++) {
			if (dev.get(i).getName().contains("input")
					|| dev.get(i).getName().contains("touch")) {
				touchIndex = i;
				return;
			}
		}
	}
	
	/**
	 * Returns message describing the touch event
	 * @param type
	 * @return
	 */
	private String touchMessage(int type) {
		int x = tpr.getLastX();
		int y = tpr.getLastY();
		String s = null;
		switch (type) {
		case TouchPatternRecognizer.TOUCHED:
			s = "Touched: " + CoreController.getNodeAt(x, y) + " x:" + x
					+ " y:" + y;
			break;
		case TouchPatternRecognizer.SLIDE:
			s = "Slide: " + tpr.getOriginX() + " x" + tpr.getOriginY()
					+ " y --> " + x + "x " + y + "y";
			break;
		case TouchPatternRecognizer.LONGPRESS:
			s = "LongPress: " + x + "x " + y + "y";
			break;
		}
		return s;
	}

}
