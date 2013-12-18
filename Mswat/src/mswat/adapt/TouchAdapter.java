package mswat.adapt;

import mswat.core.CoreController;

public class TouchAdapter {

	public static final int NO_ADAPT = 0;

	public static int[] adapt(int type, int code, int value, int mode) {
		int[] event = new int[3];

		switch (mode) {
		case NO_ADAPT:
			if (code == 53)
				value = CoreController.xToScreenCoord(value);
			if (code == 54) {
				value = CoreController.yToScreenCoord(value);
			}
			event[0] = type;
			event[1] = code;
			event[2] = value;
			break;
		}

		return event;
	}
}
