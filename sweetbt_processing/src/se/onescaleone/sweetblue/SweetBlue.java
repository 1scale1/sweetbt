/**
 * you can put a one sentence description of your library here.
 *
 * ##copyright##
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 * 
 * @author		##author##
 * @modified	##date##
 * @version		##version##
 */

package se.onescaleone.sweetblue;

/*
 *  Written by Andreas Göransson & David Cuartielles, 1scale1 Handelsbolag, 
 *  for use in the project SweetBlue.
 *  
 *  SweetBlue: a library and communication protocol used to set Arduino 
 *  states over bluetooth from an Android device. Effectively removing 
 *  the need to program the Arduino chip.
 *  
 *  Copyright (C) 2011  1scale1 Handelsbolag
 *
 *  This file is part of SweetBlue.
 *
 *  SweetBlue is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  SweetBlue is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with SweetBlue.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.util.HashMap;

import processing.core.PApplet;
import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * This is a template class and can be used to start a new processing library or
 * tool. Make sure you rename this class as well as the name of the example
 * package 'template' to your own lobrary or tool naming convention.
 * 
 * @example Hello
 * 
 *          (the tag @example followed by the name of an example included in
 *          folder 'examples' will automatically include the example in the
 *          javadoc.)
 * 
 */

public class SweetBlue {

	// myParent is a reference to the parent sketch
	PApplet myParent;
//test
	public final static String VERSION = "##version##";

	/* Contains threads to control the communication */
	private BluetoothChatService mChatService = null;
	private static boolean currentlySendingData = false;

	/* Debug variables */
	public static boolean DEBUG = false;
	public static String DEBUGTAG = "##name## ##version## Debug message: ";

	private int state = -1;
	public static final int STATE_CONNECTED = 18;
	public static final int STATE_DISCONNECTED = 28;
	public static final int STATE_CONNECTING = 38;
	
	/* Link to the applications main handler */
	private Handler mainHandler;
	private Handler recieveHandler;

	/* Bluetooth constants, handler messages */
	public static final int MESSAGE_STATE_CHANGE = 19;
	public static final int MESSAGE_READ = 29;
	public static final int MESSAGE_WRITE = 39;
	public static final int MESSAGE_DEVICE_NAME = 49;
	public static final int MESSAGE_TOAST = 59;
	public static final int MESSATE_TEST_VIBRATOR = 69;
	// public static final int MESSAGE_ECHO = 79;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String DATA_STRING = "data_string";
	public static final String DATA_VALUE = "data_value";
	public static final String TOAST = "toast";

	/* Arduino Constants */
	public static final int HIGH = 1;
	public static final int LOW = 0;
	public static final int INPUT = 6;
	public static final int OUTPUT = 7;

	/* Map containing all pins and their read-values */
	private HashMap<Integer, Integer> values;

	/**
	 * a Constructor, usually called in the setup() method in your sketch to
	 * initialize and start the library.
	 * 
	 * @example Hello
	 * @param theParent
	 */
	public SweetBlue(PApplet theParent) {
		myParent = theParent;
		welcome();
		/* Init hashmap, has only 16 spaces now though */
		values = new HashMap<Integer, Integer>();
		
		/*
		 * Add the listener to the parent, this should force the sketch to
		 * implement the method...
		 */
		/*try {
			Method m = myParent.getClass().getMethod("SweetBlueConnected", new Class[] { SweetBlueEvent.class });
			this.addSweetBlueListener(new GenericListener(this, m));
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

	}

	/**
	 * Set all your pinmodes in this function inside Processing. The library
	 * will call this automatically.
	 */
	public void callPinModes() {
		if (SweetBlue.DEBUG)
			Log.i("System.out", SweetBlue.DEBUGTAG + "Attempting to set pin modes");

	}

	/**
	 * Tries to connect to the supplied MAC address.
	 * 
	 * @param mac
	 */
	public void connect(final String mac) {
		/* Make sure the chatservice isn't connected */
		if (mChatService != null) {
			mChatService.stop();
		}

		if (mac != null) {

			mainHandler = new Handler(Looper.getMainLooper());
			mainHandler.post(new Runnable() {

				@Override
				public void run() {

					// init the handler (recieve messages from bt thread)
					if (recieveHandler == null)
						recieveHandler = new Handler() {

							@Override
							public void handleMessage(Message msg) {

								switch (msg.what) {
								case MESSAGE_STATE_CHANGE:

									switch (msg.arg1) {
									case BluetoothChatService.STATE_CONNECTED:
										/* Dispatch the connected event */
										//dispatchConnectedEvent(new SweetBlueEvent(SweetBlue.this, true));
										state = STATE_CONNECTED;
										break;
									case BluetoothChatService.STATE_CONNECTING:
										/* Dispatch the connected event */
										//dispatchConnectedEvent(new SweetBlueEvent(SweetBlue.this, false));
										state = STATE_CONNECTING;
										break;
									case BluetoothChatService.STATE_LISTEN:
										/* Dispatch the connected event */
										//dispatchConnectedEvent(new SweetBlueEvent(SweetBlue.this, false));
										state = STATE_CONNECTING;
										break;
									case BluetoothChatService.STATE_NONE:
										/* Dispatch the connected event */
										//dispatchConnectedEvent(new SweetBlueEvent(SweetBlue.this, false));
										state = STATE_DISCONNECTED;
										break;
									}
									break;
								case MESSAGE_DEVICE_NAME:
									// Print the connected device name to PDE
									PApplet.println(msg.getData().getString(DEVICE_NAME)
											+ " connected.");
									break;
								case MESSAGE_READ:
									// Read from the output stream... byte[]
									int[] data = msg.getData().getIntArray(DATA_VALUE);

									/* Add the value to the hashmap */
									if (data != null && values != null)
										values.put(data[0], data[1]);

									if (SweetBlue.DEBUG && data != null) {
										/* Print the read data array */
										StringBuffer sb = new StringBuffer();
										for (int i = 0; i < data.length; i++)
											sb.append(data[i]).append(",");
										Log.i("System.out", SweetBlue.DEBUGTAG
												+ "ArduinoBT package: " + sb.toString());
									} else if (SweetBlue.DEBUG && data == null) {
										Log.i("System.out", SweetBlue.DEBUGTAG
												+ "Read data is null!");
									}
									break;
								}
							}

						};
					// init the chatservice
					if (mChatService == null && recieveHandler != null)
						mChatService = new BluetoothChatService(recieveHandler);

					// Connect the chatservice
					mChatService.connect(BluetoothAdapter.getDefaultAdapter()
							.getRemoteDevice(mac));

					// Add the listener??
				}
			});

		}
	}

	public boolean isConnected() {
		if (mChatService != null
				&& mChatService.getState() == BluetoothChatService.STATE_CONNECTED)
			return true;
		else
			return false;
	}

	private void welcome() {
		System.out.println("##name## ##version## by ##author##");
	}

	/**
	 * return the version of the library.
	 * 
	 * @return String
	 */
	public static String version() {
		return VERSION;
	}

	/**
	 * DEPRECATED
	 * 
	 * Tries to write data to the bluetooth port.
	 * 
	 * byte[]
	 * 
	 * @param data
	 */
	@Deprecated
	public byte[] write(byte[] value) {
		if (!this.isCurrentlySendingData()) {
			mChatService.write(attachHeaderBytes(value));
			return value;
		}

		return null;
	}

	/**
	 * Used to determine if we're sending data or not... don't use this
	 * yourself!
	 * 
	 * @return
	 */
	public static boolean isCurrentlySendingData() {
		return SweetBlue.currentlySendingData;
	}

	/**
	 * Function that allows BluetoothWorkers to "steal" the attention for the
	 * chatservice. Returns true to the worker if the variable was succesfully
	 * set. Returns false if the chatservice is busy.
	 * 
	 * @param currentlySendingData
	 * @return
	 */
	public static boolean setCurrentlySendingData(boolean currentlySendingData) {
		if (SweetBlue.currentlySendingData != currentlySendingData) {
			SweetBlue.currentlySendingData = currentlySendingData;
			return true;
		} else {
			return false;
		}
	}

	public int getState() {
		return state;
	}

	/**
	 * Changes the mode of a pin on the ArduinoBT.
	 * 
	 * example: pinMode( 1, SweetBlue.OUTPUT );
	 * 
	 * Note: Pins 0, 1, and 7 are a big NO-NO! These pins are connected to the
	 * bluetooth communication and reset and shouldn't be used!
	 * 
	 * @param pin
	 *            number on the ArduinoBT
	 * @param mode
	 *            OUTPUT or INPUT
	 * @return null (if fail) or the sent byte[]
	 */
	public byte[] pinMode(final int pin, int mode) {
		switch (mode) {
		case OUTPUT:
			mChatService.write(assemblePackage((byte) pin, (byte) 0x01, (byte) 0x00));
			break;
		case INPUT:
			mChatService.write(assemblePackage((byte) pin, (byte) 0x01, (byte) 0x01));
		}
		return null;
	}

	/**
	 * Writes HIGH or LOW to the specified pin on the ArduinoBT.
	 * 
	 * example: ditialWrite( 2, SweetBlue.HIGH );
	 * 
	 * @param pin
	 *            number on the ArduinoBT
	 * @param value
	 *            HIGH or LOW
	 */
	public void digitalWrite(final int pin, int value) {
		mChatService.write(assemblePackage((byte) pin, (byte) 0x03, (byte) value));
	}

	/**
	 * Reads value from pin.
	 * 
	 * @param pin
	 * @param variable
	 */
	public void digitalRead(int pin, int[] variable) {
		mChatService.write(assemblePackage((byte) pin, (byte) 0x02, (byte) 0x00));

		if (values.containsKey(pin))
			variable[0] = values.get(pin);
		else if (SweetBlue.DEBUG)
			Log.i("System.out", SweetBlue.DEBUGTAG + "No value exists on pin " + pin);
	}

	/**
	 * Writes value to the specified pin on the ArduinoBT.
	 * 
	 * example: analogWrite( 2, 127 );
	 * 
	 * @param pin
	 *            number on the ArduinoBT
	 * @param value
	 *            0 - 255
	 */
	public void analogWrite(final int pin, int value) {
		if (value >= 0 && value <= 255)
			mChatService.write(assemblePackage((byte) pin, (byte) 0x05, (byte) value));
		else
			Log.i("System.out", SweetBlue.DEBUGTAG + "Bad value on analogWrite!");
	}

	/**
	 * Reads value from pin.
	 * 
	 * IMPORTANT! To "fix" the issue of pass-by-value on primitives, we need the
	 * variable to be non-primitive. An array will do fine for solving this
	 * initially...
	 * 
	 * @param pin
	 *            The pin number to read
	 * @param variable
	 *            variable to which the reading should be written.
	 */
	public void analogRead(int pin, int[] variable) {
		mChatService.write(assemblePackage((byte) pin, (byte) 0x04, (byte) 0x00));

		if (values.containsKey(pin))
			variable[0] = values.get(pin);
		else if (SweetBlue.DEBUG)
			Log.i("System.out", SweetBlue.DEBUGTAG + "No value exists on pin " + pin);
	}

	/**
	 * Sends the close command to Arduino, so it knows to reset the
	 * BluetoothChip. This should allways be called in the "onStop" method in
	 * the sketch.
	 */
	public void close() {
		if (SweetBlue.DEBUG)
			Log.i("System.out", SweetBlue.DEBUGTAG + "Sending disconnect signal!");

		/* Sending the main cmd 0x05 will make the Arduino reset it's state */
		mChatService.write(assemblePackage((byte) 0x05, (byte) 0x00, (byte) 0x00,
				(byte) 0x00));
	}

	/**
	 * Used by the standard functions to send state to the arduino.
	 * 
	 * @param pin
	 * @param cmd
	 * @param val
	 * @return
	 */
	private byte[] assemblePackage(byte pin, byte cmd, byte val) {
		return this.assemblePackage((byte) 0x02, pin, cmd, val);
	}

	/**
	 * Assembles the Arduino command package and prepares it for serial
	 * 
	 * @param pin
	 * @param cmd
	 * @param val
	 */
	private byte[] assemblePackage(byte cmd, byte pin, byte arduinocmd, byte val) {
		/* Header */
		// [FP][FP][cmd][len][arduinocmd][pin][val][chksum]

		/* Create the package */
		byte[] buffer = new byte[8];

		/* Footprint */
		buffer[0] = (byte) 0xff;
		buffer[1] = (byte) 0xff;

		/* Main command - 0x02 right now */
		// buffer[2] = (byte) 0x02;
		buffer[2] = (byte) cmd;

		/* Length, it's always the same size... for now */
		buffer[3] = (byte) 0x03;

		/* Arduino command - pinmode, digitalwrite, analogread... etc */
		buffer[4] = arduinocmd;

		/* The pin on which to act */
		buffer[5] = pin;

		/* The value */
		buffer[6] = val;

		/* The checksum - cmd ^ len ^ arduinocmd ^ pin ^ val */
		buffer[7] = (byte) ((((buffer[2] ^ buffer[3]) ^ arduinocmd) ^ pin) ^ val);

		return buffer;
	}

	/**
	 * 
	 * Used to organize data before transmitting to bluetooth device.
	 * 
	 * HEADER(4 bytes) - DATA(x bytes) - CHECKSUM(1 byte)
	 * 
	 * Header: [FOOTPRINT][FOOTPRINT][COMMAND][LENGTH]: (0xff)(0xff)(0x??)(0x??)
	 * 
	 * Data: [DATA]...: (0 - 127)
	 * 
	 * Checksum: [CHECKSUM]: (COMMAND XOR LENGTH)
	 * 
	 * @param in
	 * @return
	 * @deprecated This function is used for the Sweet tool, the library uses
	 *             the newer "firmata" like function to communicate with
	 *             ArduinoBT.
	 */
	@Deprecated
	private byte[] attachHeaderBytes(byte[] in) {
		/* Create the header bytes */
		byte[] header = new byte[4];
		header[0] = (byte) 0xff;
		header[1] = (byte) 0xff;
		header[2] = (byte) 0x02;
		header[3] = (byte) in.length;

		/* Create the checksum byte */
		byte checksum = 0;
		checksum = (byte) (header[2] ^ header[3]);
		for (int i = 0; i < in.length; i++)
			checksum ^= in[i];

		/* Data fix, making sure we won't have two 0xff in a row */
		// !! Causes issues... we're not really using this anymore
		// for (int i = 1; i < in.length; i += 2)
		// in[i] |= 0x80;

		/* Final assembly... */
		byte[] outdata = new byte[header.length + in.length + 1];
		int i = 0;
		/* ...header */
		for (int index = 0; index < header.length; index++, i++)
			outdata[i] = header[index];
		/* ...data */
		for (int index = 0; index < in.length; index++, i++)
			outdata[i] = in[index];
		/* ...chksum */
		outdata[outdata.length - 1] = checksum;

		return outdata;
	}

	/* ===== EVENT OBJECTS ===== */
	/**
	 * List of event listeners.
	 */
	protected EventListenerList listenerList = new EventListenerList();

	/**
	 * Add a new EssemmessListener to the list.
	 * 
	 * @param listener
	 */
	public void addSweetBlueListener(SweetBlueListener listener) {
		listenerList.add(SweetBlueListener.class, listener);
	}

	/**
	 * Remove the selected EssemmessListener from the list.
	 * 
	 * @param listener
	 */
	public void removeSweetBlueListener(SweetBlueListener listener) {
		listenerList.remove(SweetBlueListener.class, listener);
	}

	/**
	 * Dispatches a new READ event, this is dispatched when the HttpWorker has
	 * executed a READ action on the Essemmess server.
	 * 
	 * @param evt
	 */
	void dispatchConnectedEvent(SweetBlueEvent evt) {
		Object[] listeners = listenerList.getListenerList();
		// Each listener occupies two elements - the first is the listener class
		// and the second is the listener instance
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == SweetBlueEvent.class) {
				((SweetBlueListener) listeners[i + 1]).SweetBlueConnected(evt);
			}
		}
	}
}
