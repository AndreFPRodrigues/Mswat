package mswat.core;

import java.util.ArrayList;

import mswat.core.activityManager.HierarchicalService;
import mswat.core.activityManager.Node;
import mswat.core.activityManager.NodeListController;
import mswat.core.feedback.FeedBack;
import mswat.core.ioManager.Monitor;
import mswat.core.logger.Logger;
import mswat.interfaces.ContentReceiver;
import mswat.interfaces.IOReceiver;
import mswat.interfaces.NotificationReceiver;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

public class CoreController {

	// debugging tag
	private final static String LT = "CoreController";

	// Modules where to forward messages
	private static NodeListController nController;
	private static Monitor monitor;

	private static String controller;
	private static boolean logging;

	// List of receivers
	private static ArrayList<IOReceiver> ioReceivers;
	private static ArrayList<ContentReceiver> contentReceivers;
	private static ArrayList<NotificationReceiver> notificationReceivers;

	private static ArrayList<Logger> loggers;

	// Context
	private static HierarchicalService hs;

	// Navigation Variables
	public final static int NAV_NEXT = 0;
	public final static int NAV_PREV = 1;
	public final static int SELECT_CURRENT = 2;
	public final static int FOCUS_INDEX = 3;

	// IO Variables
	public final static int SET_BLOCK = 0;
	public final static int MONITOR_DEV = 1;
	public final static int CREATE_VIRTUAL_TOUCH = 2;
	public final static int SETUP_TOUCH = 3;
	public final static int SET_TOUCH_RAW = 4;
	public final static int FOWARD_TO_VIRTUAL = 5;

	// Mapped screen resolution
	public static double M_WIDTH;
	public static double M_HEIGHT;

	// Screen resolution
	public static double S_WIDTH = 1024;
	public static double S_HEIGHT = 960;

	// keyboard active
	public static boolean keyboardState = false;

	/**
	 * Initialise CoreController
	 * 
	 * @param nController
	 * @param monitor
	 * @param hierarchicalService
	 * @param controller
	 * @param waitForCalibration
	 */
	public CoreController(NodeListController nController, Monitor monitor,
			HierarchicalService hierarchicalService, String controller,
			boolean waitForCalibration, boolean logging) {
		CoreController.monitor = monitor;
		CoreController.nController = nController;
		hs = hierarchicalService;

		// initialise arrayListReceivers
		ioReceivers = new ArrayList<IOReceiver>();
		contentReceivers = new ArrayList<ContentReceiver>();
		notificationReceivers = new ArrayList<NotificationReceiver>();

		this.controller = controller;
		this.logging = logging;

		Log.d(LT, "log:" + logging);

		// Broadcast the init signal
		if (!waitForCalibration) {
			Log.d(LT, "STARTING SERVICE");

			startService(controller, logging);
		} else
			Log.d(LT, "CALIBRATION");

		// get screen resolution
		WindowManager wm = (WindowManager) hierarchicalService
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		Point size = new Point();
		display.getSize(size);
		M_WIDTH = size.x;
		M_HEIGHT = size.y;
	}

	/***********************************
	 * IO Commands and messages
	 * 
	 ************************************* */

	/**
	 * Register logger receiver (receives keystrokes info)
	 * 
	 * @param logger
	 * @return
	 */
	public static int registerLogger(Logger logger) {
		loggers.add(logger);
		return loggers.size() - 1;
	}

	/**
	 * Register IO events receiver
	 * 
	 * @param ioReceiver
	 * @return
	 */
	public static int registerIOReceiver(IOReceiver ioReceiver) {
		int size = ioReceivers.size();
		ioReceivers.add(ioReceiver);
		return size;
	}

	/**
	 * Io event propagated to io receivers
	 * 
	 * @param device
	 * @param type
	 * @param code
	 * @param value
	 * @param timestamp
	 */
	public static void updateIOReceivers(int device, int type, int code,
			int value, int timestamp) {
		int size = ioReceivers.size();
		for (int i = 0; i < size; i++) {
			ioReceivers.get(i).onUpdateIO(device, type, code, value, timestamp);
		}

	}

	/**
	 * Keystrokes propagated to loggers
	 * 
	 * @param record
	 *            string representing the keystroke
	 */
	public static void updateLoggers(String record) {
		int size = loggers.size();
		for (int i = 0; i < size; i++) {
			loggers.get(i).logToFile(record);
		}

	}

	/**
	 * Broadcasts raw monitor messages
	 * 
	 * @param device
	 *            - event origin
	 * @param type
	 * @param code
	 * @param value
	 * @param timestamp
	 */
	public static void monitorMessages(final String[] devices,
			final int[] types, final int[] codes, final int[] values,
			final int[] timestamps) {
		Thread b = new Thread(new Runnable() {
			public void run() {

				if (hs != null) {

					// Broadcast event
					Intent intent = new Intent();
					intent.setAction("monitor");
					intent.putExtra("dev", devices);
					intent.putExtra("type", types);
					intent.putExtra("code", codes);
					intent.putExtra("value", values);
					intent.putExtra("timestamp", timestamps);
					hs.sendBroadcast(intent);

				}
			}
		});
		b.start();

	}

	/**
	 * Broadcasts identified touch messages
	 */
	public static void touchMessage(final int type, final String message) {
		Thread b = new Thread(new Runnable() {
			public void run() {

				if (hs != null) {

					// Broadcast event
					Intent intent = new Intent();
					intent.setAction("monitor");
					intent.putExtra("type", type);
					intent.putExtra("message", message);
					hs.sendBroadcast(intent);

				}
			}
		});
		b.start();

	}

	/**
	 * Forwards the message to the appropriate component
	 * 
	 * @param command
	 *            - SET_BLOCK/MONITOR_DEV/CREATE_VIRTUAL_TOUCH/SETUP_TOUCH
	 * @param index
	 *            - device index for SET_BLOCK/MONITOR_DEV/SETUP_TOUCH
	 * @param state
	 *            - state SET_BLOCK/MONITOR_DEV
	 */
	public static void commandIO(final int command, final int index,
			final boolean state) {

		Thread b = new Thread(new Runnable() {
			public void run() {

				// Separates and fowards messages to the apropriate module
				switch (command) {
				case SET_BLOCK:
					Log.d(LT, "Block device: " + state);
					monitor.setBlock(index, state);
					break;
				case MONITOR_DEV:
					monitor.monitorDevice(index, state);
					break;
				case CREATE_VIRTUAL_TOUCH:
					Log.d(LT, "Create virtual drive ");

					monitor.createVirtualTouchDrive();
					break;
				case SETUP_TOUCH:
					monitor.setupTouch(index);
					break;

				}
			}
		});
		b.start();
	}

	/**
	 * Inject event into touch virtual drive
	 * 
	 * @requires virtual touch driver created
	 * @param t
	 *            type
	 * @param c
	 *            code
	 * @param v
	 *            value
	 */
	public static void injectToVirtual(int t, int c, int v) {
		monitor.injectToVirtual(t, c, v);
	}

	/**
	 * Inject event into the device on the position index
	 * 
	 * @param index
	 * @param type
	 * @param code
	 * @param value
	 */
	public static void inject(int index, int type, int code, int value) {
		monitor.inject(index, type, code, value);
	}

	public static int monitorTouch() {
		return monitor.monitorTouch();
	}

	/**
	 * Get list of internal devices (touchscree, keypad, etc)
	 * 
	 * @return
	 */
	public static String[] getDevices() {
		return monitor.getDevices();
	}

	/*************************************************
	 * Navigation and content Commands and messages
	 * 
	 ************************************************** 
	 **/

	/**
	 * Register content update receiver
	 * 
	 * @param contentReceiver
	 * @return
	 */
	public static int registerContentReceiver(ContentReceiver contentReceiver) {
		contentReceivers.add(contentReceiver);
		return contentReceivers.size() - 1;
	}

	/**
	 * Content update event propagated to content update receivers
	 * 
	 * @param content
	 */
	public static void updateContentReceivers(ArrayList<Node> content) {
		if (keyboardState) {

			if (hs.getResources().getConfiguration().keyboardHidden == hs
					.getResources().getConfiguration().KEYBOARDHIDDEN_YES) {
				keyboardState = false;
				Log.d(LT, "keyboard hidden");
			}
		}
		int size = contentReceivers.size();
		for (int i = 0; i < size; i++) {
			contentReceivers.get(i).onUpdateContent(content);
		}
	}

	/**
	 * Broadcasts node controller messages Message format [ node, node] Node
	 * format (<description/text>, <x center coord>, <y center coord>, <icon
	 * index>) *icon index = -1 if no correspondent icon found
	 * 
	 * @param message
	 */
	public static void nodeMessages(final String message) {
		Thread b = new Thread(new Runnable() {
			public void run() {
				if (hs != null) {

					// Broadcast event
					Intent intent = new Intent();
					intent.setAction("contentUpdate");
					intent.putExtra("content", message);
					hs.sendBroadcast(intent);

				}
			}
		});
		b.start();
	}

	/**
	 * Forwards the message to the appropriate component
	 * 
	 * @param command
	 *            NAV_NEXT/NAV_PREV/SELECT_CURRENT/FOCUS_INDEX
	 * @param index
	 *            - FOCUS_INDEX
	 */
	public static void commandNav(final int command, final int index) {
		Thread b = new Thread(new Runnable() {
			public void run() {

				// Separates and fowards messages to the apropriate module
				switch (command) {
				case NAV_NEXT:
					nController.navNext();
					break;
				case NAV_PREV:
					nController.navPrev();
					break;
				case SELECT_CURRENT:
					nController.selectFocus();
					break;
				case FOCUS_INDEX:
					nController.focusIndex(index);
					break;

				}
			}
		});
		b.start();
	}

	/**
	 * Return list with the describable nodes TODO - how to represent the tree -
	 * implement gathering all the nodes info
	 * 
	 * @return
	 */
	public static ArrayList<Node> getContent() {
		return nController.getContent();
	}

	/**
	 * Return list with all the view nodes
	 * 
	 * @return
	 */
	public static ArrayList<Node> getFullContent() {
		return nController.getFullContent();
	}

	/**
	 * Return the node that contains point with coord x and y
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static String getNodeAt(double x, double y) {
		return nController.getNodeAt(xToScreenCoord(x), yToScreenCoord(y));
	}

	/**
	 * Returns drawable with the description s TODO
	 * 
	 * @param s
	 * @return
	 */
	public static Drawable getIcon(String s) {
		// TODO getIcon
		return nController.getIcon(s);
	}

	/**
	 * Sets highlighting to automatic
	 * 
	 * @param state
	 */
	public static void setAutoHighlight(boolean state) {
		nController.setAutoHighlight(state);
	}

	/**
	 * Sets audio feedback (tts) to automatic
	 * 
	 * @param state
	 */
	public static void setAutoTTS(boolean state) {
		nController.setAutoHighlight(state);
	}

	/*************************************************
	 * Feeback methods
	 * 
	 ************************************************** 
	 **/

	/**
	 * Removes all highlights and adds a new overlay
	 * 
	 * @param marginTop
	 *            - margin to the top of the screen
	 * @param marginLeft
	 *            - margin to the left of the screen
	 * @param alpha
	 *            - overlay transparency
	 * @param width
	 *            - width of the overlay
	 * @param height
	 *            - overlay height
	 * @param color
	 *            - int color
	 */
	public static void hightlight(int marginTop, int marginLeft, float alpha,
			int width, int height, int color) {
		FeedBack.hightlight(marginTop, marginLeft, alpha, width, height, color);
	}

	/**
	 * Clears all highlights
	 */
	public static void clearHightlights() {
		FeedBack.clearHightlights();
	}

	/**
	 * Text to speech returns false when it stops speaking
	 * 
	 * @param text
	 */
	public static boolean waitFortextToSpeech(String text) {
		return FeedBack.waitFortextToSpeech(text);
	}

	/**
	 * Text to speech
	 * 
	 * @param text
	 */
	public static void textToSpeech(String text) {
		FeedBack.textToSpeech(text);
	}

	/**
	 * TODO add a highlight
	 * 
	 * @param marginTop
	 * @param marginLeft
	 * @param alpha
	 * @param width
	 * @param height
	 */
	public static void addHightlight(int marginTop, int marginLeft,
			float alpha, int width, int height) {
		FeedBack.addHightlight(marginTop, marginLeft, alpha, width, height);
	}

	/*************************************************
	 * Auxiliary functions
	 * 
	 ************************************************** 
	 **/
	/**
	 * Calculate the mapped coordinate of x
	 * 
	 * @param x
	 * @return
	 */
	public static int xToScreenCoord(double x) {
		return (int) (M_WIDTH / S_WIDTH * x);
	}

	/**
	 * Calculate the mapped coordenate of y
	 * 
	 * @param x
	 * @return
	 */
	public static int yToScreenCoord(double y) {
		return (int) (M_HEIGHT / S_HEIGHT * y);
	}

	public static void stopService() {
		// Broadcast event
		Intent intent = new Intent();
		intent.setAction("mswat_stop");
		hs.sendBroadcast(intent);
		hs.stopService();
	}

	private static void startService(String controller, boolean logging) {
		// Broadcast event
		Intent intent = new Intent();
		intent.setAction("mswat_init");
		intent.putExtra("controller", controller);
		intent.putExtra("logging", logging);
		hs.sendBroadcast(intent);
	}

	public void setCalibration() {
		monitor.setCalibration();
	}

	public static void stopCalibration() {
		monitor.stopCalibration();
		startService(controller, logging);
	}

	public static void setScreenSize(int width, int height) {
		CoreController.S_HEIGHT = height;
		CoreController.S_WIDTH = width;
		hs.storeScreenSize(width, height);
	}

	/**
	 * Returns to home
	 */
	public static void home() {
		hs.home();
	}

	/**
	 * Set allow screen to lock
	 * 
	 * @param state
	 */
	public static void lockScreen(boolean state) {
		hs.lockedScreen(state);
	}

	/**
	 * Register a notification receiver
	 * 
	 * @param nr
	 * @return
	 */
	public static int registerNotificationReceiver(NotificationReceiver nr) {
		notificationReceivers.add(nr);
		return notificationReceivers.size() - 1;
	}

	/**
	 * Update all notifications receivers
	 * 
	 * @param note
	 */
	public static void updateNotificationReceivers(String note) {
		int size = notificationReceivers.size();
		for (int i = 0; i < size; i++) {
			notificationReceivers.get(i).onNotification(note);
		}

	}

	public static void startKeyboard() {
		if (!keyboardState) {
			Log.d(LT, "keyboard visible");
			char c= ' ';
			int a = c;
			Log.d(LT, "valor" + a);
			keyboardState = true;
		}
	}

	public static void callKeyboardWriteChar(int code) {
		Intent intent = new Intent();
		intent.setAction("mswat_pressKey");
		intent.putExtra("code", code);
		hs.sendBroadcast(intent);
	}

	public static void callKeyboardWriteString(int[] array) {
		Intent intent = new Intent();
		intent.setAction("mswat_writeString");
		intent.putExtra("codes", array);
		hs.sendBroadcast(intent);
		
	}

}
