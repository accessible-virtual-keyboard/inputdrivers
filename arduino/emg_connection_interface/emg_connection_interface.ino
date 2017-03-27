
#include "RunningAverage.h"

const int EMG_SENSOR_PIN_INPUT1 = A0;
const int EMG_SENSOR_PIN_INPUT2 = A1;
const int EMG_SENSOR_PIN_INPUT3 = A2;
const int EMG_SENSOR_PIN_INPUT4 = A3;

const int NUMBER_OF_SENSORS = 2;
const int RUNNING_AVG_SAMPLE_COUNT = 10;
const int THRESHOLD_AVG_SAMPLE_COUNT = 20;

  
const int sensors[] = {EMG_SENSOR_PIN_INPUT1, EMG_SENSOR_PIN_INPUT2, EMG_SENSOR_PIN_INPUT3, EMG_SENSOR_PIN_INPUT4};

const int THRESHOLD_SAMPLE_COUNTER_TARGET = 11;

int thresholdSampleCounter[] = {0, 0, 0, 0}; // Used to only sample the threshold values every THRESHOLD_SAMPLE_COUNTER_TARGET times through the main loop

RunningAverage sensorValues[] = {
  RunningAverage(RUNNING_AVG_SAMPLE_COUNT),
  RunningAverage(RUNNING_AVG_SAMPLE_COUNT),
  RunningAverage(RUNNING_AVG_SAMPLE_COUNT),
  RunningAverage(RUNNING_AVG_SAMPLE_COUNT)
};

RunningAverage thresholdValuesHigh[] = {
  RunningAverage(THRESHOLD_AVG_SAMPLE_COUNT),
  RunningAverage(THRESHOLD_AVG_SAMPLE_COUNT),
  RunningAverage(THRESHOLD_AVG_SAMPLE_COUNT),
  RunningAverage(THRESHOLD_AVG_SAMPLE_COUNT)
};

RunningAverage thresholdValuesLow[] = {
  RunningAverage(THRESHOLD_AVG_SAMPLE_COUNT),
  RunningAverage(THRESHOLD_AVG_SAMPLE_COUNT),
  RunningAverage(THRESHOLD_AVG_SAMPLE_COUNT),
  RunningAverage(THRESHOLD_AVG_SAMPLE_COUNT)
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
    resetThresholdValueToZero(i);
  }
}


void loop() {

  for(int i=0; i < NUMBER_OF_SENSORS; i++){
    sensorValues[i].addValue(getEMGSignal(i));

    float sensorValue = sensorValues[i].getAverage();

    // For debugging uncomment and use the serial plotter to see the signal values. (Remember to comment out the signal trigger println a few lines below) 
    //Serial.println(String(sensorValue) + "  " + thresholdValuesHigh[i].getAverage() + " " + thresholdValuesLow[i].getAverage());
    
    if(sensorValue > thresholdValuesHigh[i].getAverage()){
      if(!inputTriggerState[i]){
        Serial.println(String(i+1));
        inputTriggerState[i] = true;
      }

    }else if(sensorValue < thresholdValuesLow[i].getAverage()){
      if(inputTriggerState[i]){
        //Serial.println("Reset " + (i+1));
        resetSensorValueToZero(i); // Reset sensor values to zero to reduce the bounce in the signal that often occurs after the signal triggers
        inputTriggerState[i] = false;
      }
    }

    // Update threshold values
    if(++thresholdSampleCounter[i] == THRESHOLD_SAMPLE_COUNTER_TARGET){
      thresholdValuesHigh[i].addValue(sensorValue*1.7);
      thresholdValuesLow[i].addValue(sensorValue);
      thresholdSampleCounter[i] = 0;
    }

  }
  delay(20);
}



int getEMGSignal(int sensorIndex) {
  return analogRead(sensors[sensorIndex]);
}

void resetSensorValueToZero(int i){
  sensorValues[i].fillValue(0, RUNNING_AVG_SAMPLE_COUNT);
}
void resetThresholdValueToZero(int i){
  thresholdValuesHigh[i].fillValue(0, THRESHOLD_AVG_SAMPLE_COUNT);
  thresholdValuesLow[i].fillValue(0, THRESHOLD_AVG_SAMPLE_COUNT);

}

