package mswat.core;

import java.util.ArrayList;

import mswat.core.activityManager.HierarchicalService;
import mswat.core.activityManager.Node;
import mswat.core.activityManager.NodeListController;
import mswat.core.feedback.FeedBack;
import mswat.core.ioManager.Monitor;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

public class CoreController {

	// debugging tag
	private final static String LT = "CoreController";

	// handles monitor messages
	// private final static Handler monitorMessages = new Handler();

	// handles received messages
	//private final static Handler receivedMessages = new Handler();

	// handles nodeListControllerMessages
	//private final Handler nodeControllerMessages = new Handler();

	// Modules where to forward messages
	private static NodeListController nController;
	private static Monitor monitor;

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
	
	// Mapped screen resolution
	public static	double M_WIDTH;
	public static	double M_HEIGHT;
	
	// Screen resolution
	public static double S_WIDTH = 1024;
	public static double S_HEIGHT = 960;
	
	

	/**
	 * Initialise CoreController
	 * 
	 * @param nController
	 * @param monitor
	 * @param hierarchicalService
	 */
	public CoreController(NodeListController nController, Monitor monitor,
			HierarchicalService hierarchicalService) {
		CoreController.monitor = monitor;
		CoreController.nController = nController;
		hs = hierarchicalService;
		
		//Broadcast the init signal
		startService();
		
		//get screen resolution
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
	 * Broadcasts raw monitor messages
	 * 
	 * @param device
	 *            - event origin
	 * @param type
	 * @param code
	 * @param value
	 * @param timestamp
	 */
	public static void monitorMessages(final String device, final int type,
			final int code, final int value, final int timestamp) {
		Thread b = new Thread(new Runnable() {
			public void run() {
		
				if (hs != null) {
		
					// Broadcast event
					Intent intent = new Intent();
					intent.setAction("monitor");
					intent.putExtra("dev", device);
					intent.putExtra("type", type);
					intent.putExtra("code", code);
					intent.putExtra("value", value);
					intent.putExtra("timestamp", timestamp);
					hs.sendBroadcast(intent);
		
				}
			}
		});
		b.start();

	}
	
	/**
	 * Broadcasts touch messages
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
	 * @param message
	 */
	public static void commandIO(final int command, final int index,
			final boolean state) {
		Thread b = new Thread(new Runnable() {
			public void run() {

				// Separates and fowards messages to the apropriate module
				switch (command) {
				case SET_BLOCK:
					monitor.setBlock(index, state);
					break;
				case MONITOR_DEV:
					monitor.monitorDevice(index, state);
					break;
				}
			}
		});
		b.start();
	}
	
	/**
	 * Inject event into the device on the position index
	 * @param index
	 * @param type
	 * @param code
	 * @param value
	 */
	public static void inject(int index, int type, int code, int value){
		monitor.inject(index, type, code, value);
	}
	
	public static int monitorTouch(){
		return monitor.monitorTouch();
	}
	
	/**
	 * Get list of internal devices (touchscree, keypad, etc)
	 * @return
	 */
	public static String[] getDevices(){
		return monitor.getDevices();
	}

	
	/*************************************************
	 * Navigation and content Commands and messages
	 * 
	**************************************************
	**/
	
	/**
	 * Broadcasts node controller messages
	 * Message format
	 * [ node, node]
	 * Node format
	 * (<description/text>, <x center coord>, <y center coord>, <icon index>)
	 *  *icon index = -1 if no correspondent icon found
	 * @param message
	 */
	public static void nodeMessages(final String message) {
		Thread b = new Thread(new Runnable(){
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
	 * @param message
	 */
	public static void commandNav(final int command, int index) {
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
					// TODO
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
	 * @param x 
	 * @param y
	 * @return
	 */
	public static String getNodeAt(double x, double y) {
		return nController.getNodeAt(xToScreenCoord(x), yToScreenCoord(y));
	}

	/**
	 * Returns drawable with the description s TODO
	 * @param s
	 * @return
	 */
	public static Drawable getIcon(String s) {
		// TODO getIcon
		return nController.getIcon(s);
	}
	
	/**
	 * Sets highlighting to automatic
	 * @param state
	 */
	public static void setAutoHighlight(boolean state){
		nController.setAutoHighlight(state);
	}
	
	/**
	 * Sets audio feedback (tts) to automatic
	 * @param state
	 */
	public static void setAutoTTS(boolean state){
		nController.setAutoHighlight(state);
	}
	
	/*************************************************
	 * Feeback methods
	 * 
	**************************************************
	**/
	
	/**
	 * Removes all highlights and adds a new overlay 
	 * @param marginTop - margin to the top of the screen
	 * @param marginLeft - margin to the left of the screen
	 * @param alpha - overlay transparency
	 * @param width - width of the overlay
	 * @param height - overlay height
	 * @param color - int color 
	 */
	public static void hightlight(int marginTop,   int marginLeft,
			  float alpha,   int width,  int height , int color){
		FeedBack.hightlight(marginTop, marginLeft, alpha, width, height, color);
	}
	
	/**
	 * Clears all highlights
	 */
	public static void clearHightlights(){
		FeedBack.clearHightlights();
	}
	
	/**
	 * Text to speech
	 * @param text
	 */
	public static void textToSpeech(String text){
		FeedBack.textToSpeech(text);
	}
	
	/**
	 * TODO add a highlight
	 * @param marginTop
	 * @param marginLeft
	 * @param alpha
	 * @param width
	 * @param height
	 */
	public static void addHightlight( int marginTop,  int marginLeft,
			 float alpha,  int width,  int height){
		FeedBack.addHightlight(marginTop, marginLeft, alpha, width, height);
	}
	
	/*************************************************
	 * Auxiliary functions
	 * 
	**************************************************
	**/
	/**
	 * Calculate the mapped coordinate of x
	 * @param x
	 * @return
	 */
	public static int xToScreenCoord(double x){
		return (int) (M_WIDTH / S_WIDTH * x);
	}
	
	/**
	 * Calculate the mapped coordenate of y
	 * @param x
	 * @return
	 */
	public static int yToScreenCoord (double y){
		return (int) (M_HEIGHT / S_HEIGHT * y);
	}
	
	public static void stopService(){
		hs.stopService();
	}
	
	private void startService(){
		// Broadcast event
		Intent intent = new Intent();
		intent.setAction("mswat_init");
		hs.sendBroadcast(intent);
	}

}
