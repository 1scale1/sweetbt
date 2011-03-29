/** 
 * SweetBlue - Analog Read
 * by Andreas Göransson. 
 * 
 * This sketch connects to ArduinoBT and rotates a shape on the
 * processing sketch depending on the value read from an analog
 * sensor on the ArduinoBT.
 * 
 * Note: Make sure to enable the BLUETOOTH and BLUETOOTH_ADMIN Sketch
 * Permissions.
 */


import se.onescaleone.sweetblue.*;

/* Library obj. */
SweetBlue bt;

/* Rotational value */
int[] val = new int[1];

void setup() {
  /* Connect to the ArduinoBT */
  if ( bt == null ) {
    bt = new SweetBlue( this );
     bt.connect( "00:07:80:82:1E:BC"/* ArduinoBT MAC */ );
  }

  /* Lock PORTRAIT view */
  orientation( PORTRAIT );

  /* Simplifies position and rotation */
  rectMode( CENTER );

  SweetBlue.DEBUG = false;
}

boolean initiated = false;

void draw() {
  if ( bt.isConnected() && !initiated ) {
    bt.pinMode( 14, SweetBlue.INPUT );
    initiated = true;
  }

  /* Draw background */
  background( (val[0] == SweetBlue.HIGH) ? 255 : 0 );

  
  /* Read value from ArduinoBT */
  if ( bt.isConnected() && millis() % 10 == 0) {
    bt.digitalRead( 14, val );
  println( val[0] );
  }
}

void stop(){
  bt.close();
}

