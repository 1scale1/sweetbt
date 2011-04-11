/*
 * SWEET GLUE
 *
 * /

SWEET is the name of a Processing library that instantiates basic Arduino commands and sends them up to an Arduino BT over the air. It requires the Arduino BT board to be ready to listen to the communication. The first version is made for the Arduino BT to become communication master what will allow the creation of localized wireless information points that could be accessed with BT enabled devices.

We envision SWEET GLUE to be used in prototyping objects for the IoT where the users can carry their own touch-based devices and use them to control, program, and interact with objects existing in the environment. Our library links Android phones with Arduino BT boards and our suggested development tool is Processing for Android.

GLUE is the name of the protocol for communicating Arduino BT boards with Android devices at the application level and includes a protocol with error detection via a simple checksum and detection of communication breakage by means of a timer.

All the GLUE messages start with a 0xFF 0xFF sequence to make sure the communication is opened there, and continue with a series of bytes that indicate request type, length of the data field, the data itself, and the checksum byte.

Even if BT communication is reasonably safe at low level, we have identified the need of a protocol like this one, that will handle low level requests and ensure the communication between both ends.

On the Arduino end, there is a state machine making sure the communication works, reseting the BT chipset otherwise.

WHO IS BEHIND THIS?

SWEET GLUE was developed by A. Goransson (on the Android side) and D. Cuartielles (on the Arduino side), the examples were made by both of them and the libraries were tested at first by D. Sjunnesson. You can find more information about this library together with examples at: http://1scale1.com/sweet

(c) 2011 1scale1.com, this code is licensed under a GPLv3 license, for more information visit:
http://www.gnu.org/licenses/gpl-3.0.txt

