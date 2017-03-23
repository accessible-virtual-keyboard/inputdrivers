
#include "RunningAverage.h"

const int EMG_SENSOR_PIN_INPUT1 = A0;
const int EMG_SENSOR_PIN_INPUT2 = A1;
const int EMG_SENSOR_PIN_INPUT3 = A2;
const int EMG_SENSOR_PIN_INPUT4 = A3;

const int NUMBER_OF_SENSORS = 2;
const int RUNNING_AVG_SAMPLE_COUNT = 15;

const int EMG_SIGNAL_TRIGGER_THRESHOLD = 12;  // The upper threshold the signal should reach to generate an keyboard input signal

// The lower threshold the signal must reach, after the last generated keyboard input, before again being able to generate a new input signal.
const int EMG_SIGNAL_RELEASE_THRESHOLD = 8;   

const int sensors[] = {EMG_SENSOR_PIN_INPUT1, EMG_SENSOR_PIN_INPUT2, EMG_SENSOR_PIN_INPUT3, EMG_SENSOR_PIN_INPUT4};

RunningAverage sensorValues[] = {
  RunningAverage(RUNNING_AVG_SAMPLE_COUNT),
  RunningAverage(RUNNING_AVG_SAMPLE_COUNT),
  RunningAverage(RUNNING_AVG_SAMPLE_COUNT),
  RunningAverage(RUNNING_AVG_SAMPLE_COUNT)
};

boolean inputTriggerState[] = {false, false, false, false};

void setup() {
  pinMode(EMG_SENSOR_PIN_INPUT1, INPUT);
  pinMode(EMG_SENSOR_PIN_INPUT2, INPUT);
  pinMode(EMG_SENSOR_PIN_INPUT3, INPUT);
  pinMode(EMG_SENSOR_PIN_INPUT4, INPUT);
  Serial.begin(9600);
  for(int i=0; i < NUMBER_OF_SENSORS; i++){
    resetSensorValueToZero(i);
  }
}

void loop() {

  for(int i=0; i < NUMBER_OF_SENSORS; i++){
    sensorValues[i].addValue(getEMGSignal(i);
  
    float sensorValue = sensorValues[i].getAverage();
    
    if(sensorValue > EMG_SIGNAL_TRIGGER_THRESHOLD){
      if(!inputTriggerState[i]){
        Serial.println(String(i+1));
        //Serial.println("Trigger " + String(i) + " "+ String(sensorValue));
        inputTriggerState[i] = true;
      }
    }else if(sensorValue < EMG_SIGNAL_RELEASE_THRESHOLD){
      if(inputTriggerState[i]){
        //Serial.println("Reset");
        resetSensorValueToZero(i);
        inputTriggerState[i] = false;
      }
    }
  }
  delay(5);
}


int getEMGSignal(int sensorIndex) {
  return analogRead(sensors[sensorIndex]);
}

void resetSensorValueToZero(int i){
  sensorValues[i].fillValue(0, RUNNING_AVG_SAMPLE_COUNT);
}


