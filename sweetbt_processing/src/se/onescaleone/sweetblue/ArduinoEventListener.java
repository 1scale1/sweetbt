package se.onescaleone.sweetblue;

import java.util.EventListener;

public interface ArduinoEventListener extends EventListener {
	public void ArduinoRead(ArduinoReadEvent evt);
}
