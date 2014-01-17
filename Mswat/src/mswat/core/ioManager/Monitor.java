package mswat.core.ioManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import mswat.core.CoreController;
import mswat.core.activityManager.HierarchicalService;
import mswat.core.ioManager.Events.InputDevice;
import mswat.core.logger.Logger;
import mswat.examples.adapt.TouchAdapter;
import mswat.touch.TouchPatternRecognizer;

public class Monitor {

	// debugging tag
	private final String LT = "Monitor";

	// List of internal devices (touch, accelerometer)
	ArrayList<InputDevice> dev;

	// Core controller
	CoreController cc;


	private int touchIndex=-1;

	private TouchPatternRecognizer tpr = new TouchPatternRecognizer();

	boolean monitoring[];
	

	private static int IOMessagesThreshold = 50;

	private boolean virtualDriveEnable = false;

	private boolean broadcastIO = false;

	private static boolean calibrating = false;

	/**
	 * Initialises list of devices Initialises Logger if logging is true
	 * @param touchIndex2 
	 * 
	 * @param logging
	 */
	public Monitor( HierarchicalService hs, boolean broadcastIO, int touchIndex) {
		Events ev = new Events();
		dev = ev.Init();
		
		this.touchIndex= touchIndex;
		monitoring = new boolean[dev.size()];
		

		this.broadcastIO = broadcastIO;

	}

	/**
	 * Log keystroke if logger is enable
	 * TODO
	 * @param keypressed
	 */
	public void registerKeystroke(String keypressed) {
		
		//TODO Log keystrokes
		CoreController.updateLoggers("Previous Keystroke: " + keypressed);
		//if (logging)
			//Logger.registerTouch("Previous Keystroke: " + keypressed);

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
	 */
	public void inject(int index, int type, int code, int value) {
		dev.get(index).send(index, type, code, value);
	}

	/**
	 * Inject events into the virtual touch device
	 * 
	 * @requires createVirtualTouchDrive() before
	 * @param type
	 * @param code
	 * @param value
	 */
	public void injectToVirtual(int type, int code, int value) {
		if (virtualDriveEnable) {
			Events.sendVirtual(type, code, value);
		}
	}

	public void stopCalibration() {
		calibrating = false;
	}

	/**
	 * Monitor for calibration
	 */
	public void setCalibration() {

		calibrating = true;

		Thread b = new Thread(new Runnable() {

			public void run() {
				TouchPatternRecognizer tp= new TouchPatternRecognizer();
				Looper.prepare();
				do{
					SystemClock.sleep(500);
				}while (touchIndex==-1);
				InputDevice idev = dev.get(touchIndex);
				while (calibrating) {
					if (idev.getOpen() && idev.getPollingEvent() == 0) {

						int type = idev.getSuccessfulPollingType();
						int code = idev.getSuccessfulPollingCode();
						int value = idev.getSuccessfulPollingValue();

						if (calibrating)
							tp.identifyOnRelease(type, code, value, idev.getTimeStamp());

					}
				}

				CoreController.setScreenSize(tp.getLastX() + 25,
						tp.getLastY() + 125);

				Log.d(LT, "height:" + CoreController.S_HEIGHT + " width:"
						+ CoreController.S_WIDTH);

			}
		});
		b.start();

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

						int eventsGathered = 0;

						ArrayList<String> event_devices = new ArrayList<String>();
						ArrayList<Integer> event_types = new ArrayList<Integer>();
						ArrayList<Integer> event_codes = new ArrayList<Integer>();
						ArrayList<Integer> event_values = new ArrayList<Integer>();
						ArrayList<Integer> event_timestamps = new ArrayList<Integer>();

						while (monitoring[index]) {

							if (idev.getOpen() && idev.getPollingEvent() == 0) {
								int type = idev.getSuccessfulPollingType();
								int code = idev.getSuccessfulPollingCode();
								int value = idev.getSuccessfulPollingValue();
								int timestamp = idev.getTimeStamp();
								Log.d(LT, type + " " + code + " " + value + " " +  timestamp );
								CoreController.updateIOReceivers(
										index, type, code, value,
										timestamp);
							
								 /*
								 * if (logging) { logger.registerTouch(message);
								 * }*/
							

								// Gather io events and only broadcast if
								// events gathered are > ioMessagesThreshold
								if (broadcastIO) {
									event_devices.add(idev.getName());
									event_types.add(type);
									event_codes.add(code);
									event_values.add(value);
									event_timestamps.add(idev.getTimeStamp());

									eventsGathered++;
									if (eventsGathered > IOMessagesThreshold) {
										eventsGathered = 0;

										String[] devices = new String[event_devices
												.size()];
										devices = event_devices
												.toArray(devices);

										int[] types = new int[event_types
												.size()];
										types = convertIntegers(event_types);

										int[] codes = new int[event_codes
												.size()];
										codes = convertIntegers(event_codes);

										int[] values = new int[event_values
												.size()];
										values = convertIntegers(event_values);

										int[] timestamps = new int[event_timestamps
												.size()];
										timestamps = convertIntegers(event_timestamps);

										event_devices.clear();
										event_types.clear();
										event_codes.clear();
										event_values.clear();
										event_timestamps.clear();

										CoreController.monitorMessages(devices,
												types, codes, values,
												timestamps);
										// }

									}
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
	public void setupTouch(int index) {
		touchIndex = index;
	}



	/**
	 * Creates a virtual touch drive
	 */
	public void createVirtualTouchDrive() {
		dev.get(0).createVirtualDrive(dev.get(touchIndex).getName());

		virtualDriveEnable = true;
	}

	/**
	 * Starts monitoring the touch device
	 * 
	 * @return index of the touch device if successful -1 if not
	 */
	public int monitorTouch() {

		if (touchIndex != -1) {
			monitorDevice(touchIndex, true);
			return touchIndex;
		} else
			return -1;

	}

	



	private static int[] convertIntegers(List<Integer> integers) {
		int[] ret = new int[integers.size()];
		Iterator<Integer> iterator = integers.iterator();
		for (int i = 0; i < ret.length; i++) {
			ret[i] = iterator.next().intValue();
		}
		return ret;
	}

}
