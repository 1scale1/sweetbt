/** 
 * SweetBlue  Serial display
 Lists paired devices and connects to the first one on the list.
 Displays an incoming string from the serial connection.
 If it gets a newline, it clears the string
 
 Not working yet.
   
 created 27 June 2011
 by Tom Igoe
 
 */


import se.onescaleone.sweetblue.*;

//For GUI
String[] fontList;
PFont androidFont;
// instance of the library:
SweetBlue bt;
String inString = "";

void setup() {
  // Setup Fonts:
  fontList = PFont.list();
  androidFont = createFont(fontList[0], 8, true);
  textFont(androidFont, 24);
  textAlign(CENTER);
  
    // instantiate the library:
    bt = new SweetBlue( this );
    // get a list of paired devices:
    String[] pairedDevices = bt.list();
    if (pairedDevices.length > 0) {
      println(pairedDevices);
      // open a connection to the first one:
      bt.connect( pairedDevices[0] );
    } 
    else {
      text("Couldn't get any paired devices", 10, height/2);
    }
}

void draw() {
  // black with a nice light blue text:
  background(0);
  fill(#D3C7FE);
  char inByte = 0;    // byte you'll read from serial connection

  // if you're connected, check for any incoming bytes:
  if ( bt != null ) {
    if ( bt.isConnected() ) {
       // put the connected device's name on the screen:
      text("connected to" + bt.getName(), 10, screenHeight/3);
      // if there are incoming bytes available, read one:
      if (bt.available() > 0) {
        inByte = char(bt.read());
        if (inByte == '\n') {
          inString = "";
        } 
        else {
          inString += inByte;
        }
      }
      // display the latest center screen:
      text(inString, screenWidth/2, screenHeight/2);
    }
  }
}

// disconnect causes a fatal error
//void onPause() {
//  bt.disconnect();
// super.onPause(); 
//}


