const int BTN1 = 7;   
const int BTN2 = 6;     
const int LED =  13;

int btnState1 = 0; 
int prevBtnState1 = 0;
int btnState2 = 0;
int prevBtnState2 = 0;

void setup() {
  Serial.begin(9600);
  pinMode(LED, OUTPUT);
  
  pinMode(BTN1, INPUT);
  pinMode(BTN2, INPUT);
}

void loop() {
  btnState1 = digitalRead(BTN1);
  btnState2 = digitalRead(BTN2);
  
  if (btnState1 != prevBtnState1) { //Prevent multiple inputs from being sent.
    prevBtnState1 = btnState1;
    if(btnState1 == HIGH){ //HIGH means button is pressed, LOW means button is released.
      Serial.println("1"); //Print 1 to serial to send input 1 to application with emg_websocket_interface.py.
      digitalWrite(LED, HIGH); //Light up buildt-in arduino led to show when a input triggers.
    }
  }else if(btnState2 != prevBtnState2){
    prevBtnState2 = btnState2;
    if(btnState2 == HIGH){ 
      Serial.println("2"); 
      digitalWrite(LED, HIGH);
    }
  } else{
    //Serial.println("BTN1: " + String(btnState1)+ ", BTN2: " + String(btnState2));
    digitalWrite(LED, LOW);
  }

  delay(50); // A small delay to prevent multiple button presses to trigger
}




