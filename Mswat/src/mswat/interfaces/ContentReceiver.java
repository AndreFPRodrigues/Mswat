package mswat.interfaces;

import java.util.ArrayList;

import mswat.core.activityManager.Node;

public interface  ContentReceiver  {
	
	/**
	 * Registers receiver, returns the receiver identifier (index)
	 * @return
	 */
	public boolean registerContentReceiver();
		
	
	/**
	 * Receives arrayList with the updated content
	 * @param content
	 */
	public abstract void onUpdateContent(ArrayList<Node> content);
		
}
