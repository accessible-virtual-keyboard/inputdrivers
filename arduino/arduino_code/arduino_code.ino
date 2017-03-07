/*  
 * @author Kristian Honningsvag
 */

//Hardware pins
const int EMG_SENSOR_0 = A0;
const int EMG_SENSOR_1 = A1;
const int EMG_SENSOR_2 = A2;
const int EMG_SENSOR_3 = A3;
const int OUTPUT_0 = 10;
const int OUTPUT_1 = 11;
const int OUTPUT_2 = 12;
const int OUTPUT_3 = 13;

//Variables
float currentValue = 0.0;
unsigned long timer = 0;
unsigned int timeBetweenLogging = 200;   // Milliseconds.


void setup() {
  pinMode(EMG_SENSOR_0, INPUT);
  pinMode(EMG_SENSOR_1, INPUT);
  pinMode(EMG_SENSOR_2, INPUT);
  pinMode(EMG_SENSOR_3, INPUT);
  pinMode(OUTPUT_0, OUTPUT);
  pinMode(OUTPUT_1, OUTPUT);
  pinMode(OUTPUT_2, OUTPUT);
  pinMode(OUTPUT_3, OUTPUT);
  Serial.begin(9600);
  timer = millis();
}


void loop() {
  currentValue = getCurrentEMG(EMG_SENSOR_0);
  if (currentValue > 900) {
    Serial.println("Active");
  }
  else {
    // Print the values.
    if ( (millis() - timer) >= timeBetweenLogging) {
      timer = millis();
      printAllCSV();
    }
  }
}


// Gets the current value from the EMG sensor.
float getCurrentEMG(int pin) {
  int sensorValue = analogRead(pin);
  // Convert the analog reading (which goes from 0 - 1023) to a voltage (0 - 5V):
  float voltage = sensorValue * (5.0 / 1023.0);
  return sensorValue;
}


// Prints the current values and their names in a format suitable for .CSV files.
void printAllCSV() {
//  Serial.print("timer,");
//  Serial.print(timer);
  Serial.print(",currentValue,");
  Serial.print(currentValue);
  Serial.print("\n");
}


// Prints the current values in readable format.
// This function is meant to be used with the Arduino IDE serial monitor.
void printAllSerial() {
  Serial.print("timer ");
  Serial.println(timer);
  Serial.print("currentValue ");
  Serial.println(currentValue);
}

