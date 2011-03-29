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

/* Processing UI */
int[] pos = new int[] { 
  0, 400
};
int[] dim = new int[] { 
  50, 200
};
boolean sliding = false;

long timer = 0;

void setup() {
  /* Connect to the ArduinoBT */
  if ( bt == null ) {
    bt = new SweetBlue( this );
     bt.connect( "00:07:80:82:1E:BC"/* ArduinoBT MAC */    );
  }

  /* Lock PORTRAIT view */
  orientation( PORTRAIT );
  
  SweetBlue.DEBUG = true;
}
boolean initiated = false;

void draw() {
  if ( bt.isConnected() && !initiated ) {
    bt.pinMode( 11, SweetBlue.OUTPUT );
    initiated = true;
    timer = millis();
  }

  background( 160 );

  /* Draw UI */
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
        bt.pinMode( 11, SweetBlue.OUTPUT );
      }
    }
    else if ( event.getAction() == MotionEvent.ACTION_MOVE ) {
      if ( sliding ) {
        /* Set slider value */
        pos[0] = constrain( (int)event.getX(), 50, screenWidth - 50 );
        
        /* Send to bluetooth */
        if( millis() - timer > 50 ){
          timer = millis();
          bt.analogWrite( 11, (int)map(pos[0], 50, screenWidth - 50, 0, 255) );
        }
      }
    }
    else if ( event.getAction() == MotionEvent.ACTION_UP ) {
      /* Stop sliding */
      sliding = false;
      
      
     // bt.digitalWrite( 11, 0 );
      bt.pinMode( 11, SweetBlue.INPUT );
    }
  }
  return super.surfaceTouchEvent( event );
}

