
#include <SoftwareSerial.h>

/*
   Reads from four EMG sensors and translates the signal into
   boolean output. The signal is then sent over bluetooth to be
   used with the Accessible Virtual Keyboard application.

   @author Kristian Honningsvag.
*/

// Hardware pins.
const int EMG_SENSOR_PIN_0 = A0;
const int EMG_SENSOR_PIN_1 = A1;
const int EMG_SENSOR_PIN_2 = A2;
const int EMG_SENSOR_PIN_3 = A3;

// Variables.
int sensorValue0 = 0;
int sensorValue1 = 0;
int sensorValue2 = 0;
int sensorValue3 = 0;

boolean currentOutputValue0 = false;
boolean currentOutputValue1 = false;
boolean currentOutputValue2 = false;
boolean currentOutputValue3 = false;

boolean previousOutputValue0 = false;
boolean previousOutputValue1 = false;
boolean previousOutputValue2 = false;
boolean previousOutputValue3 = false;

unsigned long timer = 0;
unsigned int timeBetweenLogging = 800;   // Milliseconds.
int EMGTreshold = 900;   // When signal should activate.

SoftwareSerial BTSerial(0, 1);

/*
   Runs once on startup.
*/
void setup() {
  pinMode(EMG_SENSOR_PIN_0, INPUT);
  pinMode(EMG_SENSOR_PIN_1, INPUT);
  pinMode(EMG_SENSOR_PIN_2, INPUT);
  pinMode(EMG_SENSOR_PIN_3, INPUT);

  Serial.begin(9600);
  timer = millis();



  String setName = String("AT+NAME=AVIKEYB_INPUT\r\n");   // Setting name.
  Serial.begin(9600);
  BTSerial.begin(38400);
  BTSerial.print("AT\r\n");       // Check Status.
  delay(500);
  while (BTSerial.available()) {
    Serial.write(BTSerial.read());
  }
  BTSerial.print(setName);  // Send Command to change the name.
  delay(500);
  while (BTSerial.available()) {
    Serial.write(BTSerial.read());
  }

  //  Serial.println("SET BT NAME AVIKEYB_INPUT");
}


/*
   Loops indefinitely.
*/
void loop() {
  // Get the values from the EMG sensors.
  sensorValue0 = getEMGSignal(EMG_SENSOR_PIN_0);
  sensorValue1 = getEMGSignal(EMG_SENSOR_PIN_1);
  sensorValue2 = getEMGSignal(EMG_SENSOR_PIN_2);
  sensorValue3 = getEMGSignal(EMG_SENSOR_PIN_3);

  generateOutput();

  //  // Print all values.
  //  if ( (millis() - timer) >= timeBetweenLogging) {
  //    timer = millis();
  //    printAllSerial();
  //  }
}


/*
   Gets the current value from an EMG sensor.
*/
float getEMGSignal(int pin) {
  int sensorValue = analogRead(pin);
  //  float voltage = sensorValue * (5.0 / 1023.0);  // Convert to voltage (0-1023) -> (0-5)
  return sensorValue;
}


/*
   Sets the values of the output variables based on their corresponding sensor values.
*/
void generateOutput() {

  if (sensorValue0 > EMGTreshold) {
    previousOutputValue0 = currentOutputValue0;
    currentOutputValue0 = true;
  }
  else {
    previousOutputValue0 = currentOutputValue0;
    currentOutputValue0 = false;
  }
  if (currentOutputValue0 && !previousOutputValue0) {
    Serial.println(0);
  }

  if (sensorValue1 > EMGTreshold) {
    previousOutputValue1 = currentOutputValue1;
    currentOutputValue1 = true;
  }
  else {
    previousOutputValue1 = currentOutputValue1;
    currentOutputValue1 = false;
  }
  if (currentOutputValue1 && !previousOutputValue1) {
    Serial.println(1);
  }

}


/*
   Prints the current values in a readable format intended for the Arduino IDE serial monitor.
*/
void printAllSerial() {
  Serial.print("Time ");
  Serial.print(timer);
  Serial.print(" | EMG0 ");
  Serial.print(sensorValue0);
  Serial.print("-");
  Serial.print(currentOutputValue0);
  Serial.print(" | EMG1 ");
  Serial.print(sensorValue1);
  Serial.print("-");
  Serial.print(currentOutputValue1);
  Serial.print(" | EMG2 ");
  Serial.print(sensorValue2);
  Serial.print("-");
  Serial.print(currentOutputValue2);
  Serial.print(" | EMG3 ");
  Serial.print(sensorValue3);
  Serial.print("-");
  Serial.println(currentOutputValue3);
}

