/*

20110224, K3 - School of Arts, MMX, Sweden

Next Goal: Adding Firmata on top of Glue. Glue has proven to be useful as a way to communicate 
over bluetooth and the error correction seems meaningful. Now we are interested in a 
version that includes bidirectional communication and compatibility with Processing sketches.

At 1scale1 we have today managed to make Glue as an addon to Processing, which means we can
send Processing sketches including BT communication to an Android phone straight from 
Processing 1.9.1. By adding Firmata functionality to Glue we aim for a standarization with the
rest of the Arduino-Processing ecosystem.

Goal: design your GUI with Processing, push it to your phone, control the world with Arduino!

 * rev 0.3.3 changelog:
 * cleaned the way messages are inserted by the precompiler
 * made many of the commands as variables in order to allow hot-modifications
 * included the Alternate bytes deactivation (seems to provoke problems sometimes)
 * included Verbose modes
 
 * rev 0.3.2 changelog:
 * merge with the possibility of controlling DMX (NOT ADDED)
 * added the basic pin control protocol and the possibily of sending data back to a Processing App on an Android phone
 
 * rev 0.3.1 changelog:
 * added the possibility to disconnect the byte alternation
 
 * on DEBUG it will echo back every byte received
 * rev 0.3 changelog:
 
 + added functions for the non-TLC scenario of use to be used with Arduino BT
 + includes inversion bit at every second byte in the data chain
 + commands are marked with a double 0xFF in the beginning
 
*/

/*

20100716, CDG airport, Paris, France

David Cuartielles originally for the Stahle project, modified to be part of the 1s1 research 
project on wearables

Title:

How to fix the communication problem between Arduino BT and Nexus ONE

Problem:

Data get's lost or receiver gets confused on arrival of subsequent chains of data that contain 
the same byte. It seems to happen when sending data at full speed from the phone to the Arduino BT. 
In test with short data chains from the computer's BT connection, the Arduino board reports 100% 
accuracy in estimating the checksum error byte. 

Suggested solution:

Create a communication protocol that includes alternating the non-used 8th bit in the data chain. 
In this way a data chain with all zeroes would look like:

0xFF 0x02 0x50 0x00 0x80 0x00 0x80 0x00 0x80 ... 0x80 [CS]
  |    |    |    |                                 |    |
  |    |    |    +-------------DATA----------------+    |
  |    |    |                                          CHECKSUM
  |    |   LENGTH
  |    |
  |   COMMAND
  |
 INIT

This possibility offers a risk, which is getting 0xFF in the data chain, and to have start 
reading data in the middle of a data arrival. There is however a way for solving this which 
is to send the INIT byte twice in a row. Due to the 8th bit alternation it is impossible to 
get 0xFF twice in a row as part of the data chain. We know that the receiver will not mix up 
data when getting two bytes in a row, the problem shows up with long chains of data.

Therefore my suggestion is to implement the following protocol subsituting the one we have right now:

0xFF 0xFF 0x02 0x50 0x00 0x80 0x00 0x80 0x00 0x80 ... 0x80 [CS]
  |    |   |    |    |                                 |    |
  +-+--+   |    |    +-------------DATA----------------+    |
    |      |    |                                          CHECKSUM
    |      |   LENGTH
    |      |
    |     COMMAND
    |
   INIT
   
The Checksum:

My suggestion regarding the [CS] byte is to estimate it without taking the inversion into 
account. In this way we will be inverting the byte by just adding 128 (0x80) or by making 
the binary operation: data = data OR 0x80 every other byte. This trick should give us 100% 
accuracy in the transmission and is not adding a lot of overhead to the protocol.

This said, the Arduino program will obviously change. I attached the modified version, though 
I haven't got the chance to test it, since I had no Arduino board with me in the flight and 
I am now sitting at the airport.

*/

