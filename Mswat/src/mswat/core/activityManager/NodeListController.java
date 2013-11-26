package mswat.core.activityManager;


import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * Content Tree controller
 * 
 * @author Andre Rodrigues
 * 
 */
public class NodeListController {
	
	//Debugging purposes
	private final String LT = "NodeListController";

	// Icon List
	ArrayList<AppIcon> appIcons = new ArrayList<AppIcon>();

	// Content list
	ArrayList<Node> listCurrentNodes = new ArrayList<Node>();
	
	//navigation variables
	final int FOWARD = 0;
	final int BACKWARD = 1;
	int currentNavIndex = -1;
	int navDirection = FOWARD;
	boolean startedLineNav = true;
	
	//Node that represents the possibility of sliding to the next window
	private Node scrollNode = null;
	
	/**
	 * Create current installed apps icon list
	 * @param pm
	 */
	public NodeListController(PackageManager pm) {
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
	}

	/**
	 * Generates the list of the content tree last children
	 * @return List of the current last children
	 */
	public ArrayList<Node> getContent() {
		return listCurrentNodes;
	}

	/**
	 * Generates the current content tree
	 * @return current full content tree
	 */
	public ArrayList<Node> getFullContent() {
		return null;
	}

	/**
	 * Select current focused node
	 */
	public void selectFocus() {
	}
	
	/**
	 * Navigate to next node
	 */
	public void navNext() {
		
	}
	
	/**
	 * Navigate to previous node
	 */
	public void navPrev() {
		
	}
	
	/**
	 * Gets node name at given coords, null if none
	 * @param x coord
	 * @param y coord
	 * @return String node description
	 */
	public String getNodeAt(double x, double y) {

		String result = "null";

		int size = listCurrentNodes.size();

		for (int i = 0; i < size; i++) {

			if (listCurrentNodes.get(i).isInside(x, y)) {
				return listCurrentNodes.get(i).getName();
			}

		}

		return result;
	}
	
	/**
	 * Returns icon associated with string s
	 * @param s 
	 * @return drawable icon
	 */
	public Drawable getIcon (String s){
		return null;
	}

	/**
	 * Check if the clickable node has a correspondent icon
	 * @param charSequence
	 * @return -1 not found, >-1 icon index
	 */
	@SuppressWarnings("unused")
	private int checkIconMatch(CharSequence charSequence) {
		for (int i = 0; i < appIcons.size(); i++) {

			if (appIcons.get(i).getDescription().contentEquals(charSequence))
				return i;
		}
		return -1;
	}

	/**
	 *  Debugging
	 *  Print to log current view list node
	 */
		public void printViewItens() {
			int size = listCurrentNodes.size();
			Log.d(LT,
					"---------------------List State------------------------------");

			for (int i = 0; i < size; i++) {
				Log.d(LT, listCurrentNodes.get(i).toString());
			}

		}

}
