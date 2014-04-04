package mswat.controllers;

import mswat.core.CoreController;
import android.content.BroadcastReceiver;
import android.os.SystemClock;

/**
 * Parent of all interfacing schemes
 * @author Andre Rodrigues
 *
 */

public abstract class ControlInterface extends BroadcastReceiver{
	
	
	
	/**
	 * Send navNext message
	 */
	protected	void navNext(){
		CoreController.commandNav(CoreController.NAV_NEXT,null);
	}
	
	/**
	 * Send navPrev message
	 */
	protected	void navPrev(){
		CoreController.commandNav(CoreController.NAV_PREV, null); 
	}
	
	/**
	 * Send selectCurrent message
	 */
	protected	void selectCurrent(){
		CoreController.commandNav(CoreController.SELECT_CURRENT,null);
	}
	
	/**
	 * Send focusIndex message
	 */
	protected void focusIndex(String string){
		CoreController.commandNav(CoreController.FOCUS_INDEX,string);
 
	}
	
	/**
	 * Send focusIndex message
	 */
	protected void highlightIndex(String index){
		CoreController.commandNav(CoreController.HIGHLIGHT_INDEX,index);
	}
	
	protected void clearHighlights(){
		CoreController.clearHightlights();
	}
	
	protected void home(){
		CoreController.home();
	}
	
	protected boolean back(){
		return CoreController.back();
	}
	
	protected void clickNode(String description){
		focusIndex(description);
		SystemClock.sleep(100);
		selectCurrent();
	}
	
}
