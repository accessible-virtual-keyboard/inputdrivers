/*
   Reads from up to four EMG sensors and translates the signal
   into boolean output that can be used with the Accessible
   Virtual Keyboard application.

   @author Kristian Honningsvag.
*/

// Hardware pins.
const int EMG_SENSOR_PIN_0 = A0;
const int EMG_SENSOR_PIN_1 = A1;
const int EMG_SENSOR_PIN_2 = A2;
const int EMG_SENSOR_PIN_3 = A3;

const int OUTPUT_PIN_0 = 10;
const int OUTPUT_PIN_1 = 11;
const int OUTPUT_PIN_2 = 12;
const int OUTPUT_PIN_3 = 13;

// Variables.
int sensorValue0 = 0;
int sensorValue1 = 0;
int sensorValue2 = 0;
int sensorValue3 = 0;

boolean outputValue0 = false;
boolean outputValue1 = false;
boolean outputValue2 = false;
boolean outputValue3 = false;

unsigned long timer = 0;
unsigned int timeBetweenLogging = 200;   // Milliseconds.
int EMGTreshold = 900;   // When signal should activate.


/*
   Runs once on startup.
*/
void setup() {
  pinMode(EMG_SENSOR_PIN_0, INPUT);
  pinMode(EMG_SENSOR_PIN_1, INPUT);
  pinMode(EMG_SENSOR_PIN_2, INPUT);
  pinMode(EMG_SENSOR_PIN_3, INPUT);

  pinMode(OUTPUT_PIN_0, OUTPUT);
  pinMode(OUTPUT_PIN_1, OUTPUT);
  pinMode(OUTPUT_PIN_2, OUTPUT);
  pinMode(OUTPUT_PIN_3, OUTPUT);

  digitalWrite(OUTPUT_PIN_0, outputValue0);
  digitalWrite(OUTPUT_PIN_1, outputValue1);
  digitalWrite(OUTPUT_PIN_2, outputValue2);
  digitalWrite(OUTPUT_PIN_3, outputValue3);

  Serial.begin(9600);
  timer = millis();
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

  // Set the output values.
  setOutput(OUTPUT_PIN_0, sensorValue0, outputValue0);
  setOutput(OUTPUT_PIN_1, sensorValue1, outputValue1);
  setOutput(OUTPUT_PIN_2, sensorValue2, outputValue2);
  setOutput(OUTPUT_PIN_3, sensorValue3, outputValue3);

  // Print all values.
  if ( (millis() - timer) >= timeBetweenLogging) {
    timer = millis();
    printAllSerial();
  }
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
   Sets the value of an output pin based on it's corresponding sensor value.
*/
void setOutput(int outPutPin, int sensorValue, int outputValue) {
  if (sensorValue > EMGTreshold) {
    outputValue = true;
  }
  else {
    outputValue = false;
  }
  digitalWrite(outPutPin, outputValue);
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
  Serial.print(outputValue0);
  Serial.print(" | EMG1 ");
  Serial.print(sensorValue1);
  Serial.print("-");
  Serial.print(outputValue1);
  Serial.print(" | EMG2 ");
  Serial.print(sensorValue2);
  Serial.print("-");
  Serial.print(outputValue2);
  Serial.print(" | EMG3 ");
  Serial.print(sensorValue3);
  Serial.print("-");
  Serial.println(outputValue3);
}


/*
   Prints the current values and their names in a format suitable for writing to a .CSV file.
*/
void printAllCSV() {
  //  Serial.print("timer,");
  //  Serial.print(timer);
  //  Serial.print(",currentValue,");
  //  Serial.print(currentValue);
  //  Serial.print("\n");
}

