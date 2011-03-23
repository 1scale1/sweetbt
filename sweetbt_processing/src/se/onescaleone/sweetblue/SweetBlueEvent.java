package se.onescaleone.sweetblue;

import java.util.EventObject;

public class SweetBlueEvent extends EventObject {

	private boolean connected;

	public SweetBlueEvent(SweetBlue src, boolean connected) {
		super(src);
		this.connected = connected;
	}

	public boolean getConnected() {
		return connected;
	}
}
