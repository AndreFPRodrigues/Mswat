package mswat.core;

import java.util.ArrayList;
import java.util.Hashtable;

import mswat.controllers.WifiControl;
import mswat.core.activityManager.HierarchicalService;
import mswat.core.activityManager.Node;
import mswat.core.activityManager.NodeListController;
import mswat.core.activityManager.R;
import mswat.core.feedback.FeedBack;
import mswat.core.ioManager.Monitor;
import mswat.core.logger.Logger;
import mswat.core.macro.MacroManagment;
import mswat.core.macro.RunMacro;
import mswat.core.macro.TouchMonitor;
import mswat.interfaces.ContentReceiver;
import mswat.interfaces.IOReceiver;
import mswat.interfaces.NotificationReceiver;
import mswat.keyboard.SwatKeyboard;
import mswat.touch.TouchRecognizer;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;

public class CoreController {

	// debugging tag
	private final static String LT = "CoreController";

	// Modules where to forward messages
	private static NodeListController nController;
	private static Monitor monitor;
	private static MacroManagment macroM;

	private static String controller;
	private static String keyboard;
	private static boolean logIo;
	private static boolean logNav;
	private static boolean logAtTouch;

	// List of receivers
	private static Hashtable<Integer, IOReceiver> ioReceivers;
	private static ArrayList<ContentReceiver> contentReceivers;
	private static ArrayList<NotificationReceiver> notificationReceivers;
	private static ArrayList<Logger> loggers;

	// active keyboard
	private static SwatKeyboard activeKeyboard = null;

	// active touch recognizer
	private static TouchRecognizer tpr = null;
	private static String tprPreference;
	// Context
	private static HierarchicalService hs;

	// Navigation Variables
	public final static int NAV_NEXT = 0;
	public final static int NAV_PREV = 1;
	public final static int SELECT_CURRENT = 2;
	public final static int FOCUS_INDEX = 3;
	public final static int HIGHLIGHT_INDEX = 4;

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

	private static String currentMacro;
	private static int macroMode = MacroManagment.NAV_MACRO;
	// TODO get from preferences to allow touch macros
	private static boolean allowMacroTouch = true;
	private static TouchMonitor tm;

	/**
	 * Initialise CoreController
	 * 
	 * @param nController
	 * @param monitor
	 * @param hierarchicalService
	 * @param controller
	 * @param waitForCalibration
	 * @param logNav
	 * @param logAtTouch
	 * @param keyboard
	 * @param tpr2
	 */
	public CoreController(NodeListController nController, Monitor monitor,
			HierarchicalService hierarchicalService, String controller,
			boolean waitForCalibration, boolean logIO, boolean logNav,
			boolean logAtTouch, String keyboard, String tprPreference) {
		CoreController.monitor = monitor;
		CoreController.nController = nController;
		CoreController.macroM = new MacroManagment();
		hs = hierarchicalService;

		// initialise arrayListReceivers
		ioReceivers = new Hashtable<Integer, IOReceiver>();
		contentReceivers = new ArrayList<ContentReceiver>();
		notificationReceivers = new ArrayList<NotificationReceiver>();
		loggers = new ArrayList<Logger>();

		this.controller = controller;
		this.logIo = logIO;
		this.keyboard = keyboard;
		this.tprPreference = tprPreference;
		this.logNav = logNav;
		this.logAtTouch = logAtTouch;

		Log.d(LT, "log:" + logIO);

		// Broadcast the init signal
		if (!waitForCalibration) {

			startService();
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
		Log.d(LT, "w:" + M_WIDTH + " h:" + M_HEIGHT);

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
		ioReceivers.put(size, ioReceiver);
		Log.d(LT, "io receiver put:" + size);

		return size;
	}

	/**
	 * Unregister IOReceiver
	 */
	public static void unregisterIOReceiver(int key) {
		ioReceivers.remove(key);
		Log.d(LT, "io remove:" + key);

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
			
			if (ioReceivers.get(i) != null)
				ioReceivers.get(i).onUpdateIO(device, type, code, value,
						timestamp);
		}
	}

	public static void sendTouchIOReceivers(int type) {
		int size = ioReceivers.size();
		for (int i = 0; i < size; i++) {

			ioReceivers.get(i).onTouchReceived(type);
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

					monitor.createVirtualTouchDrive(index);
					break;
				case SETUP_TOUCH:
					hs.storeTouchIndex(index);
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

	public static void injectToTouch(int t, int c, int v) {
		monitor.injectToTouch(t, c, v);
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

		int size = contentReceivers.size();
		for (int i = 0; i < size; i++) {
			;
			content = prepareContent(contentReceivers.get(i).getType(), content);
			contentReceivers.get(i).onUpdateContent(content);
		}
	}

	private static ArrayList<Node> prepareContent(int contentType,
			ArrayList<Node> content) {

		Rect outBounds = new Rect();
		ArrayList<Node> result = new ArrayList<Node>();

		switch (contentType) {
		case ContentReceiver.ALL_CONTENT:
			return content;
		case ContentReceiver.CLICKABLE:
			for (Node n : content) {
				n.getAccessNode().getBoundsInScreen(outBounds);
				AccessibilityNodeInfo parent = n.getAccessNode().getParent();
				if ((n.getAccessNode().isClickable() || parent.isClickable())
						&& (outBounds.centerX() > 0 && outBounds.centerY() > 0))
					result.add(n);
			}
			return result;
		case ContentReceiver.DESCRIBABLE:
			String debug;
			for (Node n : content) {
				n.getAccessNode().getBoundsInScreen(outBounds);

				if ((n.getAccessNode().getText() != null || n.getAccessNode()
						.getContentDescription() != null)
						&& (outBounds.centerX() > 0 && outBounds.centerY() > 0)) {
					result.add(n);
				}

			}
			return result;
		case ContentReceiver.INTERACTIVE:
			for (Node n : content) {
				n.getAccessNode().getBoundsInScreen(outBounds);
				AccessibilityNodeInfo parent = n.getAccessNode().getParent();

				if ((n.getAccessNode().getText() != null || n.getAccessNode()
						.getContentDescription() != null)
						&& (n.getAccessNode().isClickable() || parent
								.isClickable())
						&& (outBounds.centerX() > 0 && outBounds.centerY() > 0))
					result.add(n);
			}
			return result;

		case ContentReceiver.INTERACTIVE_CHILDREN:
			for (Node n : content) {
				n.getAccessNode().getBoundsInScreen(outBounds);
				AccessibilityNodeInfo parent = n.getAccessNode().getParent();

				if ((n.getAccessNode().getText() != null || n.getAccessNode()
						.getContentDescription() != null)
						&& (n.getAccessNode().isClickable() || parent
								.isClickable())
						&& (outBounds.centerX() > 0 && outBounds.centerY() > 0)) {

					boolean childClickable = false;
					int childCount;
					if ((childCount = n.getAccessNode().getChildCount()) > 0) {
						for (int i = 0; i < childCount; i++) {
							AccessibilityNodeInfo child = n.getAccessNode()
									.getChild(i);
							if (child.isClickable())
								childClickable = true;

						}
					}
					if (!childClickable)
						result.add(n);

				}
			}
			return result;
		}
		return content;
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
	 * @param string
	 *            - FOCUS_INDEX
	 */
	public static void commandNav(final int command, final String string) {
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
					String navTo = nController.selectFocus();
					if (logNav && navTo.length() > 0) {
						updateLoggers("Nav To: " + navTo);
					}
					break;
				case FOCUS_INDEX:
					nController.focusIndex(string);
					break;
				case HIGHLIGHT_INDEX:
					nController.highlightIndex(string);
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

	public static Node getNodeByIndex(int index) {

		return nController.getNodeByIndex(index);
	}

	/**
	 * Return the index of the node that contains point with coord x and y
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static int getNodeIndexAt(double x, double y) {
		return nController.getNodeIndexAt(xToScreenCoord(x), yToScreenCoord(y));
	}

	public static String getNodeNameByIndex(int index) {
		return nController.getNodeNameByIndex(index);
	}

	/**
	 * Return the index of the node by name
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static int getNodeIndexByName(String name) {
		return nController.getNodeIndexByName(name);
	}

	/**
	 * Return a array string with the descriptions of the 4 nearest nodes (if
	 * they exist) in the four directions
	 * 
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static String[] getNearNode(int x, int y, int radius) {
		return nController.getNearNode(xToScreenCoord(x), yToScreenCoord(y),
				radius);
	}

	/**
	 * Return index of the nearest node
	 * 
	 * 
	 * @param x
	 * @param y
	 * @param lastUp
	 * @param doubleClickLastUp
	 * @return
	 */
	public static int getNearestNode(int x, int y, String node1, String node2) {
		return nController.getNearestNode(xToScreenCoord(x), yToScreenCoord(y),
				node1, node2);
	}

	/**
	 * Return distance to the nearest node
	 * 
	 * 
	 * @param x
	 * @param y
	 * @param lastUp
	 * @param doubleClickLastUp
	 * @return
	 */
	public static int getNearestNodeDistance(int x, int y, String node1,
			String node2) {
		return nController.getNearestNodeDistance(xToScreenCoord(x),
				yToScreenCoord(y), node1, node2);
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
	 * Enables visual feedback after a clear
	 */
	public static void enableHightlights() {
		FeedBack.enableHightlights();
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
		FeedBack.addHighlight(marginTop, marginLeft, alpha, width, height,
				Color.BLUE);
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
		// Log.d(LT, "Mwidth:" + M_WIDTH + " Swidth:" + S_WIDTH + " x:" + x);
		// Log.d(LT, "m height:" + M_HEIGHT);
		return (int) (M_WIDTH / S_WIDTH * x);
	}

	/**
	 * Calculate the mapped coordenate of y
	 * 
	 * @param x
	 * @return
	 */
	public static int yToScreenCoord(double y) {
		// Log.d(LT, "Mheight:" + M_HEIGHT + " Sheight:" + S_HEIGHT + " y:" +
		// y);

		return (int) (M_HEIGHT / S_HEIGHT * y);
	}

	public static void stopService() {
		// Broadcast event
		Intent intent = new Intent();
		intent.setAction("mswat_stop");
		hs.sendBroadcast(intent);
		hs.stopService();
	}

	public static void stopServiceNoBroadCast() {
		hs.stopService();
	}

	private static void startService() {

		Log.d(LT, "STARTING SERVICE");
		// Broadcast event
		Intent intent = new Intent();
		intent.setAction("mswat_init");
		intent.putExtra("controller", controller);
		intent.putExtra("logIO", logIo);
		intent.putExtra("logAtTouch", logAtTouch);
		intent.putExtra("keyboard", keyboard);
		hs.sendBroadcast(intent);

	}

	public void setCalibration() {
		monitor.setCalibration();
	}

	public static void stopCalibration() {
		monitor.stopCalibration();
		startService();
	}

	public static void setScreenSize(int width, int height) {
		height = height - 80;
		CoreController.S_HEIGHT = height;
		CoreController.S_WIDTH = width;
		Log.d(LT, "width:" + width + " height:" + height);
		hs.storeScreenSize(width, height);
	}

	/**
	 * Returns to home
	 * 
	 * @return
	 */
	public static boolean home() {
		return hs.home();
	}

	/**
	 * Returns to home
	 */
	public static boolean back() {
		return hs.back();
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
		if (note.equals("[]")) {
			return;
		}

		note = note.substring(1, note.length() - 1);

		for (int i = 0; i < size; i++) {
			notificationReceivers.get(i).onNotification(note);
		}

	}

	public static void startKeyboard() {
		if (activeKeyboard != null)
			activeKeyboard.start();
		// Broadcast event
		Intent intent = new Intent();
		intent.setAction("mswat_keyboard");
		intent.putExtra("status", true);
		hs.sendBroadcast(intent);
	}

	public static void updateKeyboardUI() {
		if (activeKeyboard != null)
			activeKeyboard.update();

	}

	public static void stopKeyboard() {
		// Broadcast event
		Intent intent = new Intent();
		intent.setAction("mswat_keyboard");
		intent.putExtra("status", false);
		hs.sendBroadcast(intent);
	}

	public static void callKeyboardWriteChar(int code) {
		Intent intent = new Intent();
		intent.setAction("mswat_pressKey");
		intent.putExtra("code", code);
		intent.putExtra("direct", false);
		hs.sendBroadcast(intent);
	}

	public static void callKeyboardWriteDirect(int code) {
		Intent intent = new Intent();
		intent.setAction("mswat_pressKey");
		intent.putExtra("code", code);
		intent.putExtra("direct", true);
		hs.sendBroadcast(intent);
	}

	public static void callKeyboardWriteString(int[] array) {
		Intent intent = new Intent();
		intent.setAction("mswat_writeString");
		intent.putExtra("codes", array);
		hs.sendBroadcast(intent);

	}

	public static void registerActivateKeyboard(SwatKeyboard keyboard) {
		activeKeyboard = keyboard;
	}

	public static void registerActivateTouch(TouchRecognizer tpr2) {
		tpr = tpr2;
	}

	public static TouchRecognizer getActiveTPR() {
		return tpr;
	}

	// Screen reader function convert pixels to milimeter for android nexus s
	public static int convertToMilY(int y) {
		return (124 * y) / 800 * 5;
	}

	// Screen reader function convert pixels to milimeter for android nexus s
	public static int convertToMilX(int x) {
		return (63 * x) / 480 * 5;
	}

	public static double distanceBetween(double x, double y, double x1,
			double y1) {
		return Math.sqrt(Math.pow(y - y1, 2) + Math.pow(x - x1, 2));
	}

	public static void writeToLog(ArrayList<String> toLog, String filepath) {
		if (loggers.size() > 0) {
			Log.d(LT, "Writing to log:"  + filepath);
			loggers.get(0).registerToLog(toLog, filepath);
		}

	}

	public static void addMacroStep(String text) {
		macroM.addStepTo(currentMacro, text);
	}

	public static void changeModeMacro() {
		if (macroMode == MacroManagment.NAV_MACRO)
			macroMode = MacroManagment.TOUCH_MACRO;
		else {
			addMacroStep(tm.getTouches());
			macroMode = MacroManagment.NAV_MACRO;
		}
		macroM.changeMode(macroMode);
		tm.setMode(macroMode);
		hs.setMacroMode(macroMode);
	}

	public static void newMacro(String name) {
		if (home()) {
			FeedBack.macroCommands();
			currentMacro = name;
			macroM.createMacro(name);
			hs.setCreateMacro(true);
			tm = new TouchMonitor();
		}
	}

	public static void finishMacro() {
		if (macroMode == MacroManagment.TOUCH_MACRO) {
			macroMode = MacroManagment.NAV_MACRO;
			addMacroStep(tm.getTouches());
		}
		CoreController.clearHightlights();
		hs.setCreateMacro(false);
		tm.finish();
		if (macroM.finishMacro())
			shortcutIcon(currentMacro);
	}

	private static void shortcutIcon(String name) {

		Intent shortcutIntent = new Intent(hs.getApplicationContext(),
				RunMacro.class);
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		shortcutIntent.putExtra("macro", name);

		Intent addIntent = new Intent();
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
		addIntent.putExtra(
				Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				Intent.ShortcutIconResource.fromContext(
						hs.getApplicationContext(), R.drawable.ic_launcher));
		addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
		hs.getApplicationContext().sendBroadcast(addIntent);
	}

	public static void runMacro(String macro) {

		hs.runMacro(macroM.runMacro(macro));
	}

}
