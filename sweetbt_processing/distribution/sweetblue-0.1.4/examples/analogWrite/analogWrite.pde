/** 
 * SweetBlue - Analog Write
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

/* Slider UI */
int[] pos = new int[] { 
  0, 400
};
int[] dim = new int[] { 
  50, 200
};
boolean sliding = false;


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

  //timer = millis();
}

void draw() {
  /* Draw the background */
  background( 126 );

  /* Draw Slider UI */
  fill( (sliding ? 0 : 255 ) );
  rect( pos[0], pos[1], dim[0], dim[1] );
}

boolean surfaceTouchEvent( MotionEvent event ) {
  /* Make sure to only interact with this element if bluetooth is connected */
  if ( bt.isConnected() ) {
    if ( event.getAction() == MotionEvent.ACTION_DOWN ) {
      /* Try to initiate the sliding */
      if ( (event.getX() > pos[0] && event.getX() < (pos[0] + dim[0])) && 
        (event.getY() > pos[1] && event.getY() < (pos[1] + dim[1])) ) {
        /* Selected, set sliding to true */
        sliding = true;
        bt.pinMode( PIN, SweetBlue.OUTPUT );
      }
    }
    else if ( event.getAction() == MotionEvent.ACTION_MOVE ) {
      if ( sliding ) {
        /* Set slider value */
        pos[0] = constrain( (int)event.getX(), 50, screenWidth - 50 );

        /* Send to bluetooth, but only if enough time has passed */
        if ( millis() - timer >= timerdelay ) {
          bt.analogWrite( PIN, (int)map(pos[0], 50, screenWidth - 50, 0, 255) );
          /* Reset timer */
          timer = millis();
        }
      }
    }
    else if ( event.getAction() == MotionEvent.ACTION_UP ) {
      /* Stop sliding */
      sliding = false;
      
      /* Reset the pinmode */
      bt.pinMode( PIN, SweetBlue.INPUT );
    }
  }
  return super.surfaceTouchEvent( event );
}

