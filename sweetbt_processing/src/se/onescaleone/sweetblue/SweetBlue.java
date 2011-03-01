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

import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.os.Looper;
import processing.core.*;

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

	public final static String VERSION = "##version##";

	/* Contains threads to control the communication */
	private BluetoothChatService mChatService = null;
	private static boolean currentlySendingData = false;

	/* Link to the applications main handler */
	private Handler mainHandler;

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

					// init the chatservice
					if (mChatService == null)
						mChatService = new BluetoothChatService();

					// Connect the chatservice

					mChatService.connect(BluetoothAdapter.getDefaultAdapter()
							.getRemoteDevice(mac));
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

	public String sayHello() {
		return "hello library.";
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
	 * Tries to write data to the bluetooth port.
	 * 
	 * byte[]
	 * 
	 * @param data
	 */
	public byte[] write(byte value) {
		if (!this.isCurrentlySendingData()) {

			byte[] data = new byte[1];
			data[0] = value;

			mChatService.write(attachHeaderBytes(data));

			return data;
		}

		return null;
	}

	/**
	 * Tries to write data to the bluetooth port.
	 * 
	 * byte[]
	 * 
	 * @param data
	 */
	public byte[] write(byte[] value) {
		if (!this.isCurrentlySendingData()) {
			mChatService.write(attachHeaderBytes(value));

			return value;
		}

		return null;
	}

	/**
	 * Tries to write data to the bluetooth port.
	 * 
	 * int
	 * 
	 * @param data
	 */
	public byte[] write(int value) {
		if (!this.isCurrentlySendingData()) {
			byte[] data = new byte[4];

			for (int i = 0; i < 4; i++) {
				int offset = (data.length - 1 - i) * 8;
				data[i] = (byte) ((value >>> offset) & 0xFF);
			}

			mChatService.write(attachHeaderBytes(data));

			return data;
		}
		return null;
	}
	
	/**
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
		if (mChatService != null)
			return mChatService.getState();
		else
			return 0;
	}

	/**
	 * Used to organize data before transmitting to bluetooth device.
	 * 
	 * HEADER(4 bytes) - DATA(x bytes) - CHECKSUM(1 byte)
	 * 
	 * Header: [FOOTPRINT][FOOTPRINT][COMMAND][LENGTH]:(0xff)(0xff)(0x??)(0x??)
	 * 
	 * Data: [DATA]...: (0 - 127)
	 * 
	 * Checksum: [CHECKSUM]: (COMMAND XOR LENGTH)
	 * 
	 * @param in
	 * @return
	 */
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
		for (int i = 1; i < in.length; i += 2)
			in[i] |= 0x80;

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
}
