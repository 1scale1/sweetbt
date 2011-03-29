package processing.android.test.digitalwrite;

import processing.core.*; 
import processing.xml.*; 

import se.onescaleone.sweetblue.*; 

import se.onescaleone.sweetblue.*; 

import android.view.MotionEvent; 
import android.view.KeyEvent; 
import android.graphics.Bitmap; 
import java.io.*; 
import java.util.*; 

public class digitalWrite extends PApplet {

/** 
 * SweetBlue - Digital Write
 * by Andreas G\u00f6ransson. 
 * 
 * Desc...
 * 
 * Note: Make sure to enable the BLUETOOTH and BLUETOOTH_ADMIN Sketch
 * Permissions.
 */





/* Library obj. */
SweetBlue bt;

/* Processing UI */
int[] pos = new int[] { 
  50, 50
};
int[] dim = new int[] { 
  200, 400
};

boolean btnstate = false;

boolean initiated = false;

public void setup() {
  /* Connect to the ArduinoBT */
  if ( bt == null ) {
    bt = new SweetBlue( this );
     bt.connect( "00:07:80:82:1E:BC"/* ArduinoBT MAC */    );
  }

  /* Lock PORTRAIT view */
  orientation( PORTRAIT );

  SweetBlue.DEBUG = true;
}

public void pinModes() {
  println("processings pinmodes...");
}

public void draw() {
  if ( bt.isConnected() && !initiated ) {
    bt.pinMode( 11, SweetBlue.OUTPUT );
    initiated = true;
  }

  background( 160 );

  /* Draw UI */
  fill( (btnstate ? 0 : 255 ) );
  rect( pos[0], pos[1], dim[0], dim[1] );
}

public boolean surfaceTouchEvent( MotionEvent event ) {
  /* Make sure to only interact with this element if bluetooth is connected */
  if ( bt.isConnected() ) {
    if ( event.getAction() == MotionEvent.ACTION_UP ) {
      /* Change btn state */
      btnstate = !btnstate;

      /* Write BT */
      if ( btnstate )
        bt.digitalWrite( 11, SweetBlue.HIGH );
      else
        bt.digitalWrite( 11, SweetBlue.LOW );
    }
  }
  return super.surfaceTouchEvent( event );
}


}
