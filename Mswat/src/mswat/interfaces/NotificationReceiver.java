package mswat.interfaces;

public interface NotificationReceiver {
	/**
	 * Registers receiver, returns the receiver identifier (index)
	 * 
	 * @return
	 */
	public int registerNotificationReceiver();

	/**
	 * Receives notification
	 * 
	 * @param notification text
	 */
	public abstract void onNotification(String notification);

}
