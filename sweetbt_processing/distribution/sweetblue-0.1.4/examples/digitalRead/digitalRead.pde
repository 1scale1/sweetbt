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

/* Related to the communication */
boolean initiated = false;
long timer = 0;
long timerdelay = 100; // minimum amount of milliseconds between each reading

/* Pin where the sensor is connected */
int PIN = 17;

/* Variable to store read-values in (it has to be int array!) */
int[] val = new int[1];

void setup() {
  /* Connect to the ArduinoBT */
  if ( bt == null ) {
    bt = new SweetBlue( this );
    bt.connect( /* ArduinoBT MAC */ );
  }

  /* Lock PORTRAIT view */
  orientation( PORTRAIT );

  /* Enable debug messages? */
  //SweetBlue.DEBUG = true;

  timer = millis();
}

void draw() {
  if ( bt.isConnected() && !initiated ) {
    /* Once the board has established a connection, set the pin modes... */
    bt.pinMode( PIN, SweetBlue.INPUT );
    /* ...but only do it once! */
    initiated = true;
  }

  /* Draw background, black or white */
  background( (val[0] == SweetBlue.HIGH) ? 255 : 0 );

  /* Read value from ArduinoBT every "timerdelay" milliseconds */
  if ( bt.isConnected() && (millis() - timer >= timerdelay) ) {
    /* Read the digital pin, PIN, from the bluetooth board */
    bt.digitalRead( PIN, val );
    /* Reset the timer */
    timer = millis();
  }
}

/* When processing stops, send the disconnect command to ArduinoBT */
void stop() {
  bt.close();
}

