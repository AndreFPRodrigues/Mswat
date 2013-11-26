package mswat.core.activityManager;



import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class HierarchicalService extends AccessibilityService {

	//Parent node to the current screen content
	AccessibilityNodeInfo currentParent=null;
	
	
	/**
	 * Triggers whenever happens an event (changeWindow, focus, slide)
	 * Updates the current top parent of the screen contents
	 */
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		
	}
	
	@Override
	public void onInterrupt() {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Initialise NodeListController
	 * Initialise Monitor
	 * Initialise CoreController
	 * Initialise Feedback
	 */
	@Override
	public void onServiceConnected() {
		
	}
	
	/**
	 * 
	 * @return current content parent
	 */
	public AccessibilityNodeInfo getContentParent(){
		return currentParent;
	}
}
