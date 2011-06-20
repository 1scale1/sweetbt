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
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
// available()
// ** connect()
// disconnect()
// readChar()
// readBytes()
// readBytesUntil()
// readString()
// readStringUntil()
// buffer()
// bufferUntil()
// last()
// lastChar()
// list()
// write()
// clear()
// stop()

public class SweetBlue {

	/* PApplet context */
	private Context ctx;

	public final static String VERSION = "##version##";

	/* Bluetooth */
	private BluetoothAdapter mAdapter;

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
		mAdapter = BluetoothAdapter.getDefaultAdapter();
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
						Log.i("Syste.out", "Bluetooth discovery started.");
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
}
