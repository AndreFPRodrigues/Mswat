package mswat.caseStudy.multiScreenReader;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
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

	private static int deviceIndex;
	private TouchRecognizer tpr;
	private Hashtable<Integer, String> readingString;

	private int lastUpIndex = -1;
	private String lastUp;
	private String lastFocus;
	private String doubleClickLastUp;
	private String doubleClickRestore;
	private boolean changedUps = false;

	private long lastDown = 0;
	private long lastDownX = 0;
	private long lastDownY = 0;
 
	private static SoundTCPClient ttc = null;

	private final String MAN = "man/";
	private final String WOMAN = "woman/";
	private String voz = MAN;

	private final int EXPLORE = 0;
	private final int TORCH = 1;
	private int mode = EXPLORE;

	// single and multi touch exploration
	private final int SINGLE = 0;
	private final int MULTI = 1;
	private int exploreMode = MULTI;

	// multi touch modes
	// screen divided in 3 areas
	private final int TOUCH_3 = 0;
	// screen divided in 2 areas
	private final int TOUCH_2 = 1;
	private final int FINGER_EAR = 2;
	private final int COLUMN_X = 3;
	private final int COLUMN_SET = 4;
	private int multiTouchMode = COLUMN_SET;

	// column variable
	private int numCol = 8;
	private int offsetLeft = 170;
	private int offsetRight = 170;

	// manual column variables
	private ArrayList<String[]> manualCol;

	// finger ear variables
	private int idRight = -1;
	private int idLeft = -1;

	// detect left and right finger
	// private ArrayList<TouchEvent> touches = new ArrayList<TouchEvent>();

	private String teste;
	private ArrayList<String> sources;

	private Node toRead;

	private int refX;
	private int refY;

	private static boolean enabled;

	private String[] near;

	private static ArrayList<String> toLog = new ArrayList<String>();
	private static String filepath;

	private static int ioIndex = 0;

	// number of touches on screen
	private int numberTouches = 0;

	// touch recognizer trick when split tap recognizes a down when move
	private int lastIDTouchUp = 0;

	// max distance from the double tap to the focused node
	private final int MAX_D_TAP_DISTANCE = 300;

	// screen width
	private int SCREEN_WIDTH = 1280;

	private static int manID = -1;
	private static int womenID = -1;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals("mswat_init_screenReader")) {

			exploreMode = intent.getIntExtra("mode", MULTI);
			multiTouchMode = intent.getIntExtra("multiMode", multiTouchMode);
			String type = intent.getStringExtra("type");
			String ip = intent.getStringExtra("ip");
			// testing
			if (type.equals("icons")) { 
				multiTouchMode = COLUMN_X;
				SCREEN_WIDTH = SCREEN_WIDTH - offsetLeft - offsetRight;
 
			} else {
				if (type.equals("text")) {
					Log.d(LT, "column set");
					multiTouchMode = COLUMN_SET;
				} else {
					multiTouchMode = COLUMN_X;
					// TODO text explore method
				}
			}

			// set manual column
			createManualCollums();

			// register receiver
			ioIndex = registerIOReceiver();
			Log.d(LT, "Registei");

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
			if (ttc == null)
				ttc = new SoundTCPClient(ip);

			// screen reader state
			enabled = true;

			// make sure visual feedback is enable
			CoreController.enableHightlights();

		} else if (intent.getAction().equals("mswat_stop") && enabled) {
			Log.d(LT, "Received STOP");
			CoreController.writeToLog((ArrayList<String>) toLog.clone(),
					filepath);

			CoreController.commandIO(CoreController.SET_BLOCK, deviceIndex,
					false);

			CoreController.unregisterIOReceiver(ioIndex);

			CoreController.clearHightlights();

		}

	}

	private void createManualCollums() {
		manualCol = new ArrayList<String[]>();

		// keyboard columns
		manualCol.add(new String[] { "q", "a" });
		manualCol.add(new String[] { "w", "s", "z" });
		manualCol.add(new String[] { "e", "d", "x" });
		manualCol.add(new String[] { "r", "f", "c" });
		manualCol.add(new String[] { "t", "g", "v", "espaco" });
		manualCol.add(new String[] { "y", "h", "b" });
		manualCol.add(new String[] { "u", "j", "n" });
		manualCol.add(new String[] { "i", "k", "m" });
		manualCol.add(new String[] { "o", "l" });
		manualCol.add(new String[] { "apagar", "p", "fim" });

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
				TouchEvent te = tpr.getlastTouch();
				int x = te.getX();

				int y = te.getY() + 20;

				// setting timestamp accordingly with app log
				// int timestampTPR = tpr.getTimestamp();
				long timestampTPR = System.currentTimeMillis();

				int identifier = te.getIdentifier();
				// int identifier = tpr.popLastID();

				// Log.d(LT, "id:" + identifier);
				// + " Result:" + result);

				// Node of the touch
				s = CoreController.getNodeAt(x, y);
				// Log.d(LT, "x:" + x + " y:" + y + " node:" + s);

				// convert coord to screen coord
				refX = CoreController.convertToMilX(CoreController
						.xToScreenCoord(x) - 240) * 2;
				refY = CoreController.convertToMilY(800 - CoreController
						.yToScreenCoord(y)) / 2;

				switch (result) {
				case TouchRecognizer.DOWN:
					if (lastIDTouchUp == identifier) {
						break; 
					}

					// alternate between man and woman voice
					if (manID == -1) {
						voz = MAN;
						manID = identifier;
						// Log.d(LT, "d manID: " + manID);
					} else {
						voz = WOMAN;
						womenID = identifier;
						// Log.d(LT, "d womenID: " + womenID);
					}

					// number of current on screen touches
					numberTouches++;

					// set right or left finger
					if (multiTouchMode == FINGER_EAR) {
						if (idRight == -1)
							idRight = identifier;
						else 
							idLeft = identifier;
					}

					// Log.d(LT, "DOWN");
  
					// logging down 
					log = TouchRecognizer.DOWN + "," + s + "," + x + "," + y
							+ "," + identifier + "," + timestampTPR;
					toLog.add(log);
					// split tap
					if (exploreMode == SINGLE && numberTouches == 2) {
						splitTap(timestampTPR);

						break;
					}

					// detect double click
					int dist = (int) Math.abs(CoreController.distanceBetween(x,
							y, lastDownX, lastDownY));

					if ((timestampTPR - lastDown) < 500 && dist < 100) {
						int indexNear;

						if (doubleClickRestore != null && changedUps) {
							lastUp = doubleClickLastUp;
							doubleClickLastUp = doubleClickRestore;
						}

						if (doubleClickLastUp != null || lastUp != null) {

							String multiIndexHighlight = null;

							// check nearest focused node if multi touch
							if (exploreMode == SINGLE) {
								indexNear = CoreController
										.getNodeIndexByName(lastUp);

								// logging double click
								log = TouchRecognizer.DOUBLE_CLICK + ","
										+ lastUp + "," + timestampTPR;
								toLog.add(log);
							} else {

								indexNear = CoreController.getNearestNode(x, y,
										lastUp, doubleClickLastUp);

								// only accept double tap if near the node
								int d1;
								if ((d1 = CoreController
										.getNearestNodeDistance(x, y, lastUp,
												doubleClickLastUp)) > MAX_D_TAP_DISTANCE) {
									break;
								}

								if (indexNear < 0 && indexNear != -66) {
									indexNear = -indexNear;
									multiIndexHighlight = lastUp;
								} else
									multiIndexHighlight = doubleClickLastUp;
							}

							if (indexNear > -1) {
								//Log.d(LT, "Double click:" + lastUp + " " + doubleClickLastUp);
								// get nearest node name
								String write = CoreController
										.getNodeNameByIndex(indexNear);

								// corrigido
								clickNode(write);
								// focusIndex(indexNear);
								// SystemClock.sleep(100);
								// selectCurrent();

								if (exploreMode == MULTI
										&& multiIndexHighlight != null) {
									highlightIndex(multiIndexHighlight);
									lastUpIndex = indexNear;
								}

								if (!write.equals(lastUp)) {
									String aux = lastUp;
									lastUp = write;
									doubleClickLastUp = aux;
									doubleClickRestore = aux;
								}
								lastUp = "null";
							//	Log.d(LT, "Write: " + write + "  Standby: "
								//		+ doubleClickLastUp);

								// SEND down MESSAGE
								toRead = CoreController
										.getNodeByIndex(indexNear);

								if (exploreMode == SINGLE)
									teste = "addSourceAndPlay,src," + voz
											+ lastUp + ".wav,0,0,0";
								else {
									teste = sendToService(identifier - 1,
											write, toRead.getX());

									// logging double click
									log = TouchRecognizer.DOUBLE_CLICK + ","
											+ write + "," + timestampTPR;
									toLog.add(log);
								}

								ttc.sendMessage(teste);

							}
						}
					} else {

						lastDown = timestampTPR;
						lastDownX = x;
						lastDownY = y;
						SystemClock.sleep(200);

						// check if touched empty
						if (!s.equals("null") && !s.equals("SCROLL")) {

							// check mode explore
							if (mode == EXPLORE) {
								toRead = CoreController
										.getNodeByIndex(CoreController
												.getNodeIndexByName(s));
								if (toRead == null)
									break;
								if (exploreMode == SINGLE) {
									teste = "addSourceAndPlay,src," + voz + s
											+ ".wav,0,0,0";
									// split tap
									lastFocus = s;
								} else
									teste = sendToService(identifier, s,
											toRead.getX());

								// Log.d(LT, "Down: " + teste);

								ttc.sendMessage(teste);

							}

							// MODE TORCH
							/*
							 * else { near = CoreController.getNearNode(x, y,
							 * 200); teste = "addSource,src" + near.length +
							 * ",man/" + s + ".wav,0,0,0"; // //Log.d(LT,
							 * "Sent: " + teste); // ttc.sendMessage(teste); }
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
						 * ttc.sendMessage(teste); //Log.d(LT, "Source: " +
						 * teste); } // ttc.sendMessage("play AllSources,500");
						 * }
						 */
					}
					break;
				case TouchRecognizer.MOVE:

					if (manID == identifier) {
						voz = MAN;
						// Log.d(LT, " m manID: " + identifier );

					} else {
						// Log.d(LT, "m women: " + identifier + " id:" +
						// womenID);

						voz = WOMAN;
					}
					// //Log.d(LT, "Move id:" + identifier);

					if (!s.equals("null") && !s.equals("SCROLL")) {
						if (readingString.get(identifier) == null
								|| !readingString.get(identifier).equals((s))) {
							readingString.put(identifier, s);

							if (mode == EXPLORE) {
								if (exploreMode == SINGLE) {
									teste = "addSourceAndPlay,src," + voz + s
											+ ".wav,0,0,0";
									// split tap
									lastFocus = s;
								} else {
									if (multiTouchMode == COLUMN_X)
										teste = sendToService(identifier, s, x);

									else
										teste = sendToService(identifier, s,
												refX);

								}

								// SEND MOVE MESSAGE
								ttc.sendMessage(teste);

								// Log.d(LT, "Moved: " + teste);

								// logging Move
								log = TouchRecognizer.MOVE + "," + s + "," + x
										+ "," + y + "," + identifier + ","
										+ timestampTPR;
								toLog.add(log);

							}
						}
					}
					break;
				case TouchRecognizer.UP:

					// number of current on screen touches
					if (numberTouches > 0) {
						numberTouches--;
						lastIDTouchUp = identifier;
					}

					if (manID == identifier) {
						manID = -1;
						voz = MAN;
						// Log.d(LT, "up man");

					} else if (womenID == identifier) {
						womenID = -1;
						voz = WOMAN;
						// Log.d(LT, "up women");

					} else {
						manID = -1;
						womenID = -1;
					}

					// Log.d(LT, "Up: " +identifier);

					if (readingString.containsKey(identifier)) {
						String up = readingString.get(identifier);
						// Log.d(LT, "UP:" + up);

						// logging up
						log = TouchRecognizer.UP + "," + up + "," + x + "," + y
								+ "," + identifier + "," + timestampTPR;
						toLog.add(log);

						if (!up.equals("null")) {

							if (!up.equals(lastUp)) {
								changedUps = true;
								doubleClickRestore = doubleClickLastUp;
								doubleClickLastUp = lastUp;
								lastUp = up;
								// Log.d(LT, "Set focused:" + lastUp + " "
								// + doubleClickLastUp);

							} else
								changedUps = false;

						} else
							changedUps = false;

						int index = CoreController.getNodeIndexByName(up);

						if (index != -1) {
							// SEND UP MESSAGE
							toRead = CoreController.getNodeByIndex(index);

							// check for mode single/multi
							if (exploreMode == SINGLE)
								teste = "addSourceAndPlay,src," + voz + up
										+ ".wav,0,0,0";
							else
								teste = sendToService(identifier, up,
										toRead.getX());

							// Log.d(LT, "Up focus: " + up + " id:" + index);

							// corrigido
							focusIndex(up);

							// highlight 2 focus if multi mode
							if (exploreMode == MULTI && lastUpIndex != -1
									&& lastUpIndex != index) {

								highlightIndex(CoreController
										.getNodeNameByIndex(lastUpIndex));
							}

							lastUpIndex = index;

							// Log.d(LT, "Up: " + teste + " lastUP: " + up);
							ttc.sendMessage(teste);

							// split tap
							lastFocus = up;

							// selectCurrent();
						}
						readingString.remove(identifier);
						writeToLog();

					}
					// if mode is finger to ear clear correspondence finger - id
					clearFingerID(identifier);

					break;

				}

			}
		}

	}

	private void splitTap(long timestampTPR) {
		String log;
		// Log.d(LT, "Split touch" + lastFocus );
		int indexSplit = CoreController.getNodeIndexByName(lastFocus);

		// writing
		// corrigido
		// focusIndex(indexSplit);
		clickNode(lastFocus);
		// SystemClock.sleep(100);
		// selectCurrent();

		// logging double click
		log = TouchRecognizer.SPLIT_TAP + "," + lastFocus + "," + timestampTPR;
		toLog.add(log);

		ttc.sendMessage("addSourceAndPlay,src," + voz + lastFocus
				+ ".wav,0,0,0");

	}

	private void writeToLog() {
		if (toLog.size() > 200) {
			// Log.d(LT, "write to log");
			CoreController.writeToLog((ArrayList<String>) toLog.clone(),
					filepath);
			toLog.clear();
		}

	}

	private void clearFingerID(int identifier) {
		// finger to ear set
		if (multiTouchMode == FINGER_EAR) {
			if (idRight == identifier)
				idRight = -1;
			else
				idLeft = -1;

		}
	}

	public String sendToService(int identifier, String s, int x) {
		String result;
		switch (multiTouchMode) {
		case TOUCH_2:
			if (x < SCREEN_WIDTH / 2)
				result = "addSourceLeftAndPlay,src" + (identifier + 1) + ","
						+ MAN + s + ".wav," + "20";
			else
				result = "addSourceRightAndPlay,src" + (identifier + 1) + ","
						+ WOMAN + s + ".wav," + "20";
			break;
		case TOUCH_3:
			if (x < SCREEN_WIDTH / 3) {
				result = "addSourceLeftAndPlay,src" + (identifier + 1) + ","
						+ voz + s + ".wav," + "20";
			} else {
				if (x < (SCREEN_WIDTH / 3 * 2))
					result = "addSourceFrontAndPlay,src" + (identifier + 1)
							+ "," + voz + s + ".wav," + "20";
				else
					result = "addSourceRightAndPlay,src" + (identifier + 1)
							+ "," + voz + s + ".wav," + "20";
			}
			break;
		case FINGER_EAR:
		//	Log.d(LT, "identifier:" + identifier + " L:" + idLeft + " R:"
			//		+ idRight);
			if (identifier == idLeft)
				result = "addSourceLeftAndPlay,src" + (identifier + 1) + ","
						+ voz + s + ".wav," + "20";
			else
				result = "addSourceRightAndPlay,src" + (identifier + 1) + ","
						+ voz + s + ".wav," + "20";
			break;
		case COLUMN_X:
			result = null;
			//Log.d(LT, "r:" + s);
			int sumAux = SCREEN_WIDTH / numCol;
			int countAux = -1;
			int max = SCREEN_WIDTH + sumAux;
			// Log.d(LT, "x:" + x + " max:" + max);
			if (x > 970)
				x -= 10;
			for (int i = 0; i <= max; i += sumAux) {

				if (x - 170 < i && x > 170) {
					result = "addSourceByAngleAndPlay,src" + (identifier + 1)
							+ "," + voz + s + ".wav,"
							+ (180 - (180 / (numCol - 1) * countAux))
							+ ",left," + "20";
					 Log.d(LT, "r:" + result);
					break;
				} 
				countAux++;
			}

			break;
		case COLUMN_SET:
			numCol = manualCol.size();
			result = "";

			for (int i = 0; i < manualCol.size(); i++) {

				if (Arrays.asList(manualCol.get(i)).contains(s)) {
					int angle = 90 - (180 / (numCol - 1) * i - 90);

					String orientation;
					if (angle > 0)
						orientation = "left";
					else
						orientation = "right";
					result = "addSourceByAngleAndPlay,src" + (identifier + 1)
							+ "," + voz + s + ".wav," + angle + ","
							+ orientation + "," + "20";
					 Log.d(LT, "r:" + result);
					break;
				}
			}
			break;
		default:
			result = "";
			break;
		}

		return result;
	}

	@Override
	public void onTouchReceived(int type) {
		// TODO Auto-generated method stub
	}

}
