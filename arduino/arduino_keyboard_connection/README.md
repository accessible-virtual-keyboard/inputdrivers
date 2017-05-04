
# Arduino keyboard connection

This script make the connection between the arduino microcontroller and the keyboard websocket interface. The script will simply
forward the output from the arduino to the keybaord over the websocket connection.


## Installation


To use this script you will need to install [python](https://www.python.org).

After installing python you need to install the python modules this script requires. This is done by issuing the following command in the terminal.

```
pip install -r requirements.txt
```
The "pip" command which is included with python. On windows you may have to include the "C:\[python_install_dir]\scripts" folder in your PATH environment variable, or use the absolute path to the pip script ("C:\[python_install_dir]\scripts\pip") for this command to work.

When the requirements has been installed you can run the script like this:
```
python arduino_keyboard_connection.py <the websocket url for the keyboard> <the path to the usb port the for the arduino>
```

For example:
```
python arduino_keyboard_connection.py ws://173.237.145.119:43879 COM5
```


## Protocol

The keybaord has simple boolean input types, for example input1, input2, input3 etc. To send these input types to the keyboard, 
the arduino sketch must use the following this protocol:

```
To send input type 1, simply print "1" using the Serial.println function.
To send input type 2, simply print "2" using the Serial.println function.
To send input type 3, simply print "3" using the Serial.println function.
etc.
```

That's it, as long as this simple protocol is used, this script should forward the input signals to the keyboard.

