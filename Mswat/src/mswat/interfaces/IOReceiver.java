package mswat.interfaces;



public interface  IOReceiver  {
	
	/**
	 * Registers receiver, returns the receiver identifier (index)
	 * @return
	 */
	public int registerIOReceiver();
	
	/**
	 * Receives io event 
	 * @param device 
	 * @param type
	 * @param code
	 * @param value
	 * @param timestamp
	 */
	public abstract void onUpdateIO(int device, int type, int code, int value, int timestamp);
		
}
