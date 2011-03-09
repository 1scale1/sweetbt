/* 
  Glue - a Protocol for communicating Arduino BT to Android devices
   
  At 1scale1 we have today managed to make Glue as an addon to Processing, which means we can
  send Processing sketches including BT communication to an Android phone straight from 
  Processing 1.9.1. By adding Firmata functionality to Glue we aim for a standarization with the
  rest of the Arduino-Processing ecosystem.
  
  Goal: design your GUI with Processing, push it to your phone, control the world with Arduino!

 (c) 2011 David Cuartielles for 1scale1, this code is Free Software, it is licensed unde GPLv3
     for more information about the license, please visit: http://www.gnu.org/licenses/gpl-3.0.html
 
 */

// OUTPUT TYPES
#define TLC 0
#define SWEET_SHIELD 1
#define RAW_BT 2
#define DMX_SHIELD 3
byte outputType = RAW_BT;

// PROTOCOL CONFIGURATION
boolean ALTERNATE_BYTES = false;

// PIN DEFINITIONS AND THE LIKE
// for the TLC
#include "Tlc5940.h"
// for the SWEET_SHIELD
#define O0 3
#define O1 5
#define O2 6
#define O3 9
#define O4 10
#define O5 11
// for the RAW_BT
// we will use the standard D0-D13 and A0-A5 definitions from Arduino's core
// for the DMX_SHIELD

// DEBUG LEVELS (flags)
// 0 - nothing, production mode
// 1 - echo just the commands back
// 2 - echo the commands, but also the explanations to them 
int VERBOSE = 0;
//#define DEBUG
//#define ECHO

// COMMAND RELATED INFO
// commands for the overall loop
#define VERSION      0x00
#define TEST         0x01
#define GENERIC      0x02
#define ECHO         0x03

// Arduino commands
#define PINMODE      0x01
#define DIGITALREAD  0x02
#define DIGITALWRITE 0x03
#define ANALOGREAD   0x04
#define ANALOGWRITE  0x05

// error message
#define ERROR        0xFF

// PROTOCOL INFO
#define NAME "FirmWear"
#define VER "0.3.3"
#define DATA_ARRAY_LENGTH 80
// toggle LED only works for TLC and SWEET_SHIELD modes
boolean TOGGLE_LED = true;

// VARIABLE DEFINITIONS

// we define different pins to act as information LED pins depending on the board we use
int ledPin = 0;
int gndPin = 0;

byte channels[] = {
  0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

boolean status = false;
byte Data[DATA_ARRAY_LENGTH];

void defineChannelArray() {
  switch (outputType) {
  case TLC:
    break;
  case SWEET_SHIELD:
    channels[0] = O0;
    channels[1] = O1;
    channels[2] = O2;
    channels[3] = O3;
    channels[4] = O4;
    channels[5] = O5;
    break;
  case RAW_BT:
    for (int i = 2; i <= sizeof(channels); i++)
      channels[i] = i;
    break;
  case DMX_SHIELD:
    break;
  }
}

void setup()
{
  defineChannelArray();
  
  switch (outputType) {
  case TLC:
    ledPin = 5;
    gndPin = 6;
    /* Call Tlc.init() to setup the tlc.
     You can optionally pass an initial PWM value (0 - 4095) for all channels.*/
    Tlc.init();
    pinMode(ledPin, OUTPUT);
    pinMode(gndPin, OUTPUT); 
    digitalWrite(gndPin, LOW);
    break;

  case SWEET_SHIELD:
    ledPin = 13;
    for(int i = 0; i < 6; i++) 
      pinMode(channels[i], OUTPUT);
    pinMode(ledPin, OUTPUT);
    break;

  case RAW_BT:
    for(int i = 2; i < sizeof(channels); i++) 
      pinMode(channels[i], OUTPUT);
    break;

  case DMX_SHIELD:
    break;
  }

  // initialize the serial port at maximum speed
  Serial.begin(115200);

#ifdef DEBUG
  Serial.print(NAME);
  Serial.print(" ");
  Serial.println(VER);
  Serial.println("Ready ...");
#endif

#ifdef TOGGLE_LED
  toggleLed(); 
  delay(200); 
  toggleLed();
#endif
}

void loop()
{
  if (ProcessCommand(Data)) toggleLed();
}

void toggleLed()
{
  digitalWrite(ledPin, status);
  status = !status;
}

byte GetByte() // helper function to read a byte from serial
{
  while (!Serial.available());
  byte theByte = Serial.read();

#ifdef DEBUG
  Serial.print(theByte, BYTE);
#endif

  return theByte;
}

byte waitForMessage() // waits until it gets 0xFF from the serial port
{
  int count = 0;
  byte serdata = 0;
  while ( count < 2 ) 
  {
    serdata = GetByte();
    // increase the counter only if we get the marker 0xFF
    // if we got one marker and then something else, we were in
    // the middle of a string
    if (serdata == 0xFF) count++;
    else count = 0;
  }

#ifdef DEBUG
  Serial.println(" - Marker");
#endif

#ifdef ECHO
  Serial.print(0x00, BYTE);
#endif

  return serdata;
}


// introduce the TLC related functions
void testTLC()
{
  int direction = 1;
  for (int channel = 0; channel < NUM_TLCS * 16; channel += direction) {

    /* Tlc.clear() sets all the grayscale values to zero, but does not send
     them to the TLCs.  To actually send the data, call Tlc.update() */
    Tlc.clear();

    /* Tlc.set(channel (0-15), value (0-4095)) sets the grayscale value for
     one channel (15 is OUT15 on the first TLC, if multiple TLCs are daisy-
     chained, then channel = 16 would be OUT0 of the second TLC, etc.).
     
     value goes from off (0) to always on (4095).
     
     Like Tlc.clear(), this function only sets up the data, Tlc.update()
     will send the data. */
    if (channel == 0) {
      direction = 1;
    } 
    else {
      Tlc.set(channel - 1, 1000);
    }
    Tlc.set(channel, 4095);

    /* Tlc.update() sends the data to the TLCs.  This is when the LEDs will
     actually change. */
    Tlc.update();

    delay(75);
  }

}


byte sendTLC(int length, byte *Data)
{
  /* Tlc.clear() sets all the grayscale values to zero, but does not send
   them to the TLCs.  To actually send the data, call Tlc.update() */
  Tlc.clear();
  for (int channel = 0; channel < length; channel++) {
    Tlc.set(channel, map(Data[channel],0,127,0,4095));
  }
  /* Tlc.update() sends the data to the TLCs.  This is when the LEDs will
   actually change. */
  Tlc.update();
}

void clear()
{
  for(int i = 0; i < 6; i++) 
    digitalWrite(channels[i], LOW);
}

void set(byte channel, int value)
{
  analogWrite(channels[channel], value);
}

void testOUTPUTS()
{
  int direction = 1;
  for (int channel = 0; channel < 6; channel += direction) {

    /* clear() sets all the grayscale values to zero */
    clear();

    /* set(channel (0-5), value (0-256)) sets the grayscale value for
     one channel.
     
     value goes from off (0) to always on (256). */
    if (channel == 0) {
      direction = 1;
    } 
    else {
      set(channel - 1, 30);
    }
    set(channel, 255);

    delay(200);
  }

}

byte sendOUTPUTS(int length, byte *Data)
{
  /* clear() sets all the grayscale values to zero */
  clear();
  for (int channel = 0; channel < length; channel++) {
    set(channel, map(Data[channel],0,127,0,255));
  }
}

// this function is used when calling to low level commands on the Arduino BT board, considering it will not
// be used in any other way, we need to disable access to pins 0, 1, 7 (RX/TX and BT reset)
// we will always answer back using the format 0xFF 0xFF 0x02 0x04 AA PP XX YY CC, where
// - AA: Arduino command
// - PP: pin number
// - XX: 2 MSb result from a reading, 0xFF if error
// - YY: 8 LSb result from a reading, 0 otherwise
// - CC: checksum
byte processArduinoCMD(int length, byte *Data)
{
  int val = 0;
  switch (Data[0]) {
    // pinMode
  case PINMODE:
    if (Data[2]) val = INPUT;
    else val = OUTPUT;
    if (Data[1] != 7 && Data[1] != 0 && Data[1] != 1) pinMode(Data[1], val);
    else val = 0xFF00;
    break;

    // digitalRead
  case DIGITALREAD:
    if (Data[1] != 7 && Data[1] != 0 && Data[1] != 1) val = digitalRead(Data[1]);
    else val = 0xFF00;
    break;

    // digitalWrite
  case DIGITALWRITE:
    if (Data[1] != 7 && Data[1] != 0 && Data[1] != 1) digitalWrite(Data[1], Data[2]);
    else val = 0xFF00;
    break;

    // analogRead
  case ANALOGREAD:
    if (Data[1] != 7 && Data[1] != 0 && Data[1] != 1) val = analogRead(Data[1]);
    else val = 0xFF00;
    break;

    // analogWrite
  case ANALOGWRITE:
    if (Data[1] != 7 && Data[1] != 0 && Data[1] != 1) analogWrite(Data[1], Data[2]);
    else val = 0xFF00;
    break;

  }

  // send the information back to the other device
  // the headers
  Serial.print(0xFF, BYTE); Serial.print(0xFF, BYTE); Serial.print(GENERIC, BYTE); Serial.print(0x04, BYTE);
  // the values
  Serial.print(Data[0], BYTE); Serial.print(Data[1], BYTE); Serial.print((val >> 8) & 0xFF, BYTE); Serial.print(val & 0xFF, BYTE); 
  // the checksum
  Serial.print(Data[0] ^ Data[1] ^ ((val >> 8) & 0xFF) ^ (val & 0xFF) ^ GENERIC ^ 0x04, BYTE);
  
  return 0;
}

// function to process commands coming from the port
byte ProcessCommand(byte *Data)
{
  byte length = 0;
  byte checksum = 0;
  byte Command = 0;

  waitForMessage(); // Receive the header byte: should be 0xFF
  Command = GetByte(); // Receive the command

#ifdef DEBUG
  Serial.println(" - Command");
#endif

  // here we are assuming that we are getting the command to take data and push it to the TLCs
  // it is possible to implement other commands with no problem and then we should just discriminate
  // according to their code. Let's say that pushing data to the TLC is command #1 (0x01)

  length = GetByte(); // Receive the amount of data being sent (in this case how many motors to control)

#ifdef DEBUG
  Serial.println(" - Length");
#endif

  if(VERBOSE == 1) {
    Serial.print(0xFF);
    Serial.print(0xFF);
    Serial.print(Command);
    Serial.print(length);
  }

  checksum = length ^ Command;
  // get all the data
  for (int i=0; i<length; ++i)
  {
    Data[i] = GetByte();
    if (VERBOSE == 1)
      Serial.print(Data[i]);
  }

  byte recChecksum = GetByte();
  if (VERBOSE == 1)
    Serial.print(recChecksum);

  // clean the data from the inversion bit
  // this means to substract 128 to the odd indexes in the 
  // data chain and respect the even ones
  if (ALTERNATE_BYTES)
    for (int i=1; i<length; i=i+2)
    {
      Data[i] -= 128;
    } 

  // calculate checksum based on received data
  for (int i=0; i<length; ++i)
  {
    checksum ^= Data[i];
  }

#ifdef DEBUG
  Serial.println(" - Data");
#endif

  if (checksum != recChecksum)
  {

#ifdef DEBUG
    Serial.println(" - Checksum Error");
#endif

    Serial.print(checksum, HEX);

#ifdef DEBUG
    Serial.println();
#endif

    return 0; // checksum failure!
  }

#ifdef DEBUG
  Serial.println(" - Checksum Correct");
#endif

  switch (Command)
  {
    // the basic test is just echoing things back over the port reporting on the protocol's version
  case VERSION: 
    Serial.print(NAME);
    Serial.print(" ");
    Serial.println(VER);

    if (TOGGLE_LED) {
      toggleLed(); 
      delay(200); 
      toggleLed(); 
    }
    break;

    // the basic test is just echoing things back over the port reporting on the protocol's version
  case TEST: 

#ifdef DEBUG
    Serial.println(" - basic OUTPUT test");
#endif

    switch (outputType) {
    case TLC:
      testTLC();
      break;

    case SWEET_SHIELD:
      testOUTPUTS();
      break;
    }
    break;

  case GENERIC: 

#ifdef DEBUG
    Serial.println(" - resend to TLC");
#endif

    switch (outputType) {
    case TLC:
      sendTLC(length, Data);
      break;

    case SWEET_SHIELD:
      sendOUTPUTS(length, Data);
      break;

    case RAW_BT:
      processArduinoCMD(length, Data);
      break;
    }
    break;

  case ECHO:
    // send the message marker
    Serial.print(0xFF, HEX);

    // send the whole thing back to the phone 
    Serial.print(Command, HEX); 
    Serial.print(length, HEX); 

    // here the data
    for (int i=0; i<length; ++i)
    {
      Serial.print(Data[i], HEX);
    }
    break;

  default:

#ifdef DEBUG
    Serial.print(" - command: ");
    Serial.println(Command, HEX);
#endif

    break;
  }

  Serial.print(checksum, HEX);

#ifdef DEBUG
  Serial.println();
#endif

  return 1;
} 


