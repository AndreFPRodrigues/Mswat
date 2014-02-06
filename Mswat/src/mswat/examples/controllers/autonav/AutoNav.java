package mswat.examples.controllers.autonav;

import java.util.ArrayList;

import mswat.controllers.ControlInterface;
import mswat.core.CoreController;
import mswat.core.activityManager.Node;
import mswat.interfaces.ContentReceiver;
import mswat.interfaces.IOReceiver;
import mswat.interfaces.NotificationReceiver;
import mswat.touch.TPRNexusS;
import mswat.touch.TouchRecognizer;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.SystemClock;
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

	// if auto nav has been activated
	private static boolean autoNavState = false;

	private int time = 3000;
	private boolean navigate = true;
	private static NavTree navTree;
	private final int NAV_TREE_ROW = 0;
	private final int NAV_TREE_LINE = 1;
	private int navMode = NAV_TREE_LINE;

	private boolean updateNavTree = false;
	static boolean handlingCall = false;
	private static boolean answeringCall = false;
	private static boolean readingNote = false;

	private TouchRecognizer tpr = null;

	private static Context c;

	public static boolean keyboardState = false;

	private String latestNote;
	private ArrayList<String> toRead = new ArrayList<String>();

	@Override
	public void onReceive(Context context, Intent intent) {

		// Triggered when the service starts
		if (intent.getAction().equals("mswat_init")
				&& intent.getExtras().get("controller").equals("autoNav")) {
			Log.d(LT, "Auto Navigation initialised");

			autoNavState = true;

			// starts monitoring touchscreen
			int deviceIndex = CoreController.monitorTouch();

			// blocks the touch screen
			CoreController.commandIO(CoreController.SET_BLOCK, deviceIndex,
					true);

			// register receivers
			registerContentReceiver();
			registerIOReceiver();
			registerNotificationReceiver();

			// initialise touch pattern Recogniser
			tpr = CoreController.getActiveTPR();

			// initialise line/row navigation controller
			if (navTree == null)
				navTree = new NavTree();
			autoNav();

			// signal to stop service
		} else if (intent.getAction().equals("mswat_stop")) {
			Log.d(LT, "MSWaT STOP");
			navigate = false;
			CoreController.clearHightlights();

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

			} else if (intent.getExtras().getString("state").equals("OFFHOOK")) {
				answeringCall = true;
				updateNavTree = true;
				handlingCall = false;
			} else if (intent.getExtras().getString("state").equals("IDLE")) {
				answeringCall = false;
				handlingCall = false;
				CoreController.home();
			}
			c = context;

			// Handling softkeyboard
		} else {

			if (autoNavState && intent.getAction().equals("mswat_keyboard")) {
				keyboardState = intent.getBooleanExtra("status", false);
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

	@Override
	public void onUpdateContent(ArrayList<Node> content) {
		if (!answeringCall || updateNavTree) {
			updateNavTree = false;
			if (navTree == null)
				navTree = new NavTree();
			createNavList(content);
			if (handlingCall) {
				navTree.prepareCall();
				Log.d(LT, "Preparing call");
				Log.d(LT, navTree.toString());
			}
			Log.d(LT, navTree.toString());
		}
	}

	@Override
	public void onUpdateIO(int device, int type, int code, int value,
			int timestamp) {
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
		switch (type) {
		case TouchRecognizer.LONGPRESS:
			Log.d(LT, "LongPress");

			// Stops service
			CoreController.stopService();
			navigate = false;
			break;
		}
		if (handlingCall && !answeringCall) {

			navTree.pause = true;
			answeringCall = true;
			handlingCall = false;
			CallManagement.answer(c);

		} else
		// Terminate call in nexusS 4.1.2 the terminate button is the first
		// index
		if (answeringCall) {
			Log.d(LT, "DESLIGUES");
			focusIndex(0);
			selectCurrent();
		} else

		// unpause autonavigation
		if (navTree.pause) {
			Log.d(LT, "Unpause");
			// answeringCall=false;
			updateNavTree = true;
			navTree.unpause = true;
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
				navTree.nextNode();

				if (navTree.lineSize() == 1
						|| (navTree.getCurrentNode() != null && navTree
								.getCurrentNode().getName().equals("SCROLL"))) {

					if (navTree.getCurrentNode() != null
							&& navTree.getCurrentNode().getName()
									.equals("Terminar")) {
						navTree.resetColumnIndex();
						answeringCall = false;
						updateNavTree = true;
						CoreController.home();
					} else if (navTree.getCurrentNode() != null
							&& navTree.getCurrentNode().getName()
									.equals("voltar")) {
						// Log.d(LT, CoreController.back() + " boolean");

						if (!CoreController.back())
							CoreController.home();

					} else {

						focusIndex(navTree.getCurrentIndex());
						selectCurrent();
					}
				} else {
					navMode = NAV_TREE_ROW;
				}
				navTree.resetColumnIndex();

			} else {
				navMode = NAV_TREE_LINE;

				if (navTree.getCurrentNode() != null
						&& navTree.getCurrentNode().getName()
								.equals("Terminar")) {

					answeringCall = false;
					updateNavTree = true;
					CoreController.home();
				} else {

					focusIndex(navTree.getCurrentIndex());
					selectCurrent();
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

				Node n;
				CoreController.home();

				while (navigate) {
					do {
						// TODO maybe handler
						SystemClock.sleep(time);
						// Log.d(LT, "Pause" + navTree.pause + "  Unpause:"
						// +navTree.unpause + " keyboard:" + keyboardState);
					} while ((navTree.pause && !navTree.unpause)
							|| keyboardState || readingNote);
					if (navTree.available()) {

						switch (navMode) {
						case NAV_TREE_LINE:
							n = navTree.nextLineStart();

							if (n != null) {

								if (n.getName().equals("SCROLL")) {

									CoreController.textToSpeech("Deslize");
									CoreController.hightlight(0, 0,
											(float) 0.6,
											(int) CoreController.S_WIDTH,
											(int) CoreController.S_HEIGHT,
											Color.LTGRAY);
								} else {

									CoreController.hightlight(
											n.getBounds().top - 40, 0,
											(float) 0.6,
											(int) CoreController.S_WIDTH, n
													.getBounds().height(),
											Color.BLUE);
									Log.d(LT, "READ linha : " + n.getName());

									if (navTree.lineSize() == 1) {
										CoreController.textToSpeech(n.getName());
									} else
										CoreController.textToSpeech(n.getName()
												+ " Linha");
								}
							}
							break;
						case NAV_TREE_ROW:
							n = navTree.nextNode();
							if (n != null) {
								CoreController.hightlight(
										n.getBounds().top - 40,
										n.getBounds().left, (float) 0.6,
										(int) n.getBounds().width(), n
												.getBounds().height(),
										Color.CYAN);
								// Log.d(LT, "READ: " + n.getName());

								boolean waitFor = CoreController
										.waitFortextToSpeech(n.getName());

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
		handleTouch(type);
	}

}
