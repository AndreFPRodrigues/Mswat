package mswat.examples.controllers.autonav;

import java.util.ArrayList;

import mswat.controllers.ControlInterface;
import mswat.core.CoreController;
import mswat.core.activityManager.Node;
import mswat.interfaces.ContentReceiver;
import mswat.interfaces.IOReceiver;
import mswat.interfaces.NotificationReceiver;
import mswat.touch.TouchPatternRecognizer;
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
		ContentReceiver, NotificationReceiver  {

	private final String LT = "AutoNav";
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

	private TouchPatternRecognizer tpr;

	private static Context c;

	public static boolean keyboardState = false;
	private static AutoNavKeyboard ank;

	@Override
	public void onReceive(Context context, Intent intent) {

		// Triggered when the service starts
		if (intent.getAction().equals("mswat_init")
				&& intent.getExtras().get("controller").equals("autoNav")) {
			Log.d(LT, "Auto Navigation initialised");

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
			tpr = new TouchPatternRecognizer();

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
		} else if (intent.getAction().equals(
				"android.intent.action.PHONE_STATE")) {
			Log.d(LT, "call: " + intent.getExtras().getString("state"));

			if (intent.getExtras().getString("state").equals("RINGING")) {
				handlingCall = true;
				keyboardState = false;


			} else if (intent.getExtras().getString("state").equals("OFFHOOK")) {
				answeringCall = true;
				updateNavTree = true;
				handlingCall = false;
			} else if (intent.getExtras().getString("state").equals("IDLE")) {
				answeringCall = false;
				CoreController.home();
			}
			c = context;

			// Handling softkeyboard
		} else {
			if (intent.getAction().equals("mswat_initKeyboard")) {
				keyboardState = true;
				Log.d(LT, "KEYBOARD ENABLE ");

				 ank = new AutoNavKeyboard(context);
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
		Log.d (LT, "Notificacao " + notification);
		readingNote=true;
		readingNote = CoreController.waitFortextToSpeech(notification);
		
	}
	@Override
	public void onUpdateContent(ArrayList<Node> content) {
		if (!answeringCall || updateNavTree) {
			updateNavTree = false;
			if (navTree == null)
				navTree = new NavTree();
			createNavList(content);
			if (handlingCall)
				navTree.prepareCall();
			// Log.d(LT, navTree.toString());
		}
	}

	@Override
	public void onUpdateIO(int device, int type, int code, int value,
			int timestamp) {
		// if keyboard is enabled ignores IO events
	
			// Debugg purposes stop the service
			int touchType;
			if ((touchType = tpr
					.identifyOnRelease(type, code, value, timestamp)) != -1) {

				//handle touch while in keyboard mode
				if (keyboardState) {
					ank.touch(touchType);
					return;
				}
				Log.d(LT, "IO update");

				
				switch (touchType) {
				case TouchPatternRecognizer.LONGPRESS:
					// Stops service
					CoreController.stopService();
					 navigate = false;
					break;
				}
				if (handlingCall && navTree.getCurrentNode() != null) {
					if (navTree.getCurrentNode().getName().equals("atender")) {
						navTree.pause = true;
						answeringCall = true;
						handlingCall = false;
						CallManagement.answer(c);

					}
				} else

				// unpause autonavigation
				if (navTree.pause) {
					Log.d(LT, "Unpause");

					handlingCall = false;
					// answeringCall=false;
					updateNavTree = true;
					navTree.unpause = true;
				} else {
					// either change navigation mode or select current focused
					// node
					if (navMode == NAV_TREE_LINE) {
						
						if(navTree.lineSize()==1){
						if (navTree.getCurrentNode() != null
								&& navTree.getCurrentNode().getName()
										.equals("Terminar")) {
							answeringCall = false;
							updateNavTree = true;
							CoreController.home();
						}
						focusIndex(navTree.getCurrentIndex());
						selectCurrent();
						}else 						navMode = NAV_TREE_ROW;

						
					} else {
						navMode = NAV_TREE_LINE;

						if (navTree.getCurrentNode() != null
								&& navTree.getCurrentNode().getName()
										.equals("Terminar")) {
							answeringCall = false;
							updateNavTree = true;
							CoreController.home();
						}
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
						//TODO maybe handler
						SystemClock.sleep(time);
						 //Log.d(LT, "Pause" + navTree.pause + "  Unpause:" +navTree.unpause + " keyboard:" + keyboardState);
					} while ((navTree.pause && !navTree.unpause) || keyboardState || readingNote);
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
									if(navTree.lineSize()==1){
										CoreController.textToSpeech(n.getName());
									}else
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



	
}
