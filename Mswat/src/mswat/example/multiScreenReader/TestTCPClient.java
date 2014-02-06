package mswat.example.multiScreenReader;

import java.util.ArrayList;

import android.os.AsyncTask;
import android.util.Log;
import mswat.tcpClient.TCPClient;

public class TestTCPClient {
	private final String LT = "ScreenReader";
	private TCPClient mTcpClient; 

	public TestTCPClient(){ 
		 
		// connect to the server
        new connectTask().execute("");
        
        
	}
	
	public void sendMessage(String message){
		 //sends the message to the server
        if (mTcpClient != null) {
            mTcpClient.sendMessage(message);
        }
	} 
	
	 public class connectTask extends AsyncTask<String,String,TCPClient> {
		  
	        @Override
	        protected TCPClient doInBackground(String... message) {
	 
	            //we create a TCPClient object and
	            mTcpClient = new TCPClient("192.168.43.9", 7999, new TCPClient.OnMessageReceived() {
	                @Override
	                //here the messageReceived method is implemented
	                public void messageReceived(String message) {
	                    //this method calls the onProgressUpdate
	                    publishProgress(message);
	                }
	            });
	            mTcpClient.run();
	 
	            return null;
	        }
	 
	        @Override
	        protected void onProgressUpdate(String... values) {
	            super.onProgressUpdate(values);          
	            Log.d(LT, "received: " + values[0]);
	        
	        }
	    }
}
