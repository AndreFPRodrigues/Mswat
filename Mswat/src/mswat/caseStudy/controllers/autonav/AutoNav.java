package mswat.caseStudy.controllers.autonav;

import java.util.ArrayList;

import mswat.controllers.ControlInterface;
import mswat.core.CoreController;
import mswat.core.activityManager.Node;
import mswat.interfaces.ContentReceiver;
import mswat.interfaces.IOReceiver;
import mswat.interfaces.NotificationReceiver;
import mswat.touch.TPRNexusS;
import mswat.touch.TouchRecognizer;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Auto navigation using touchscreen as a switch Navigation is done firstly
 * across lines and then across the row
 * 
 * @author Andre Rodrigues
 * 
 */
public class AutoNav extends ControlInterface implements IOReceiver,
		ContentReceiver, NotificationReceiver {

	private final String LT = "AutoNav";

	// Touchscreen device index
	private static int deviceIndex;
	// index of the keypad device (nexus s)
	private final int KEYPAD = 3;

	// if auto nav has been activated
	private static boolean autoNavState = false;

	private int time = 3000;
	private boolean navigate = true;
	private static NavTree navTree;
	private static final int NAV_TREE_ROW = 0;
	private static final int NAV_TREE_LINE = 1;
	private static int navMode = NAV_TREE_LINE;

	private static boolean updateNavTree = false;
	static boolean handlingCall = false;
	private static boolean answeringCall = false;
	private static boolean readingNote = false;

	private TouchRecognizer tpr = null;

	private static Context c;

	public static boolean keyboardState = false;

	private String latestNote;
	private ArrayList<String> toRead = new ArrayList<String>();
	private static boolean autonav = false;
	private static boolean firstAutoNav = true;

	private static ArrayList<Node> toFilter = new ArrayList<Node>();
	private static String[] filter;

	private static Node currentNode;

	// variables to handle turning of and on the autonav through volume down
	private final int VOLUME_DOWN = 114;
	static boolean pause = false;

	// call contact
	private static String contact = null;

	@Override
	public void onReceive(Context context, Intent intent) {

		// Triggered when the service starts
		if (intent.getAction().equals("mswat_init")
				&& intent.getExtras().get("controller").equals("autoNav")) {

			c = context;

			// register receivers
			registerContentReceiver();

		} else if (intent.getAction().equals("mswat_autoNavStart")) {
			Log.d(LT, "Auto Navigation initialised");
			// starts monitoring touchscreen
			deviceIndex = CoreController.monitorTouch();
			CoreController.commandIO(CoreController.MONITOR_DEV, KEYPAD, true);

			filter = intent.getStringArrayExtra("filter");

			// blocks the touch screen
			CoreController.commandIO(CoreController.SET_BLOCK, deviceIndex,
					true);

			autoNavState = true;

			registerIOReceiver();
			registerNotificationReceiver();

			// initialise touch pattern Recogniser
			tpr = CoreController.getActiveTPR();

			// initialise line/row navigation controller
			if (navTree == null)
				navTree = new NavTree();
			autoNav();
			CoreController.home();
			// signal to stop service
		} else if (intent.getAction().equals("mswat_stop") && autoNavState) {
			Log.d(LT, "MSWaT STOP");
			navigate = false;
			// CoreController.clearHightlights();

			// Handling calls
		} else if (autoNavState
				&& intent.getAction().equals(
						"android.intent.action.PHONE_STATE")) {
			Log.d(LT, "call: " + intent.getExtras().getString("state"));

			if (intent.getExtras().getString("state").equals("RINGING")) {
				if (keyboardState && !handlingCall)
					CoreController.stopKeyboard();
				handlingCall = true;
				keyboardState = false;
				Bundle bundle = intent.getExtras();
				contact = getContactName(
						context,
						bundle.getString(TelephonyManager.EXTRA_INCOMING_NUMBER));
			} else if (intent.getExtras().getString("state").equals("OFFHOOK")) {
				answeringCall = true;
				updateNavTree = true;
				handlingCall = false;
				AudioManager audioManager = (AudioManager) context
						.getSystemService(Context.AUDIO_SERVICE);
				audioManager.setMode(AudioManager.MODE_IN_CALL);
				audioManager.setSpeakerphoneOn(true);

			} else if (intent.getExtras().getString("state").equals("IDLE")) {
				AudioManager audioManager = (AudioManager) context
						.getSystemService(Context.AUDIO_SERVICE);
				audioManager.setMode(AudioManager.MODE_NORMAL);
				audioManager.setSpeakerphoneOn(true);
				answeringCall = false;
				handlingCall = false;
				CoreController.home();
				if (!autonav)
					autoNav();
			}

			// Handling softkeyboard
		} else {

			if (autoNavState && intent.getAction().equals("mswat_keyboard")) {
				keyboardState = intent.getBooleanExtra("status", false);
				if (!keyboardState && !autonav) {
					autoNav();
				}
				Log.d(LT, "KEYBOARD ENABLE " + keyboardState);

			}

		}

	}

	@Override
	public int registerContentReceiver() {
		return CoreController.registerContentReceiver(this);

	}

	@Override
	public int registerIOReceiver() {
		return CoreController.registerIOReceiver(this);
	}

	@Override
	public int registerNotificationReceiver() {
		return CoreController.registerNotificationReceiver(this);
	}

	@Override
	public void onNotification(String notification) {
		Log.d(LT, "Notificacao " + notification);
		if (latestNote != null && latestNote.equals(notification))
			return;
		readingNote = true;
		readingNote = CoreController.waitFortextToSpeech(notification);
		latestNote = notification;
		if (navTree.pause && !navTree.unpause) {
			toRead.add(latestNote);
		}
	}

	private static boolean firstTime = true;

	@Override
	public void onUpdateContent(ArrayList<Node> content) {

		content = applyFilter(content);

		if (!answeringCall || updateNavTree) {
			updateNavTree = false;
			if (navTree == null)
				navTree = new NavTree();
			createNavList(content);
			if (handlingCall) {
				navTree.prepareCall(contact);
				Log.d(LT, "Preparing call");
				Log.d(LT, navTree.toString());
			}

			Log.d(LT, navTree.toString());
		}

		// first calibration
		if (firstTime) {
			toFilter = (ArrayList<Node>) content.clone();
			Intent i = new Intent(c, AutoNavSetup.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.putExtra("values", navTree.getDescriptions());
			c.startActivity(i);
			firstTime = false;
		}

	}

	/**
	 * Check if is list to be filtered Filters unwanted options
	 * 
	 * @param content
	 * @return
	 */
	private ArrayList<Node> applyFilter(ArrayList<Node> content) {

		if (content.size() == toFilter.size() && filter != null
				&& filter.length > 0) {

			for (int i = 0; i < content.size(); i++) {
				if (!content.get(i).getName().equals(toFilter.get(i).getName())) {
					return content;
				}
			}
			ArrayList<Node> an = new ArrayList<Node>();
			int aux = 0;
			for (Node n : content) {

				if (n.getName().equals(filter[aux])) {
					an.add(n);
					aux++;
				}
				if (aux > filter.length - 1) {

					return an;
				}
			}

			return an;
		}
		return content;

	}

	@Override
	public void onUpdateIO(int device, int type, int code, int value,
			int timestamp) {
		if (device == KEYPAD) {
			if (code == VOLUME_DOWN && value == 0) {
				if (keyboardState) {
					Log.d(LT, "unpause");
					CoreController.commandIO(CoreController.SET_BLOCK,
							deviceIndex, true);
					autoNav();
				} else {
					Log.d(LT, "pause");
					CoreController.commandIO(CoreController.SET_BLOCK,
							deviceIndex, false);
					clearHighlights();
				}
				pause = !pause;
				keyboardState = !keyboardState;
			}
			return;
		}
		if (tpr == null) {
			tpr = CoreController.getActiveTPR();
		}
		// if keyboard is enabled ignores IO events
		if (keyboardState) {
			return;
		}
		// Debugg purposes stop the service
		int touchType;

		if ((touchType = tpr.identifyOnRelease(type, code, value, timestamp)) != -1) {
			handleTouch(touchType);

		}

	}

	private void handleTouch(int type) {
		// if keyboard is enabled ignores IO events
		if (keyboardState) {
			Log.d(LT, "return ;");

			return;
		}
		switch (type) {
		case TouchRecognizer.LONGPRESS:
			Log.d(LT, "LongPress");

			// Stops service
			CoreController.stopService();
			navigate = false;
			break;
		}
		Log.d(LT, "Handling touch");

		if (handlingCall && !answeringCall) {
			Log.d(LT, "CALL MANAGEMENt");

			navTree.pause = true;
			answeringCall = true;
			handlingCall = false;
			CallManagement.answer(c);

		} else
		// Terminate call in nexusS 4.1.2 the terminate button is the first
		// index
		if (answeringCall) {
			Log.d(LT, "TERMINATE CALL");
			// focusIndex(0);
			clickNode("Terminar");
		} else

		// unpause autonavigation
		if (navTree.pause) {
			Log.d(LT, "UNPAUSE");
			// answeringCall=false;
			updateNavTree = true;
			navTree.unpause = true;
			// testing pause
			if (!autonav)
				autoNav();
			if (toRead.size() > 0) {
				onNotification("Notificações");
				for (int i = 0; i < toRead.size(); i++) {
					onNotification(toRead.get(i));
				}
				toRead.clear();
			}
		} else {
			// either change navigation mode or select current focused
			// node
			if (navMode == NAV_TREE_LINE) {
				Log.d(LT, "Handling touch nav tree line");

				if (navTree.lineSize() == 1
						|| (navTree.getCurrentNode() != null && navTree
								.getCurrentNode().getName().equals("SCROLL"))) {
					Log.d(LT, "Nav Tree size of list = 1");

					if (navTree.getCurrentNode() != null
							&& navTree.getCurrentNode().getName()
									.equals("Terminar")) {
						Log.d(LT, "Node terminate Call");

						navTree.resetColumnIndex();
						answeringCall = false;
						updateNavTree = true;
						CoreController.home();

						// if the row is -1 and it is voltar
					} else if (navTree.checkBack()) {

						// Log.d(LT, CoreController.back() + " boolean");
						Log.d(LT, "Back");

						if (!back()) {
							CoreController.home();
						}

					} else {
						Log.d(LT, "Click select text" + currentNode.getName());

						clickNode(currentNode.getName());

						/*
						 * focusIndex(navTree.getCurrentNode().getName());
						 * selectCurrent();
						 */
					}
				} else {
					Log.d(LT, "Change to row scaning");

					navMode = NAV_TREE_ROW;
				}
				navTree.resetColumnIndex();

			} else {
				Log.d(LT, "Handling row Touch");

				navMode = NAV_TREE_LINE;

				if (navTree.getCurrentNode() != null
						&& navTree.getCurrentNode().getName()
								.equals("Terminar")) {

					answeringCall = false;
					updateNavTree = true;
					CoreController.home();
				} else {
					// Log.d(LT, "Handling touch focus select");
					if (currentNode != null)
						clickNode(currentNode.getName());
					/*
					 * focusIndex(navTree.getCurrentNode().getName());
					 * selectCurrent();
					 */
				}
			}
		}
	}

	/**
	 * Update auto nav controller
	 * 
	 */
	private void createNavList(ArrayList<Node> arrayList) {

		navTree.navTreeUpdate(arrayList);
		if (navTree.pause && !keyboardState)
			navTree.unpause = true;
	}

	/**
	 * Auto navigation thread responsible to automatically cycle through
	 * lines/nodes and highlight them
	 */
	private void autoNav() {
		Thread auto = new Thread(new Runnable() {
			public void run() {
				autonav = true;
				updateNavTree = true;
				Log.d(LT, "INIT AUTONAV Thread");

				if (firstAutoNav) {
					CoreController.home();
					firstAutoNav = false;
				}

				outer: while (navigate) {
					do {
						// TODO maybe handler
						SystemClock.sleep(time);

						if ((navTree.pause && !navTree.unpause)
								|| keyboardState || answeringCall) {

							Log.d(LT, "BOPN");
							autonav = false;
							break outer;
						}

					} while (keyboardState || readingNote);
					if (navTree.available()) {

						switch (navMode) {
						case NAV_TREE_LINE:
							currentNode = navTree.nextLineStart();

							if (currentNode != null) {

								if (currentNode.getName().equals("SCROLL")) {

									CoreController.textToSpeech("Deslize");
									CoreController.hightlight(0, 0,
											(float) 0.6,
											(int) CoreController.S_WIDTH,
											(int) CoreController.S_HEIGHT,
											Color.LTGRAY);
								} else {

									CoreController.hightlight(
											currentNode.getBounds().top - 40,
											0, (float) 0.6,
											(int) CoreController.S_WIDTH,
											currentNode.getBounds().height(),
											Color.BLUE);
									// Log.d(LT, "READ linha : " + n.getName());

									if (navTree.lineSize() == 1) {
										CoreController.textToSpeech(currentNode
												.getName());
									} else
										CoreController.textToSpeech(currentNode
												.getName() + " Linha");
								}
							}
							break;
						case NAV_TREE_ROW:
							currentNode = navTree.nextNode();
							if (currentNode != null) {
								CoreController.hightlight(currentNode
										.getBounds().top - 40, currentNode
										.getBounds().left, (float) 0.6,
										(int) currentNode.getBounds().width(),
										currentNode.getBounds().height(),
										Color.CYAN);
								// Log.d(LT, "READ: " + n.getName());

								boolean waitFor = CoreController
										.waitFortextToSpeech(currentNode
												.getName());

							} else {
								navMode = NAV_TREE_LINE;
							}
							break;
						}
					}
				}
			}
		});

		auto.start();

	}

	@Override
	public void onTouchReceived(int type) {
		Log.d(LT, "Received Touch");
		handleTouch(type);
	}

	@Override
	public int getType() {
		return INTERACTIVE_CHILDREN;
	}

	public static String getContactName(Context context, String phoneNumber) {
		ContentResolver cr = context.getContentResolver();
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(phoneNumber));
		Cursor cursor = cr.query(uri,
				new String[] { PhoneLookup.DISPLAY_NAME }, null, null, null);
		if (cursor == null) {
			return null;
		}
		String contactName = null;
		if (cursor.moveToFirst()) {
			contactName = cursor.getString(cursor
					.getColumnIndex(PhoneLookup.DISPLAY_NAME));
		}

		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}

		return contactName;
	}

}
