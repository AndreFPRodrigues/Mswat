package mswat.core.activityManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import mswat.controllers.WifiControl;
import mswat.core.CoreController;
import mswat.core.calibration.CalibrationActivity;
import mswat.core.feedback.FeedBack;
import mswat.core.ioManager.Monitor;
import mswat.core.macro.MacroManagment;
import mswat.core.macro.RunMacro;
import mswat.core.macro.Touch;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.webkit.WebView.FindListener;
import android.widget.EditText;

public class HierarchicalService extends AccessibilityService {

	// Parent node to the current screen content
	AccessibilityNodeInfo currentParent = null;

	private final String LT = "Hier";

	// list with the current screen nodes
	private ArrayList<Node> nodeList = new ArrayList<Node>();
	private ArrayList<Node> checkList = new ArrayList<Node>();

	private EditText editText;

	// node that represents the possibility of sliding
	private ArrayList<Node> scrollNodes = new ArrayList<Node>();
	// list of install apps icons
	private ArrayList<AppIcon> appIcons = new ArrayList<AppIcon>();

	// flag to register keystrokes
	private boolean logAtTouch = false;

	private SharedPreferences sharedPref;
	private ServicePreferences servPref;
	private Monitor monitor;
	private NodeListController nlc;
	private FeedBack fb;

	private boolean broadcastContent = false;

	private static int identifier = 0;

	private static boolean creatingMacro = false;
	private boolean runningMacro = false;
	private int macroMode = MacroManagment.NAV_MACRO;
	private Stack<String> command;
	private final int THRESHOLD_GUARD = 50;
	private int runMacroMode = MacroManagment.NAV_MACRO;

	/**
	 * Triggers whenever happens an event (changeWindow, focus, slide) Updates
	 * the current top parent of the screen contents
	 */
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		// Log.d(LT, event.toString());
		if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED)
			CoreController.updateNotificationReceivers("" + event.getText());
		else {
			// Log.d(LT, event.getEventType() + "");

			if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
				if (event.getClassName().toString().contains("EditText")
						|| event.getClassName().toString()
								.contains("MultiAutoCompleteTextView")) {
					CoreController.startKeyboard();
				}

				if (creatingMacro && macroMode == MacroManagment.NAV_MACRO)
					handleMacroCreation(event);

			} else {

				AccessibilityNodeInfo source = event.getSource();
				if (source == null) {
					return;
				}

				source = getRootParent(source);
				if (source == null) {
					return;
				}
				currentParent = source;

				// register keystrokes
				if (AccessibilityEvent.eventTypeToString(event.getEventType())
						.contains("TEXT")) {
					if (logAtTouch) {
						if (event.getRemovedCount() > event.getAddedCount())
							monitor.registerKeystroke("BackSpace");
						else {

							if (event.getRemovedCount() != event
									.getAddedCount()) {
								// When the before text is a space it needs this
								// to
								// properly
								// detect the backspace
								// Bug a string a char follow by a space "t "
								// when
								// using
								// backspace it detects "t" instead of backspace
								if ((event.getText().size() - 2) == event
										.getBeforeText().length()
										|| (event.getAddedCount() - event
												.getRemovedCount()) > 1)
									monitor.registerKeystroke("BackSpace");
								else {
									String keypressed = event.getText()
											.toString();
									keypressed = ""
											+ keypressed.charAt(keypressed
													.length() - 2);
									if (keypressed.equals(" "))
										keypressed = "Space";
									monitor.registerKeystroke(keypressed);
								}
							}

						}
					}
					CoreController.updateKeyboardUI();

				}
				// update the current content list
				checkList.clear();
				scrollNodes.clear();
				identifier = 0;
				listUpdate(currentParent);
				if (scrollNodes.size() > 0) {

					checkList.addAll(scrollNodes);
				}

				if (checkUpdate()) {
					Log.d(LT, "Updated Tree");
					nlc.updateList(nodeList);
					if (broadcastContent)
						CoreController.nodeMessages(nodeList.toString());

					// Send content update to the receivers
					CoreController.updateContentReceivers(nodeList);
					printViewItens((ArrayList<Node>) nodeList.clone());

					// Macro step
					if (runningMacro
							&& runMacroMode == MacroManagment.NAV_MACRO)
						checkStep();
				}

			}
		}
	}

	/**
	 * Get root parent from node source
	 * 
	 * @param source
	 * @return
	 */
	private AccessibilityNodeInfo getRootParent(AccessibilityNodeInfo source) {
		AccessibilityNodeInfo current = source;
		while (current.getParent() != null) {
			AccessibilityNodeInfo oldCurrent = current;
			current = current.getParent();
			oldCurrent.recycle();
		}
		return current;
	}

	/**
	 * Rebuild list with the current content of the view
	 * 
	 * @param source
	 */
	private synchronized void listUpdate(AccessibilityNodeInfo source) {
		AccessibilityNodeInfo child;
		Rect outBounds = new Rect();

		Node n;
		boolean addScroll = false;
		for (int i = 0; i < source.getChildCount(); i++) {
			child = source.getChild(i);
			// Log.d(LT, child.toString());
			if (child != null) {

				if (child.isScrollable() && !addScroll) {
					source.getBoundsInScreen(outBounds);
					addScroll = true;
					scrollNodes.add(new Node((String) "SCROLL", outBounds,
							child));
				}
				String text;

				if (child.getChildCount() == 0) {

					/*
					 * if (child.getText() != null ||
					 * child.getContentDescription() != null /* &&
					 * (child.isClickable() || source.isClickable()) ) {
					 */
					outBounds = new Rect();
					if (source.getClassName().toString().contains("Linear")) {
						source.getBoundsInScreen(outBounds);

					} else
						child.getBoundsInScreen(outBounds);
					if ((outBounds.centerX() > 0 && outBounds.centerY() > 0)) {
						if ((text = getText(child)) != null) {
							n = new Node(text, outBounds, child);

							n.setIcon(checkIconMatch(text));

						} else {
							n = new Node(child.getClassName().toString()
									+ identifier, outBounds, child);
							identifier++;

						}

						checkList.add(n);
					}
					// }
				} else {
					child.getBoundsInScreen(outBounds);
					if ((text = getText(child)) != null) {

						n = new Node(text, outBounds, child);
					} else {

						n = new Node(child.getClassName().toString()
								+ identifier, outBounds, child);
						identifier++;

					}
					checkList.add(n);

					listUpdate(child);
				}
			}
		}
	}

	/**
	 * Check if the clickable node has a correspondent icon
	 * 
	 * @param charSequence
	 * @return
	 */
	int checkIconMatch(CharSequence charSequence) {
		for (int i = 0; i < appIcons.size(); i++) {

			if (appIcons.get(i).getDescription().contentEquals(charSequence))
				return i;
		}
		return -1;
	}

	/**
	 * Print to log current view list node
	 */
	protected void printViewItens(ArrayList<Node> listCurrentNodes) {
		int size = listCurrentNodes.size();
		Log.d(LT,
				"---------------------List State------------------------------");
		if (size > 0)
			for (int i = 0; i < size; i++) {
				Log.d(LT, listCurrentNodes.get(i).toString());
			}

	}

	@Override
	public void onInterrupt() {
		stopService();
	}

	@Override
	public void onDestroy() {
		stopService();
	}

	public void stopService() {
		monitor.stop();
		FeedBack.stop();
		this.stopSelf();
	}

	@Override
	public boolean onKeyEvent(KeyEvent event) {
		Log.d(LT, "got one");
		return true;
	}

	/**
	 * Initialise NodeListController Initialise Monitor Initialise
	 * CoreController Initialise Feedback
	 */
	@Override
	public void onServiceConnected() {
		getServiceInfo().flags = AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
		// getServiceInfo().flags =
		// AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;

		Log.d(LT, "CONNECTED");

		// Creating the install app icons list
		PackageManager pm = getPackageManager();
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);

		for (ResolveInfo info : apps) {
			String title = info.loadLabel(pm).toString();
			if (title.length() == 0) {
				title = info.activityInfo.name.toString();
			}

			AppIcon app = new AppIcon(info.loadIcon(pm), title);
			appIcons.add(app);
		}

		WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		@SuppressWarnings("deprecation")
		WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.FILL_PARENT,
				WindowManager.LayoutParams.FILL_PARENT,
				WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
						| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
				PixelFormat.TRANSLUCENT);

		// shared preferences
		sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		servPref = new ServicePreferences();
		logAtTouch = sharedPref.getBoolean(servPref.LOG_AT_TOUCH, false);

		// Log touch events
		boolean logIO = sharedPref.getBoolean(servPref.LOG_IO, false);

		// Log navigation using the framework control interface
		boolean logNav = sharedPref.getBoolean(servPref.LOG_NAV, false);

		boolean audioFeedBack = sharedPref.getBoolean(servPref.AUDIO, false);
		boolean visualFeedBack = sharedPref.getBoolean(servPref.VISUAL, false);

		boolean calibration = sharedPref
				.getBoolean(servPref.CALIBRATION, false);

		boolean broadcastIO = sharedPref.getBoolean(servPref.BROADCAST_IO,
				false);
		broadcastContent = sharedPref.getBoolean(servPref.BROADCAST_CONTENT,
				false);

		boolean wifi = sharedPref.getBoolean(servPref.WIFI, false);
		int touchIndex = sharedPref.getInt(servPref.TOUCH_INDEX, -1);

		if ((sharedPref.getInt("s_width", (int) CoreController.S_WIDTH)) != 0) {
			CoreController.S_WIDTH = sharedPref.getInt("s_width",
					(int) CoreController.S_WIDTH);
			CoreController.S_HEIGHT = sharedPref.getInt("s_height",
					(int) CoreController.S_HEIGHT);

		} else {
			CoreController.S_WIDTH = 1024;
			CoreController.S_HEIGHT = 960;
		}

		String controller = sharedPref.getString(servPref.CONTROLLER, "null");

		String keyboard = sharedPref.getString(servPref.KEYBOARD, "null");

		// Start tpr
		String tpr = sharedPref.getString(servPref.TPR, "null");

		Intent intent = new Intent();
		intent.setAction("mswat_tpr");
		intent.putExtra("touchRecog", tpr);
		sendBroadcast(intent);

		// initialise feedback
		fb = new FeedBack(this, windowManager, params);

		nlc = new NodeListController(this, audioFeedBack, visualFeedBack);

		// initialise monitor
		monitor = new Monitor(this, broadcastIO, touchIndex);

		// initialise coreController
		CoreController cc = new CoreController(nlc, monitor, this, controller,
				calibration, logIO, logNav, logAtTouch, keyboard, tpr);

		// initialise wifi controller
		if (wifi) {
			WifiControl wc = new WifiControl();
		}

		// starts calibration activity
		if (calibration) {
			cc.setCalibration();
			Intent i = new Intent(getBaseContext(), CalibrationActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			getApplication().startActivity(i);

		} else {
			// Goes to home screen (triggers accessibility event to update
			// current
			// content)
			home();
		}

	}

	/**
	 * 
	 * @return current content parent
	 */
	public AccessibilityNodeInfo getContentParent() {
		return currentParent;
	}

	/**
	 * 
	 * @return current content list
	 */
	public synchronized ArrayList<Node> getDescribedContent() {
		return checkList;
	}

	/**
	 * Check if the current content list is the latest if not it updates it
	 * 
	 * @param checkList
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean checkUpdate() {
		if (checkList.size() != nodeList.size()) {
			nodeList.clear();
			nodeList = (ArrayList<Node>) checkList.clone();
			// checkList.clear();
			return true;
		} else {
			for (int i = 0; i < checkList.size(); i++) {
				if (!checkList.get(i).getName()
						.equals(nodeList.get(i).getName())) {
					nodeList.clear();
					nodeList = (ArrayList<Node>) checkList.clone();
					// checkList.clear();

					return true;
				}
			}
		}

		return false;
	}

	// go to home screen
	public boolean home() {
		return performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);

	}

	// go to home screen
	public boolean back() {
		return performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
	}

	/**
	 * Register screen size to shared preferences
	 * 
	 * @param width
	 * @param height
	 */
	public void storeScreenSize(int width, int height) {
		sharedPref.edit().putInt("s_width", width).commit();
		sharedPref.edit().putInt("s_height", height).commit();

	}

	/**
	 * Register the touch device index to the shared preferences
	 * 
	 * @param index
	 */
	public void storeTouchIndex(int index) {
		sharedPref.edit().putInt(servPref.TOUCH_INDEX, index).commit();
	}

	public void lockedScreen(boolean state) {
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
				| PowerManager.ACQUIRE_CAUSES_WAKEUP
				| PowerManager.ON_AFTER_RELEASE, "INFO");
		wl.acquire();

		KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
		KeyguardLock kl = km.newKeyguardLock("name");
		kl.disableKeyguard();
		if (state)
			kl.reenableKeyguard();
	}

	/**
	 * Gets the node text either getText() or contentDescription
	 * 
	 * @param src
	 * @return node text/description null if it doesnt have
	 */
	private String getText(AccessibilityNodeInfo src) {
		String text = null;

		if (src.getText() != null || src.getContentDescription() != null) {
			if (src.getText() != null)
				text = src.getText().toString();
			else
				text = src.getContentDescription().toString();
		}
		return text;
	}

	/**
	 * If creating macro is active it sends the text of the clicked node
	 * 
	 * @param event
	 * @param src
	 */
	private void handleMacroCreation(AccessibilityEvent event) {

		AccessibilityNodeInfo src = event.getSource();
		// Log.d(LT, "handling macro: " +src.toString());
		if (src != null) {
			String text;
			if ((text = getText(src)) != null)
				CoreController.addMacroStep(text);
			else {
				int numchilds = src.getChildCount();
				// Log.d(LT,"childs: " + numchilds);
				for (int i = 0; i < numchilds; i++) {
					if ((text = getText(src.getChild(i))) != null) {
						CoreController.addMacroStep(text);
						return;
					}
				}
				src = src.getParent();
				numchilds = src.getChildCount();
				// Log.d(LT,"childs: " + numchilds);
				for (int i = 0; i < numchilds; i++) {
					if ((text = getText(src.getChild(i))) != null) {
						CoreController.addMacroStep(text);
						return;
					}
				}
			}
		}

	}

	/**
	 * Start macro recording
	 * 
	 * @param createMacro
	 */
	public void setCreateMacro(boolean createMacro) {
		//Log.d("Macro", "set create mode:" + createMacro);
		creatingMacro = createMacro;
		if(creatingMacro)
			setMacroMode(MacroManagment.NAV_MACRO); 

	}

	/**
	 * Change recording macro mode touch/navigation
	 * 
	 * @param mode
	 */
	public void setMacroMode(int mode) {
		macroMode = mode;
	}

	/**
	 * Run macro
	 * 
	 * @param st
	 */
	public void runMacro(Stack<String> st) {
		runningMacro = true;
		command = st;
		checkStep();

	}

	/**
	 * Macro step
	 */
	private void checkStep() {
		if (command.size() == 0) {
			runningMacro = false;
			return;
		}

		String step = command.peek();
		 Log.d("Macro", "step:" +step);
		 //Log.d("Macro" , "size:" + step.length());
		 if(step.length()==0){ 
			 command.pop();
			 return ;
		 }
		if (step.equals("!*!")) {
			command.pop();
			runMacroMode = MacroManagment.TOUCH_MACRO;
			makeStepTouch();
			return;
		}
		if (step.equals("Home")) {
			if (command.size() == 1)
				CoreController.home();
			command.pop();
			if (command.size() == 0) {
				runningMacro = false;
				return;
			}
			checkStep();
		} else if (step.equals("Back")) {
			boolean resul;
			int tries = 0;
			do {
				tries++;
				resul = CoreController.back();
			} while (resul && tries < 15);

			command.pop();
			if (command.size() == 0) {
				runningMacro = false;
				return;
			}
			checkStep();
		} else {

			for (Node n : nodeList) {
				if (n.getName().equals(step)) {
					Log.d("Macro", "Found:" + n.getName());

					int failSafe = 0;
					String result;
					do {
						nlc.focusIndex(step);
						failSafe++;
						result = nlc.selectFocus();
						Log.d("Macro", "clicked:" + result);


					} while ((result == null || result.length() == 0)
							&& failSafe < THRESHOLD_GUARD);


					if (failSafe < THRESHOLD_GUARD) {
						command.pop();
						if (command.size() == 0)
							runningMacro = false;
						checkStep();
					}
					return;
				} 
			}
			Node n;
			if ((n = nodeList.get(nodeList.size() - 1)).getName().equals(
					"SCROLL")) {
				nlc.focusIndex("SCROLL");
				if (nlc.selectFocus() == null) {
					nlc.selectFocus();

				}
			}

		}
	}

	private void makeStepTouch() {
		if (command.size() == 0) {
			runningMacro = false;
			runMacroMode = MacroManagment.NAV_MACRO;
 
			return; 

		} 

		String step = command.peek();
		if (step.equals("!*!")) {
			command.pop();
			//makeStepTouch();
			runMacroMode = MacroManagment.NAV_MACRO;
			checkStep();
		} else {
			if (command.size() > 0) {
				String s = command.pop();
				Log.d("Macro", "touch Step : " + s);
				runTouches(s);
				//makeStepTouch();
			} else {
				runMacroMode = MacroManagment.NAV_MACRO;
				runningMacro = false;
			}
		}

	}

	private void runTouches(String s) {
		String split[] = s.split(",");
		final ArrayList<Touch> touches = new ArrayList<Touch>();

		for (int i = 1; i < (split.length - 3); i += 4) {
			touches.add(new Touch(Integer.parseInt(split[i]), Integer
					.parseInt(split[i + 1]), Integer.parseInt(split[i + 2]),
					Double.parseDouble(split[i + 3])));
		}
		
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			public void run() { 
				if (touches.size() > 0) { 
					double time = touches.get(0).getTimestamp();
					int value;
					//Log.d()
					for (Touch t : touches) {
						value = t.getValue(); 
						double sleep = (t.getTimestamp() - time) ; 
						if(sleep<0)
							sleep=0;
						
						SystemClock.sleep((long) (sleep ));
					

						CoreController.injectToTouch(t.getType(), t.getCode(),
								value);
						time = t.getTimestamp();
					}
					makeStepTouch();				}
			}
		}, 2000);

	}
}
