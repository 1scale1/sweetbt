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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
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

/* TODO */
// ** available()
// ** read()
// ** connect()
// ** readChar()
// ** readBytes()
// readBytesUntil()
// readString()
// readStringUntil()
// ** buffer()
// bufferUntil()
// ** last()
// ** lastChar()
// ** list()
// ** write()
// ** clear()
// ** stop()

public class SweetBlue implements Runnable {

	/* PApplet context */
	private Context ctx;

	public final static String VERSION = "##version##";

	/* Bluetooth */
	private BluetoothAdapter mAdapter;
	private BluetoothDevice mDevice;
	private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	/* Socket & streams for BT communication */
	private BluetoothSocket mSocket;
	private InputStream mInputStream;
	private OutputStream mOutputStream;
	private boolean connected = false;

	/* Buffer */
	private int bufferlength = 128;
	private int available = 0;
	private byte[] buffer;
	private byte[] rawbuffer;

	/* Debug variables */
	public static boolean DEBUG = false;
	public static String DEBUGTAG = "##name## ##version## Debug message: ";

	/**
	 * a Constructor, usually called in the setup() method in your sketch to
	 * initialize and start the library.
	 * 
	 * @example Hello
	 * @param theParent
	 */
	public SweetBlue(Context ctx) {
		this.ctx = ctx;
		welcome();

		/* Init the adapter */
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				mAdapter = BluetoothAdapter.getDefaultAdapter();
			}
		});
	}

	/**
	 * This will list all nearby bluetooth devices which have enabled
	 * "discovery mode".
	 * 
	 * This is an asynchronous call, as such it won't block the UI thread. Make
	 * sure not to try to connect while this is being handled because that will
	 * significantly reduce success rate and speed of both the connect() and
	 * list() methods.
	 */
	public void list() {
		/* Make sure the adapter is enabled */
		if (mAdapter.isEnabled()) {

			/* Register for intents! */
			IntentFilter filter = new IntentFilter();
			filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
			filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
			filter.addAction(BluetoothDevice.ACTION_FOUND);

			BroadcastReceiver receiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (intent.getAction().equals(
							BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
						Log.i("System.out", "Bluetooth discovery started.");
					} else if (intent.getAction().equals(
							BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
						Log.i("System.out", "Bluetooth discovery finished.");

						/* Unregister this reciever */
						ctx.unregisterReceiver(this);
					} else if (intent.getAction().equals(
							BluetoothDevice.ACTION_FOUND)) {
						/* Get the found device! */
						BluetoothDevice device = intent
								.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

						Log.i("System.out", "Device found: " + device.getName()
								+ " at[ " + device.getAddress() + " ]");
					}
				}
			};

			/* Register the receiver for listening to discovery events */
			ctx.registerReceiver(receiver, filter);

			/* Start discovery */
			mAdapter.startDiscovery();
		} else {
			Log.i("System.out", "Bluetooth adapter not enabled, aborting!");
		}
	}

	public void connect(String mac) {
		/* Before we connect, make sure to cancel any discovery! */
		if (mAdapter.isDiscovering()) {
			mAdapter.cancelDiscovery();

			Log.i("System.out", "Cancelled ongoing discovery");
		}

		/* Make sure we're using a real bluetooth address to connect with */
		if (BluetoothAdapter.checkBluetoothAddress(mac)) {
			/* Get the remove device we're trying to connect to */
			mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac);

			/* Create the RFCOMM sockets */
			try {
				mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
				mSocket.connect();

				/* Set the status */
				connected = true;

				/* Attach the streams */
				mInputStream = mSocket.getInputStream();
				mOutputStream = mSocket.getOutputStream();

				Thread thread = new Thread(this);
				thread.start();

				Log.i("System.out", "Connected to device " + mDevice.getName()
						+ " [" + mDevice.getAddress() + "]");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			Log.i("System.out", "Addres is not Bluetooth, please verify MAC.");
		}
	}

	/**
	 * Returns the available number of bytes in the buffer.
	 * 
	 * @return
	 */
	public int available() {
		return available;
	}

	/**
	 * 
	 */
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

	@Override
	public void run() {
		/* Set the connected state */
		connected = true;
		
		/* Init the buffer */
		buffer = new byte[bufferlength];
		rawbuffer = new byte[bufferlength];

		while (connected) {
			// Read from the InputStream
			try {
				/* Read the available bytes into the buffer */
				available = mInputStream.read(rawbuffer);

				/* Clone the raw buffer */
				buffer = rawbuffer.clone();

				Log.i("System.out", "Read " + available + " bytes from device "
						+ mDevice.getName() + " [" + mDevice.getAddress() + "]");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Writes a byte[] buffer to the output stream.
	 * 
	 * @param buffer
	 */
	public void write(byte[] buffer) {
		try {
			mOutputStream.write(buffer);

			Log.i("System.out", "Wrote " + buffer.toString() + " to device "
					+ mDevice.getName() + " [" + mDevice.getAddress() + "]");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Returns the first available byte in "buffer" and then removes it.
	 * 
	 * @return
	 */
	private byte readByte() {
		/* Get the first byte */
		byte b = buffer[0];

		/* Remove the read byte from the buffer */
		if (available > 0) {
			/* This essentially copies the buffer to itself */
			System.arraycopy(buffer, 1, buffer, 0, buffer.length - 1);

			/* Decrease the available bytes */
			available--;
		} else {
			/* Clone the raw buffer */
			buffer = rawbuffer.clone();
		}

		return (byte) (b & 0xFF);
	}

	/**
	 * Returns the next byte in the buffer as an int (0-255);
	 * 
	 * @return
	 */
	public int read() {
		return readByte();
	}

	/**
	 * Returns the whole byte buffer.
	 * 
	 * @return
	 */
	public byte[] readBytes() {
		return buffer;
	}

	/**
	 * Returns the available number of bytes in the buffer, and copies the
	 * buffer contents to the passed byte[]
	 * 
	 * @param buffer
	 * @return
	 */
	public int readBytes(byte[] buffer) {
		buffer = this.buffer.clone();
		return available;
	}

	/**
	 * Returns the next byte in the buffer as a char, if nothing is there it
	 * returns -1.
	 * 
	 * @return
	 */
	public char readChar() {
		return (char) readByte();
	}

	/**
	 * Sets the number of bytes to buffer.
	 * 
	 * @param bytes
	 * @return
	 */
	public int buffer(int bytes) {
		bufferlength = bytes;

		buffer = new byte[bytes];
		rawbuffer = buffer.clone();

		return bytes;
	}

	/**
	 * Returns the last byte in the buffer.
	 * 
	 * @return
	 */
	public int last() {
		return buffer[buffer.length - 1];
	}

	/**
	 * Returns the last byte in the buffer as char.
	 * 
	 * @return
	 */
	public char lastChar() {
		return (char) buffer[buffer.length - 1];
	}

	/**
	 * Clears the byte buffer.
	 */
	public void clear() {
		buffer = new byte[bufferlength];
	}

	/**
	 * Closes the connection on the socket. (What should this return?)
	 * 
	 * @return
	 */
	public int stop() {
		try {
			mSocket.close();
			/* If it successfully closes I guess we just return a success? */
			return 0;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			/* Otherwise we'll go ahead and say "no, this didn't work well!" */
			return 1;
		}
	}
}
