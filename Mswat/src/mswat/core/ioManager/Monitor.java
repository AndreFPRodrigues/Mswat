package mswat.core.ioManager;

import java.util.ArrayList;

import mswat.core.CoreController;
import mswat.core.ioManager.Events.InputDevice;


public class Monitor {

	//List of internal devices (touch, accelerometer)
	ArrayList <InputDevice> dev;
	
	/**
	 * Initialises list of devices
	 */
	public Monitor () {
		Events ev= new Events();
		dev=ev.Init();
		
	}
	
	/**
	 * Open device in the index position
	 * @param index position of the device to open
	 * @return if successful
	 */
	public boolean open(int index){
		return false;
	}
	
	/**
	 * Blocks or unblock the device in index position
	 * @param b - true to block, false to unblock
	 * @param index - device index
	 * @return if successful
	 */
	public boolean setBlock(int index, boolean b){
		return false;
	}
	
	/**
	 * Inject event into the device in the position index
	 * @param index - device index
	 * @param type  
	 * @param code
	 * @param value
	 * @return if successful
	 */
	public boolean inject(int index ,int type, int code, int value){
		return false;
	}
	
	/**
	 * Starts or stops monitoring the device in position index
	 * @param index - device index
	 * @param b - true to monitor, false to stop monitoring
	 */
	public void monitorDevice(int index, boolean b){
		
		//Posts messages to Core Controller
	}
	

	
	/**
	 * Returns a String array of the internal devices
	 * @return String [] - internal devices names
	 */
	public String [] getDevices(){
		return null;
	}
}
