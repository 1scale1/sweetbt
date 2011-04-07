/** 
 * SweetBlue - Digital Write
 * by Andreas Göransson. 
 * 
 * Desc...
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
int PIN = 0;

/* UI Button */
int[] pos = new int[] { 
  50, 50
};
int[] dim = new int[] { 
  200, 400
};

boolean btnstate = false;

void setup() {
  /* Connect to the ArduinoBT */
  if ( bt == null ) {
    bt = new SweetBlue( this );
     bt.connect( "00:07:80:82:1F:AC" /* ArduinoBT MAC */ );
  }

  /* Lock PORTRAIT view */
  orientation( PORTRAIT );

  /* Enable debug messages? */
  //SweetBlue.DEBUG = true;

  //timer = millis();
}

void draw() {
 if ( bt.isConnected() && !initiated ) {
    /* Once the board has established a connection, set the pin modes... */
    bt.pinMode( PIN, SweetBlue.OUTPUT );
    /* ...but only do it once! */
    initiated = true;
  }
  
  /* Draw the background */
  background( 160 );

  /* Draw Button UI */
  fill( (btnstate ? 0 : 255 ) );
  rect( pos[0], pos[1], dim[0], dim[1] );
}

boolean surfaceTouchEvent( MotionEvent event ) {
  /* Make sure to only interact with this element if bluetooth is connected */
  if ( bt.isConnected() ) {
    if ( event.getAction() == MotionEvent.ACTION_UP ) {
      /* Change btn state */
      btnstate = !btnstate;

      /* Write BT */
      if ( btnstate )
        bt.digitalWrite( PIN, SweetBlue.HIGH );
      else
        bt.digitalWrite( PIN, SweetBlue.LOW );
    }
  }
  return super.surfaceTouchEvent( event );
}

/* When processing stops, send the disconnect command to ArduinoBT */
void stop() {
  bt.close();
}


