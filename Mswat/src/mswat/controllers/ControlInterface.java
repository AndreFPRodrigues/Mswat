package mswat.controllers;

import mswat.core.CoreController;
import android.content.BroadcastReceiver;

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
		CoreController.commandNav(CoreController.NAV_NEXT,0);
	}
	
	/**
	 * Send navPrev message
	 */
	protected	void navPrev(){
		CoreController.commandNav(CoreController.NAV_PREV,0); 
	}
	
	/**
	 * Send selectCurrent message
	 */
	protected	void selectCurrent(){
		CoreController.commandNav(CoreController.SELECT_CURRENT,0);
	}
	
	/**
	 * Send focusIndex message
	 */
	protected void focusIndex(int index){
		CoreController.commandNav(CoreController.FOCUS_INDEX,index);
 
	}
	
	/**
	 * Send focusIndex message
	 */
	protected void highlightIndex(int index){
		CoreController.commandNav(CoreController.HIGHLIGHT_INDEX,index);
 
	}
	
	protected void home(){
		CoreController.home();
	}
	
	protected void back(){
		CoreController.back();
	}
	
}
