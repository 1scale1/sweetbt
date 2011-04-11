/*
 * Glue protocol for Arduino BT to talk to Android phones
 *
 * this Arduino sketch will make Arduino talk using the so called Glue
 * protocol as designed by 1scale1. This protocol encapsulates data to
 * allow error detection when sending data over the air to another device
 *
 * the protocol has been customized to support the four basic Arduino
 * commands: digitalRead/Write, analogRead/Write
 *
 * this is not designed for power efficiency but for prototyping, therefore
 * the protocol tries to keep the BT connection opened by sending ping
 * over the air to the other device. If there was a series of NACKs to
 * that message, the processor would reset the BT chipset
 *
 * (c) 2011 D. Cuartielles & A. Goransson, 1scale1.com
 *
 * This code is free software and is licensed under GPLv3.0, for more
 * information about this license, please visit:
 * http://www.gnu.org/licenses/gpl-3.0.txt
 *
 */

// LOAD LIBRARIES
#include <NewSoftSerial.h>
NewSoftSerial mySerial(2, 4); // these pins aren't used by the tinkerkit
#define SERDEBUG 0

// COMMAND RELATED INFO
// commands for the overall loop
#define VERSION      0x00
#define TEST         0x01
#define GENERIC      0x02
#define ECHO         0x03
#define SYNCH        0x08
#define BT_STOP      0x05

// BT state machine
#define BT_TIMEOUT      5000  // time for BT reset
#define BT_ERROR_MAX    3     // amount of errors to be accounted before BT reset 
#define BT_RESET_PIN    7
#define BT_ST_DISCOVERABLE  0
#define BT_ST_RUNNING       1
byte btErrors = 0;
unsigned long timer = 0;
byte btStatus = BT_ST_DISCOVERABLE;

// Arduino commands
#define PINMODE      0x01
#define DIGITALREAD  0x02
#define DIGITALWRITE 0x03
#define ANALOGREAD   0x04
#define ANALOGWRITE  0x05

// LEDs for debugging purposes
#define O0   3
#define O1   5
#define O2   6
#define O3   9
#define O4   10
#define O5   11

// we define different pins to act as information LED pins depending on the board we use
int ledPin = O3;
int gndPin = 0;

boolean status = false;
boolean blueStatus = false;

// error message
#define MSG_ERROR        0xFF    // value to report errors back to the phone
#define MSG_SYNCH        0x01    // value to send a synch request command to phones

byte theData[] = {4, 3, 0};

void sendByte(byte data) {
  Serial.print(data, BYTE);
//  delayMicroseconds(10);
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
  int reading = 0;
  int lowB = 0;
  int highB = 0;
  
  if(SERDEBUG) {
    mySerial.print("Arduino Command sent: ");
    mySerial.println((char)Data[0]+48);
    mySerial.print("Pin: ");
    mySerial.println((char)Data[1]+48);
    mySerial.print("Initial value: ");
    mySerial.println((char)Data[2]+48);
  }
    
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
  analogReference(DEFAULT);
    if (Data[1] <= 5) {
//  toggleLed();
//XXX analog readings are failing, we don't know why ... this needs to be researched further
      reading = analogRead(Data[1]);
 mySerial.println(reading);
      lowB = reading & 0x007F;
      highB = reading >> 7;
      val = highB * 256 + lowB;
    }
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
  sendByte(0xFF); sendByte(0xFF); sendByte(GENERIC); sendByte(0x04);
  // the values
  sendByte(Data[0]); sendByte(Data[1]); sendByte((val >> 8) & 0xFF); sendByte(val & 0xFF); 
  // the checksum
  sendByte(Data[0] ^ Data[1] ^ ((val >> 8) & 0xFF) ^ (val & 0xFF) ^ GENERIC ^ 0x04);
  
  if(SERDEBUG) {
    mySerial.print("Result sent: ");
    mySerial.println(reading);
    mySerial.print("Result decomposed: ");
    mySerial.print((val >> 8) & 0xFF);
    mySerial.print(" - ");
    mySerial.println(val & 0xFF);
  }
    
  return 0;
}

void sendSynch() {
  // send synch information back to the other device
  // the headers
  sendByte(0xFF); sendByte(0xFF); sendByte(SYNCH); sendByte(0x04);
  // the values
  sendByte(MSG_SYNCH); sendByte(0); sendByte(0); sendByte(0); 
  // the checksum
  sendByte(MSG_SYNCH ^ SYNCH ^ 0x04);
}

// function to process commands coming from the port
byte ProcessCommand(byte *Data)
{
  byte length = 0;
  byte checksum = 0;
  byte Command = 0;

  waitForMessage(); // Receive the header byte: should be 0xFF
  Command = GetByte(); // Receive the command

  // here we are assuming that we are getting the command to take data and push it to the TLCs
  // it is possible to implement other commands with no problem and then we should just discriminate
  // according to their code. Let's say that pushing data to the TLC is command #1 (0x01)

  length = GetByte(); // Receive the amount of data being sent (in this case how many motors to control)

  checksum = length ^ Command;
  // get all the data
  for (int i=0; i<length; ++i)
  {
    Data[i] = GetByte();
  }

  byte recChecksum = GetByte();

  // clean the data from the inversion bit
  // this means to substract 128 to the odd indexes in the 
  // data chain and respect the even ones
  
  // calculate checksum based on received data
  for (int i=0; i<length; ++i)
  {
    checksum ^= Data[i];
  }

  if (checksum != recChecksum)
  {

//    Serial.print(checksum, HEX);

    return 0; // checksum failure!
  }

  if(SERDEBUG) {
    mySerial.print("Command sent: ");
    mySerial.println((char)Command+48);
  }
  
  switch (Command)
  {
    case GENERIC: 
      processArduinoCMD(length, Data);
      btStatus = BT_ST_RUNNING;
      timer = millis();
      btErrors = 0;
      break;
      
    case SYNCH: 
      timer = millis();
      btErrors = 0;
      btStatus = BT_ST_RUNNING;
     break;
      
    case BT_STOP: 
      timer = millis();
      btErrors = 0;
      btReset();
      break;
      
    default:
      break;
  }

  return 1;
}

void blinkLed(int ledPin, int time) 
{
  digitalWrite(ledPin, HIGH);
  delay(time);
  digitalWrite(ledPin, LOW);
  delay(time);
}

void toggleLed()
{
  digitalWrite(ledPin, status);
  status = !status;
}

void toggleBlueLed()
{
  digitalWrite(O2, blueStatus);
  blueStatus = !blueStatus;
}

byte GetByte() // helper function to read a byte from serial
{
  while (!Serial.available());
  byte theByte = Serial.read();

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

  return serdata;
}

void btReset() {
  digitalWrite(BT_RESET_PIN, HIGH);
  delay(200);
  digitalWrite(BT_RESET_PIN, LOW);
  btStatus = BT_ST_DISCOVERABLE;
}

void setup() {
  // preconfigure the BT reset pin
  pinMode(BT_RESET_PIN, OUTPUT);
  digitalWrite(BT_RESET_PIN, LOW);
  
  pinMode(ledPin, OUTPUT);

  // initialize the timer for the state machine
  timer = millis();
  
  Serial.begin(115200);

  if(SERDEBUG) {
    // set the data rate for the NewSoftSerial port
    mySerial.begin(4800);
    mySerial.println("Debug Monitor V0.1");
  }

}

void loop() {
  // check commands if there is data available on Serial
  if (Serial.available() > 0)
    if (ProcessCommand(theData)) {
    //  toggleBlueLed();
    }
    
  // decide when to reset based on the amount of timeouts
  if (btStatus == BT_ST_RUNNING) {
    if (millis() - timer >= BT_TIMEOUT) {
      btErrors++;
      if (btErrors > BT_ERROR_MAX) {
        btReset();
      } else {
        sendSynch();
        timer = millis();
      }
    }
  } else {
    btErrors = 0;
    timer = millis();
  }
}

