package mswat.core.macro;

public class Touch {
	private int type,  code,  value;
	private double timestamp;
	public Touch(int type, int code, int value, double timestamp){
		this.type=type;
		this.code  = code;
		this.value= value;
		this.timestamp = timestamp;
	}
	public int getType() {
		return type;
	}
	public int getCode() {
		return code;
	}
	
	public int getValue() {
		return value;
	}
	
	public double getTimestamp() {
		return timestamp;
	}
	

}
