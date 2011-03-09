package se.onescaleone.sweetblue;

public class ArduinoPin {

	public int pin;
	public int val;

	public ArduinoPin(int pin) {
		this(pin, 0);
	}

	public ArduinoPin(int pin, int val) {
		this.pin = pin;
		this.val = val;
	}
}
