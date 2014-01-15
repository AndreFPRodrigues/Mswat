package mswat.core.activityManager;

import java.util.ArrayList;
import java.util.List;

import mswat.core.CoreController;
import mswat.core.calibration.CalibrationActivity;
import mswat.core.feedback.FeedBack;
import mswat.core.ioManager.Monitor;

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
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
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
	private boolean logging = false;

	private SharedPreferences sharedPref;
	private ServicePreferences servPref;
	private Monitor monitor;
	private NodeListController nlc;
	private FeedBack fb;

	private boolean broadcastContent = false;

	/**
	 * Triggers whenever happens an event (changeWindow, focus, slide) Updates
	 * the current top parent of the screen contents
	 */
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		//Log.d(LT, event.toString());

		if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED)
			CoreController.updateNotificationReceivers("" + event.getText());
		else {

			if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED ) {
				if (event.getClassName().toString().contains("EditText")|| event.getClassName().toString().contains("MultiAutoCompleteTextView")) {					
					CoreController.startKeyboard();
				}
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
				if (logging
						&& AccessibilityEvent.eventTypeToString(
								event.getEventType()).contains("TEXT")) {

					if (event.getRemovedCount() > event.getAddedCount())
						monitor.registerKeystroke("BackSpace");
					else {

						if (event.getRemovedCount() != event.getAddedCount()) {
							// When the before text is a space it needs this to
							// properly
							// detect the backspace
							// Bug a string a char follow by a space "t " when
							// using
							// backspace it detects "t" instead of backspace
							if ((event.getText().size() - 2) == event
									.getBeforeText().length()
									|| (event.getAddedCount() - event
											.getRemovedCount()) > 1)
								monitor.registerKeystroke("BackSpace");
							else {
								String keypressed = event.getText().toString();
								keypressed = ""
										+ keypressed
												.charAt(keypressed.length() - 2);
								if (keypressed.equals(" "))
									keypressed = "Space";
								monitor.registerKeystroke(keypressed);
							}
						}

					}

				}
				// update the current content list
				checkList.clear();
				scrollNodes.clear();
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
					 printViewItens(checkList);
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
	
			if (child != null) {

				if (child.isScrollable() && !addScroll) {
					source.getBoundsInScreen(outBounds);
					addScroll = true;
					scrollNodes.add(new Node((String) "SCROLL", outBounds,
							child));
				}

				if (child.getChildCount() == 0) {

					if (child.getText() != null
							|| child.getContentDescription() != null
					/* && (child.isClickable() || source.isClickable()) */) {
						outBounds = new Rect();
						if (source.getClassName().toString().contains("Linear")) {
							source.getBoundsInScreen(outBounds);

						} else
							child.getBoundsInScreen(outBounds);
						if ((outBounds.centerX() > 0 && outBounds.centerY() > 0)) {

							if (child.getText() != null) {
								n = new Node((String) child.getText()
										.toString(), outBounds, child);

								// Log.d(LT, n.getAccessNode().toString());
								n.setIcon(checkIconMatch(child.getText()
										.toString()));

							} else
								n = new Node((String) child
										.getContentDescription().toString(),
										outBounds, child);

							checkList.add(n);
						}
					}
				} else
					listUpdate(child);
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
	public void printViewItens(ArrayList<Node> listCurrentNodes) {
		int size = listCurrentNodes.size();
		Log.d(LT,
				"---------------------List State------------------------------");

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
		// getServiceInfo().flags =
		// AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
		// getServiceInfo().flags =
		//AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;

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
		logging = sharedPref.getBoolean(servPref.LOG, false);
		boolean audioFeedBack = sharedPref.getBoolean(servPref.AUDIO, false);
		boolean visualFeedBack = sharedPref.getBoolean(servPref.VISUAL, false);

		boolean calibration = sharedPref
				.getBoolean(servPref.CALIBRATION, false);

		boolean broadcastIO = sharedPref.getBoolean(servPref.BROADCAST_IO,
				false);
		broadcastContent = sharedPref.getBoolean(servPref.BROADCAST_CONTENT,
				false);

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


		// initialise feedback
		fb = new FeedBack(this, windowManager, params);

		nlc = new NodeListController(this, audioFeedBack, visualFeedBack);

		// initialise monitor
		monitor = new Monitor(this, broadcastIO, touchIndex);

		// initialise coreController
		CoreController cc = new CoreController(nlc, monitor, this, controller,
				calibration, logging , keyboard);

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
	public void home() {
		performGlobalAction(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);

	}

	/**
	 * Register screen size to shared preferences
	 * 
	 * @param width
	 * @param height
	 */
	public void storeScreenSize(int width, int height) {
		sharedPref.edit().putInt("s_width", width);
		sharedPref.edit().putInt("s_height", height);

	}

	/**
	 * Register the touch device index to the shared preferences
	 * 
	 * @param index
	 */
	public void storeTouchIndex(int index) {
		sharedPref.edit().putInt(servPref.TOUCH_INDEX, index);
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

}
