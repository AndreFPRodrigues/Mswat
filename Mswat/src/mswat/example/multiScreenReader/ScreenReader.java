package mswat.example.multiScreenReader;

import java.util.ArrayList;
import java.util.Hashtable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import mswat.controllers.ControlInterface;
import mswat.core.CoreController;
import mswat.core.activityManager.Node;
import mswat.interfaces.ContentReceiver;
import mswat.interfaces.IOReceiver;
import mswat.touch.TPRNexusS;
import mswat.touch.TouchEvent;
import mswat.touch.TouchRecognizer;

public class ScreenReader extends ControlInterface implements IOReceiver {
	private final String LT = "ScreenReader";

	private int deviceIndex;
	private TouchRecognizer tpr; 
	private Hashtable<Integer, String> readingString;

	private int lastUpIndex = -1;
	private String lastUp;
	private String doubleClickLastUp;
	private String doubleClickRestore;
	private boolean changedUps = false;

	private int lastDown = 0;
	private int lastDownX = 0;
	private int lastDownY = 0;

	private TestTCPClient ttc;

	private final String MAN = "man/";
	private final String WOMAN = "woman/";
	private String voz = MAN;

	private final int EXPLORE = 0;
	private final int TORCH = 1;
	private int mode = EXPLORE;

	private final int SINGLE = 0;
	private final int MULTI = 1;
	private int exploreMode = MULTI;

	// detect left and right finger
	// private ArrayList<TouchEvent> touches = new ArrayList<TouchEvent>();

	private String teste;
	private ArrayList<String> sources;

	private Node toRead;

	private int refX;
	private int refY;

	private static boolean enabled;

	private String[] near;
 
	private ArrayList<String> toLog = new ArrayList<String>();
	private static String filepath;

	@Override 
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals("mswat_init_screenReader")) {
			Log.d(LT, "Registei");
			// register receiver
			registerIOReceiver();

			// log filepath
			filepath = Environment.getExternalStorageDirectory().toString()
					+ "/testLogs/io" + intent.getStringExtra("name") + ".text";

			// starts monitoring touchscreen
			deviceIndex = CoreController.monitorTouch();

			// blocks the touch screen
			CoreController.commandIO(CoreController.SET_BLOCK, deviceIndex,
					true);

			// touch recogniser
			tpr = CoreController.getActiveTPR();

			// current reading string
			readingString = new Hashtable<Integer, String>();

			// connects with the service
			// ttc = new TestTCPClient();

			// screen reader state
			enabled = true;

		} else if (intent.getAction().equals("mswat_stop") && enabled) {
			Log.d(LT, "Received STOP");
			CoreController.writeToLog((ArrayList<String>) toLog.clone(), filepath);
			
			CoreController.commandIO(CoreController.SET_BLOCK, deviceIndex,
					false);
			CoreController.stopServiceNoBroadCast();
		}

	}

	@Override
	public int registerIOReceiver() {
		return CoreController.registerIOReceiver(this);
	}

	@Override
	public void onUpdateIO(int device, int type, int code, int value,
			int timestamp) {

		// verify if the update is of the monitored device
		if (this.deviceIndex == device && enabled) {

			// get active touch recognizer
			if (tpr == null)
				tpr = CoreController.getActiveTPR();

			// identify the type of touch (UP/DOWN/MOVE)
			int result = tpr.identifyOnChange(type, code, value, timestamp);

			if (result != -1) {

				// Last touch characteristics
				String s;
				String log;
				int x = tpr.getLastX();
				int y = tpr.getLastY() + 20;
				int timestampTPR = tpr.getTimestamp();
				int identifier = tpr.getIdentifier();

				Log.d(LT, "x:" + x + " y:" + y + " id:" + identifier
						+ " Result:" + result);

				// Node of the touch
				s = CoreController.getNodeAt(x, y);

				// convert coord to screen coord
				refX = CoreController.convertToMilX(CoreController
						.xToScreenCoord(x) - 240) * 2;
				refY = CoreController.convertToMilY(800 - CoreController
						.yToScreenCoord(y)) / 2;

				// alternate between man and woman voice
				if (identifier % 2 == 0)
					voz = MAN;
				else
					voz = WOMAN;

				switch (result) {
				case TouchRecognizer.DOWN:
					// Log.d(LT, "DOWN");

					// Detect left or right finger
					/*
					 * if (touches.size() < 2) { if (touches.size() == 1) { if
					 * (touches.get(0).getX() < x) touches.add(new TouchEvent(x,
					 * y, (double) identifier)); else touches.add(0, new
					 * TouchEvent(x, y, (double) identifier));
					 * 
					 * } else { touches.add(new TouchEvent(x, y, (double)
					 * identifier)); } }
					 */
					
					//logging down
					log = TouchRecognizer.DOWN + "," + s + "," + x
							+ "," + y + "," + identifier + ","
							+ timestamp;
					toLog.add(log);
					
					// detect double click
					int dist = (int) Math.abs(CoreController.distanceBetween(x,
							y, lastDownX, lastDownY));

					if ((timestampTPR - lastDown) < 500 && dist < 100) {
						
						if (doubleClickRestore != null && changedUps) {
							lastUp = doubleClickLastUp;
							doubleClickLastUp = doubleClickRestore;
						}

						if (doubleClickLastUp != null || lastUp != null) {

				

							int indexNear;
							int multiIndexHighlight = -1;
							
							// check nearest focused node if multi touch
							if (exploreMode == SINGLE){
								indexNear = CoreController
										.getNodeIndexByName(lastUp);
								
								//logging double click
								log = TouchRecognizer.DOUBLE_CLICK + "," + lastUp;
								toLog.add(log);
							}
							else {

								indexNear = CoreController.getNearestNode(x, y,
										lastUp, doubleClickLastUp);
								if (indexNear < 0 && indexNear != -66) {
									indexNear = -indexNear;
									multiIndexHighlight = CoreController
											.getNodeIndexByName(lastUp);
								} else
									multiIndexHighlight = CoreController
											.getNodeIndexByName(doubleClickLastUp);
							}

							if (indexNear != -1) {
								Log.d(LT, "Double click i:" + indexNear);
								focusIndex(indexNear);
								SystemClock.sleep(100);
								selectCurrent();

								if (exploreMode == MULTI
										&& multiIndexHighlight != -1) {
									highlightIndex(multiIndexHighlight);
									lastUpIndex = indexNear;
								}

								// get nearest node name
								String write = CoreController
										.getNodeNameByIndex(indexNear);
								if (!write.equals(lastUp)) {
									String aux = lastUp;
									lastUp = write;
									doubleClickLastUp = aux;
									doubleClickRestore = aux;
								}
								Log.d(LT, "Nearest: " + write);
								
								// SEND down MESSAGE
								toRead = CoreController
										.getNodeByIndex(indexNear);

								if (exploreMode == SINGLE)
									teste = "addSource,src," + voz + lastUp
											+ ".wav,0,0,0";
								else{
									teste = sendToService(identifier, s,
											toRead.getX());
									
									//logging double click
									log = TouchRecognizer.DOUBLE_CLICK + "," + write;
									toLog.add(log);
								}

								

								// ttc.sendMessage(teste);

							}
						}
					} else {

						lastDown = timestampTPR;
						lastDownX = x;
						lastDownY = y;

						// check if touched empty
						if (!s.equals("null") && !s.equals("SCROLL")) {

							// check mode explore
							if (mode == EXPLORE) {
								toRead = CoreController
										.getNodeByIndex(CoreController
												.getNodeIndexByName(s));
								if (exploreMode == SINGLE)
									teste = "addSource,src," + voz + s
											+ ".wav,0,0,0";
								else
									teste = sendToService(identifier, s,
											toRead.getX());

								Log.d(LT, "Down: " + teste);
								
								// ttc.sendMessage(teste);

							}

							// MODE TORCH
							/*
							 * else { near = CoreController.getNearNode(x, y,
							 * 200); teste = "addSource,src" + near.length +
							 * ",man/" + s + ".wav,0,0,0"; // Log.d(LT, "Sent: "
							 * + teste); // ttc.sendMessage(teste); }
							 */
						}

						readingString.put(identifier, s);

						// MODE TORCH
						/*
						 * if (mode == TORCH) { for (int i = 0; i < near.length;
						 * i++) { near = CoreController.getNearNode(x, y, 200);
						 * String[] split = near[i].split(","); teste =
						 * "addSource,src" + (i) + ",man/" + split[0] + ".wav,"
						 * + split[1] + ",0," + split[2]; //
						 * ttc.sendMessage(teste); Log.d(LT, "Source: " +
						 * teste); } // ttc.sendMessage("play AllSources,500");
						 * }
						 */
					}
					break;
				case TouchRecognizer.MOVE:
					// Log.d(LT, "Move id:" + identifier);

					if (!s.equals("null") && !s.equals("SCROLL")) {
						if (readingString.get(identifier) == null
								|| !readingString.get(identifier).equals((s))) {
							readingString.put(identifier, s);

							if (mode == EXPLORE) {
								if (exploreMode == SINGLE)
									teste = "addSource,src," + voz + s
											+ ".wav,0,0,0";
								else
									teste = sendToService(identifier, s, refX);

								// SEND MOVE MESSAGE
								// ttc.sendMessage(teste);

								Log.d(LT, "Moved: " + teste);
								
								//logging Move
								log = TouchRecognizer.MOVE+ "," + s + "," + x
										+ "," + y + "," + identifier + ","
										+ timestamp;
								toLog.add(log);

							}
						}
					}
					break;
				case TouchRecognizer.UP:
					if (readingString.containsKey(identifier)) {
						// Log.d(LT, "UP:" + lastUp);
						String up = readingString.get(identifier);
						
						//logging up
						log = TouchRecognizer.UP + "," + up + "," + x
								+ "," + y + "," + identifier + ","
								+ timestamp;
						toLog.add(log);
						
						if (!up.equals("null")) {

							if (!up.equals(lastUp)) {
								changedUps = true;
								doubleClickRestore = doubleClickLastUp;
								doubleClickLastUp = lastUp;
								lastUp = up;
								Log.d(LT, "Set focused:" + lastUp + " "
										+ doubleClickLastUp);
								
								

							} else
								changedUps = false;

						} else
							changedUps = false;

						int index = CoreController.getNodeIndexByName(up);

						if (index != -1) {
							// SEND UP MESSAGE
							toRead = CoreController.getNodeByIndex(index);
							
							//check for mode single/multi
							if (exploreMode == SINGLE)
								teste = "addSource,src," + voz + s
										+ ".wav,0,0,0";
							else
								teste = sendToService(identifier, s,
										toRead.getX());

							focusIndex(index);
							
							//highlight 2 focus if multi mode
							if (exploreMode == MULTI && lastUpIndex != -1
									&& lastUpIndex != index)
								highlightIndex(lastUpIndex);

							lastUpIndex = index;

							Log.d(LT, "Up: " + teste + " id:" + index);

							// ttc.sendMessage(teste);

							// selectCurrent();
						}
						readingString.remove(identifier);

						if (toLog.size() > 200) {
							Log.d(LT, "write to log");
							CoreController.writeToLog((ArrayList<String>) toLog.clone(), filepath);
							toLog.clear();
						}
					}
					break;

				}
			}
		}

	}

	public String sendToService(int identifier, String s, int x) {
		String result;
		if (x < 400) {
			result = "addSourceLeftAndPlay,src" + (identifier + 1) + "," + voz
					+ s + ".wav," + "20";
		} else {
			if (x < 800)
				result = "addSourceFrontAndPlay,src" + (identifier + 1) + ","
						+ voz + s + ".wav," + "20";
			else
				result = "addSourceRightAndPlay,src" + (identifier + 1) + ","
						+ voz + s + ".wav," + "20";
		}
		return result;
	}

	@Override
	public void onTouchReceived(int type) {
		// TODO Auto-generated method stub	
	}
	
}
