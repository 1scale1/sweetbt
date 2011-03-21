package se.onescaleone.sweetblue;

/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 *  Modified by Andreas Göransson & David Cuartielles, 1scale1 Handelsbolag, 
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for incoming
 * connections, a thread for connecting with a device, and a thread for
 * performing data transmissions when connected.
 */
public class BluetoothChatService {
	private static final String TAG = "BluetoothChatService";
	private static final boolean D = true;

	// Name for the SDP record when creating server socket
	private static final String NAME = "BluetoothChat";

	// Unique UUID for this application
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	// Member fields
	private final BluetoothAdapter mAdapter;
	private Handler mHandler;
	private AcceptThread mAcceptThread;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private int mState;

	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0; // we're doing nothing
	public static final int STATE_LISTEN = 1; // now listening for incoming
	// connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing
	// connection
	public static final int STATE_CONNECTED = 3; // now connected to a remote

	// device

	/**
	 * Constructor. Prepares a new BluetoothChat session.
	 * 
	 * @param context
	 *            The UI Activity Context
	 * @param handler
	 *            A Handler to send messages back to the UI Activity
	 */
	public BluetoothChatService(/* Context context, */Handler handler) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
	}

	/**
	 * Set the current state of the chat connection
	 * 
	 * @param state
	 *            An integer defining the current connection state
	 */
	private synchronized void setState(int state) {
		if (D)
			Log.d(TAG, "setState() " + mState + " -> " + state);
		mState = state;

		if (SweetBlue.DEBUG)
			Log.i("System.out", SweetBlue.DEBUGTAG + " setState() " + mState + " -> " + state);

		// Give the new state to the Handler so the UI Activity can update
		mHandler.obtainMessage(SweetBlue.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	}

	/**
	 * Return the current connection state.
	 */
	public synchronized int getState() {
		return mState;
	}

	/**
	 * Start the chat service. Specifically start AcceptThread to begin a
	 * session in listening (server) mode. Called by the Activity onResume()
	 */
	public synchronized void start() {
		if (D)
			Log.d(TAG, "start");

		// Cancel any thread attempting to make a connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Start the thread to listen on a BluetoothServerSocket
		if (mAcceptThread == null) {
			mAcceptThread = new AcceptThread();
			mAcceptThread.start();
		}
		setState(STATE_LISTEN);
	}

	/**
	 * Start the ConnectThread to initiate a connection to a remote device.
	 * 
	 * @param device
	 *            The BluetoothDevice to connect
	 */
	public synchronized void connect(BluetoothDevice device) {
		if (D)
			Log.d(TAG, "connect to: " + device);

		// Cancel any thread attempting to make a connection
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * 
	 * @param socket
	 *            The BluetoothSocket on which the connection was made
	 * @param device
	 *            The BluetoothDevice that has been connected
	 */
	public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
		if (D)
			Log.d(TAG, "connected");

		// Cancel the thread that completed the connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Cancel the accept thread because we only want to connect to one
		// device
		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}

		// Start the thread to manage the connection and perform transmissions
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();

		// Send the name of the connected device back to the UI Activity
		Message msg = mHandler.obtainMessage(SweetBlue.MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(SweetBlue.DEVICE_NAME, device.getName());
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		setState(STATE_CONNECTED);
	}

	/**
	 * Stop all threads
	 */
	public synchronized void stop() {
		if (D)
			Log.d(TAG, "stop");
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}
		setState(STATE_NONE);
	}

	/**
	 * Write to the ConnectedThread in an unsynchronized manner
	 * 
	 * @param out
	 *            The bytes to write
	 * @see ConnectedThread#write(byte[])
	 */
	public void write(byte[] out) {
		// Create temporary object
		ConnectedThread r;
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (mState != STATE_CONNECTED)
				return;
			r = mConnectedThread;
		}
		// Perform the write unsynchronized
		r.write(out); // , channels, samples );
	}

	/**
	 * Indicate that the connection attempt failed and notify the UI Activity.
	 */
	private void connectionFailed() {
		setState(STATE_LISTEN);

		// Send a failure message back to the Activity

		Message msg = mHandler.obtainMessage(SweetBlue.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(SweetBlue.TOAST, "Unable to connect device");
		msg.setData(bundle);
		mHandler.sendMessage(msg);

	}

	/**
	 * Indicate that the connection was lost and notify the UI Activity.
	 */
	private void connectionLost() {
		setState(STATE_LISTEN);

		// Send a failure message back to the Activity

		Message msg = mHandler.obtainMessage(SweetBlue.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(SweetBlue.TOAST, "Device connection was lost");
		msg.setData(bundle);
		mHandler.sendMessage(msg);

	}

	/**
	 * This thread runs while listening for incoming connections. It behaves
	 * like a server-side client. It runs until a connection is accepted (or
	 * until cancelled).
	 */
	private class AcceptThread extends Thread {
		// The local server socket
		private final BluetoothServerSocket mmServerSocket;

		public AcceptThread() {
			BluetoothServerSocket tmp = null;

			// Create a new listening server socket
			try {
				tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "listen() failed", e);
			}
			mmServerSocket = tmp;
		}

		@Override
		public void run() {
			if (D)
				Log.d(TAG, "BEGIN mAcceptThread" + this);
			setName("AcceptThread");
			BluetoothSocket socket = null;

			// Listen to the server socket if we're not connected
			while (mState != STATE_CONNECTED) {
				try {
					// This is a blocking call and will only return on a
					// successful connection or an exception
					socket = mmServerSocket.accept();
				} catch (IOException e) {
					Log.e(TAG, "accept() failed", e);
					break;
				}

				// If a connection was accepted
				if (socket != null) {
					synchronized (BluetoothChatService.this) {
						switch (mState) {
						case STATE_LISTEN:
						case STATE_CONNECTING:
							// Situation normal. Start the connected thread.
							connected(socket, socket.getRemoteDevice());
							break;
						case STATE_NONE:
						case STATE_CONNECTED:
							// Either not ready or already connected. Terminate
							// new socket.
							try {
								socket.close();
							} catch (IOException e) {
								Log.e(TAG, "Could not close unwanted socket", e);
							}
							break;
						}
					}
				}
			}
			if (D)
				Log.i(TAG, "END mAcceptThread");
		}

		public void cancel() {
			if (D)
				Log.d(TAG, "cancel " + this);
			try {
				mmServerSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of server failed", e);
			}
		}
	}

	/**
	 * This thread runs while attempting to make an outgoing connection with a
	 * device. It runs straight through; the connection either succeeds or
	 * fails.
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			try {
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "create() failed", e);
			}
			mmSocket = tmp;
		}

		@Override
		public void run() {
			Log.i(TAG, "BEGIN mConnectThread");
			setName("ConnectThread");

			// Always cancel discovery because it will slow down a connection
			mAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				mmSocket.connect();
			} catch (IOException e) {
				connectionFailed();
				// Close the socket
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG, "unable to close() socket during connection failure", e2);
				}
				// Start the service over to restart listening mode
				BluetoothChatService.this.start();
				return;
			}

			// Reset the ConnectThread because we're done
			synchronized (BluetoothChatService.this) {
				mConnectThread = null;
			}

			// Start the connected thread
			connected(mmSocket, mmDevice);
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	/**
	 * This thread runs during a connection with a remote device. It handles all
	 * incoming and outgoing transmissions.
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			Log.d(TAG, "create ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		@Override
		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
			byte[] buffer = new byte[1024];
			int bytes;

			// Keep listening to the InputStream while connected
			while (true) {
				try {
					buffer = new byte[1024];

					// Read from the InputStream
					bytes = mmInStream.read(buffer);

					if (bytes > 3) {
						/* For debugging purposes */
						if (SweetBlue.DEBUG) {
							StringBuilder in = new StringBuilder();
							in.append("Reading buffer... ");
							for (int b = 0; b < bytes; b++)
								in.append(buffer[b]).append(",");
							Log.i("System.out", SweetBlue.DEBUGTAG + in.toString());
						}

						/* Parse the buffer */
						parseReadBuffer(buffer, bytes);

					} else {
						if (SweetBlue.DEBUG)
							Log.i("System.out", SweetBlue.DEBUGTAG
									+ "Not enough bytes in stream, impossible package!");
					}

				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					connectionLost();
					break;
				}
			}
		}

		/**
		 * This parses the input stream of the connected bluetooth socket. The
		 * package is constructed the same way as the output socket (to BT)
		 * 
		 * [FOOTPRINT][FOOTPRINT][CMD][LEN][AA][PP][XX][YY][CC]
		 * 
		 * CMD - board status LEN - command length AA - arduino command PP - pin
		 * XX - read value 1 YY - read value 2 CC - checksum
		 * 
		 * @param buffer
		 * @return
		 */
		private byte[] parseReadBuffer(byte[] buffer, int bytes) {
			int headerstart = -1;

			/*
			 * Find the foot print, [0xff][0xff], this marks the beginning of
			 * the header
			 */
			for (int i = 0; i < bytes - 1; i++) {
				headerstart = (buffer[i] == (byte) 0xff && buffer[i + 1] == (byte) 0xff) ? i : -1;

				/* We found a possible package... */
				if (headerstart != -1) {
					if (SweetBlue.DEBUG)
						Log.i("System.out", SweetBlue.DEBUGTAG + "Found header footprint!");

					/* Footprint 1 */
					byte footp1 = buffer[headerstart + 0];

					/* Footprint 2 */
					byte footp2 = buffer[headerstart + 1];

					/* Get the board cmd */
					byte cmd = buffer[headerstart + 2];

					/* Get the data length */
					byte datalen = buffer[headerstart + 3];

					/* Get the arduino cmd */
					byte arduinocmd = buffer[headerstart + 4];

					/* Get the pin */
					byte pin = buffer[headerstart + 5];

					/* Get XX */
					byte xx = buffer[headerstart + 6];

					/* If read-error */
					if (xx == 0xff) {
						if (SweetBlue.DEBUG)
							Log.i("processing.android.sweetblue", SweetBlue.DEBUGTAG
									+ "Failed value check!");
						continue;
					}

					/* get YY */
					byte yy = buffer[headerstart + 7];

					/* get the sent chksum */
					byte readchksum = buffer[headerstart + 8];

					/* calculate the chksum and compare */
					byte calcchksum = 0;
					for (int j = 2; j < 8; j++)
						calcchksum ^= buffer[headerstart + j];

					if (readchksum != calcchksum) {
						if (SweetBlue.DEBUG)
							Log.i("processing.android.sweetblue", SweetBlue.DEBUGTAG
									+ "Failed checksum!");
						continue;
					} else {
						/* What are we reading - a ping or a response? */
						if (cmd == (byte) 0x04) {
							/* Arduino PING request */
							/*
							 * We'll send the values we read from the stream
							 * back EXCEPT the chksum, we'll calculate that from
							 * the read values instead.
							 */
							byte[] pingresponse = new byte[] { footp1, footp2, cmd, datalen,
									arduinocmd, pin, xx, yy, calcchksum };
							this.write(pingresponse);
						} else {
							/* Arduino RESPONSE */

							/* SUCCESS !! - Send the pin & value to the activity */
							Message msg = mHandler.obtainMessage(SweetBlue.MESSAGE_READ);
							Bundle bundle = new Bundle();
							/* Temporary, we're just sending the byte[] */
							// bundle.putByteArray(SweetBlue.DATA_VALUE, data);
							bundle.putIntArray(SweetBlue.DATA_VALUE, new int[] { pin,
									(xx * 128 + yy) });
							msg.setData(bundle);
							mHandler.sendMessage(msg);

							if (SweetBlue.DEBUG) {
								String arduinopkg = (byte) buffer[headerstart] + " "
										+ (byte) buffer[headerstart + 1] + " "
										+ (byte) buffer[headerstart + 2] + " "
										+ (byte) buffer[headerstart + 3] + " "
										+ (byte) buffer[headerstart + 4] + " "
										+ (byte) buffer[headerstart + 5] + " "
										+ (byte) buffer[headerstart + 6] + " "
										+ (byte) buffer[headerstart + 7] + " "
										+ (byte) buffer[headerstart + 8];

								Log.i("System.out", SweetBlue.DEBUGTAG
										+ "Found ArduinoBT package! [" + arduinopkg + "]");
							}
						}
					}
				} else {
					/* Failed finding header footprint */
					if (SweetBlue.DEBUG)
						Log.i("System.out", SweetBlue.DEBUGTAG + "Failed header footprint!");
				}
			}

			return null;
		}

		/**
		 * Write to the connected OutStream.
		 * 
		 * @param buffer
		 *            The bytes to write
		 */
		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);

				/* For debugging purposes */
				if (SweetBlue.DEBUG) {
					StringBuilder out = new StringBuilder();
					out.append("Writing buffer... ");
					for (int b = 0; b < buffer.length; b++)
						out.append(buffer[b]).append(",");
					Log.i("System.out", SweetBlue.DEBUGTAG + out.toString());
				}
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

}
