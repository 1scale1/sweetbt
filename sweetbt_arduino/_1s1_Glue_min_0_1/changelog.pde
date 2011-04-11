/*

 * CHANGELOG FOR GLUE
 *
 * GLUE is the name of the protocol for communicating ArduinoBT boards with Android devices at the application level
 * it was developed by A. Goransson (on the Android side) and D. Cuartielles (on the Arduino side) and includes a protocol
 * with error detection via a simple checksum and detection of communication breakage by means of a timer
 *
 * All the GLUE messages start with a 0xFF 0xFF sequence to make sure the communication is opened there, and continue with a
 * series of bytes that indicate request type, length of the data field, the data itself, and the checksum byte.
 *
 * Even if BT communication is reasonably safe at low level, we have identified the need of a protocol like this one, that will
 * handle low level requests and ensure the communication between both ends.
 *
 * On the Arduino end, there is a state machine making sure the communication works, reseting the BT chipset otherwise
 *

 * rev 0.1:
   - added NewSoftwareSerial on pins 2 and 4 to allow debugging of the communication with the computer
   - fixed a problem with analogRead, it seems that analogReference breaks if updated too often

 */
