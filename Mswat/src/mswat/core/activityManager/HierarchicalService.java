package mswat.core.activityManager;

import java.util.ArrayList;
import java.util.List;

import mswat.core.CoreController;
import mswat.core.calibration.CalibrationActivity;
import mswat.core.feedback.FeedBack;
import mswat.core.ioManager.Monitor;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class HierarchicalService extends AccessibilityService {

	// Parent node to the current screen content
	AccessibilityNodeInfo currentParent = null;

	private final String LT = "Hier";

	// list with the current screen nodes
	private ArrayList<Node> nodeList = new ArrayList<Node>();
	private ArrayList<Node> checkList = new ArrayList<Node>();

	// node that represents the possibility of sliding
	private Node scrollNode = null;
	// list of install apps icons
	private ArrayList<AppIcon> appIcons = new ArrayList<AppIcon>();

	// flag to register keystrokes
	private boolean logging = false;

	private SharedPreferences sharedPref;
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
				&& AccessibilityEvent.eventTypeToString(event.getEventType())
						.contains("TEXT")) {

			if (event.getRemovedCount() > event.getAddedCount())
				monitor.registerKeystroke("BackSpace");
			else {

				if (event.getRemovedCount() != event.getAddedCount()) {
					// When the before text is a space it needs this to properly
					// detect the backspace
					// Bug a string a char follow by a space "t " when using
					// backspace it detects "t" instead of backspace
					if ((event.getText().size() - 2) == event.getBeforeText()
							.length()
							|| (event.getAddedCount() - event.getRemovedCount()) > 1)
						monitor.registerKeystroke("BackSpace");
					else {
						String keypressed = event.getText().toString();
						keypressed = ""
								+ keypressed.charAt(keypressed.length() - 2);
						if (keypressed.equals(" "))
							keypressed = "Space";
						monitor.registerKeystroke(keypressed);
					}
				}

			}

		}
		// update the current content list
		checkList.clear();
		scrollNode = null;
		listUpdate(currentParent);
		if (scrollNode != null)
			checkList.add(scrollNode);

		if (checkUpdate()) {
			nlc.updateList(nodeList);
			if(broadcastContent)
				CoreController.nodeMessages(nodeList.toString());
			
			//Send content update to the receivers
			CoreController.updateContentReceivers(nodeList);
		}

		// printViewItens(checkList);
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
		for (int i = 0; i < source.getChildCount(); i++) {
			child = source.getChild(i);

			if (child != null) {

				if (child.isScrollable())
					scrollNode = new Node((String) "SCROLL", outBounds, child);

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

	/**
	 * Initialise NodeListController Initialise Monitor Initialise
	 * CoreController Initialise Feedback
	 */
	@Override
	public void onServiceConnected() {
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
		logging = sharedPref.getBoolean(ServicePreferences.LOG, false);
		boolean audioFeedBack = sharedPref.getBoolean(ServicePreferences.AUDIO,
				false);
		boolean visualFeedBack = sharedPref.getBoolean(
				ServicePreferences.VISUAL, false);
		
		boolean calibration = sharedPref.getBoolean(
				ServicePreferences.CALIBRATION, false);
		
		boolean broadcastIO = sharedPref.getBoolean(ServicePreferences.BROADCAST_IO, false);
		broadcastContent = sharedPref.getBoolean(ServicePreferences.BROADCAST_CONTENT, false);
		
		if ((sharedPref.getInt("s_width", (int) CoreController.S_WIDTH)) != 0) {
			CoreController.S_WIDTH = sharedPref.getInt("s_width",
					(int) CoreController.S_WIDTH);
			CoreController.S_HEIGHT = sharedPref.getInt("s_height",
					(int) CoreController.S_HEIGHT);		
		}else{
			CoreController.S_WIDTH = 1024;
			CoreController.S_HEIGHT = 960;
		}

		String controller = sharedPref.getString(ServicePreferences.CONTROLLER,
				"null");

		// initialise feedback
		fb = new FeedBack(this, windowManager, params);

		nlc = new NodeListController(this, audioFeedBack, visualFeedBack);

		// initialise monitor
		monitor = new Monitor( this, broadcastIO);

		// initialise coreController
		CoreController cc = new CoreController(nlc, monitor, this, controller,
				calibration, logging);

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

	public void storeScreenSize(int width, int height) {
		sharedPref.edit().putInt("s_width", width);
		sharedPref.edit().putInt("s_height", height);

	}

}
