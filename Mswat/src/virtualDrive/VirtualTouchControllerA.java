package virtualDrive;

import mswat.core.CoreController;
import android.content.BroadcastReceiver;

public  abstract class VirtualTouchControllerA extends BroadcastReceiver  {
	final int EV_ABS = 0x03,ABS_MT_POSITION_X =53, ABS_MT_POSITION_Y=54, SYN_MT_REPORT=2, SYN_REPORT=0;// 330
	
	
	protected void injectTouch(int x , int y){
		CoreController.injectToVirtual(EV_ABS, ABS_MT_POSITION_X, x);
		CoreController.injectToVirtual(EV_ABS, ABS_MT_POSITION_Y, y);
		CoreController.injectToVirtual(0, SYN_REPORT, 0);

	}
	
	protected void injectSlide(int x , int y, int x1 , int y1){
		int addX=(x1-x)/5;
		int addY=(y1-y1)/5;
		
		for(int i =0;i<5;i++){
			CoreController.injectToVirtual(EV_ABS, ABS_MT_POSITION_X, x);
			CoreController.injectToVirtual(EV_ABS, ABS_MT_POSITION_Y, y);
			CoreController.injectToVirtual(0, SYN_REPORT, 0);
			x+=addX;
			y+=addY;
		}
	}
	
	

	protected void sinc(){
		CoreController.injectToVirtual(0, SYN_REPORT, 0);
		CoreController.injectToVirtual(0, SYN_MT_REPORT, 0);
		CoreController.injectToVirtual(0, SYN_REPORT, 0);

	}
}
