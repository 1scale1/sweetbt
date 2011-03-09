/** 
 * SweetBlue - Connect
 * by Andreas Göransson. 
 * 
 * This sketch connects to ArduinoBT. If a connection was successfully
 * established it will fill a small rectangle with green color.
 * 
 * Note: Make sure to enable the BLUETOOTH and BLUETOOTH_ADMIN Sketch
 * Permissions.
 */


import se.onescaleone.sweetblue.*;

/* Library obj. */
SweetBlue bt;

void setup() {
  /* Connect to the ArduinoBT */
  if ( bt == null ) {
    bt = new SweetBlue( this );
    bt.connect( /* ArduinoBT MAC */ );
  }
}

void draw() {
  /* UI - Bluetooth state */
  if ( bt != null ) {
    if ( bt.getState() == BluetoothChatService.STATE_CONNECTED ) {
      fill( 0, 255, 0 );
    } 
    else {
      noFill();
    }
    rect( 5, 5, 40, 40 );
  }
}

