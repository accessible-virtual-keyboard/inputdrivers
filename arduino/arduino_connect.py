
import serial
import numpy
import matplotlib.pyplot as pyplot
import csv

# Open a log-file for writing data to.
logFile = open('log_file.csv','a')
logWriter = csv.writer(logFile)

# Connect to the Arduino.
try:
#    arduino = serial.Serial('/dev/ttyUSB0', 9600)
    arduino = serial.Serial('/dev/ttyACM0', 9600)
except:
    print("ERROR: Failed to connect to Arduino")

# Continuously listen for activity and write to file.
while True:
    while (arduino.inWaiting()==0):  #Wait here until there is data
        pass #Do nothing.
    arduinoByteString = arduino.readline()
    arduinoString = arduinoByteString.decode('utf-8')
    arduinoArray = arduinoString.strip().split(",")
    logWriter.writerow(arduinoArray)
    print(arduinoString, end="")
