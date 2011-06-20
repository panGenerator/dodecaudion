/**
 * Odczytywanie wartości z sensorów IR
 * i wysyłanie odczytów przez port szeregowy/ 
 * Wersja do testów
 */

//int inputMap[] = {1,7,2,8,3,9,3,6,4,10,5,11}
//wartości aktualnych odczytów
float val[] = {
  0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
//wartości aktualnych odczytów
float val_next[] = {
  0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
float tmp_val_next[] = {
  0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
//wartośći poprzednich odczytów
float val_prev[] = {
  0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
//bo nie wiem jak się sprawdza długość tablicy w arduino :D
int valCount = 12;
//wartośc ustawiana do odczytu z multipleksera
int multiplexerInput[] = {
  0, 1, 10, 11, 100, 101, 110, 111};
//czy wysyłać wartości odczytów
boolean sendNewValues = false;

void setup(){

  pinMode( 5, OUTPUT );
  pinMode( 6, OUTPUT );
  pinMode( 7, OUTPUT );

  analogReference( EXTERNAL );

  Serial.begin(115200);

}


void loop(){  
  analogReadMultiplexer();
  for( int i = 0 ; i < valCount ; i++ ){
    val[i] = calcNewValFromDiff( val_next[i] , val[i] );  
  }

  sendNewValues = true;
  //dane są wysyłane tylko wtedy kiedy się zmienią
  if( sendNewValues ){
    for( int i = 0 ; i < valCount ; i++ ){
      Serial.print( val[i] );
      if( i < (valCount-1) ){
        Serial.print( "," );
      }
      val_prev[i] = val[i];
    }
    Serial.println( "" );
    sendNewValues = false;    
  }
  delay(20);
}

void analogReadMultiplexer(){
  float ir,dist,cdist,mdist;
  int row,r0,r1,r2; 
  int input = 0;

  int analogPins[] = {4,5};

  input = -1;
  int i = 0;
  for( int mi = 0 ; mi < 6 ; mi++ ){
    //pierwszy ADC
    input++;
    i = 0;
    //for( int i = 0 ; i < 2 ; i++ ){        
    row = multiplexerInput[ mi ];      
    r0 = row & 0x01; 
    r1 = (row>>1) & 0x01; 
    r2 = (row>>2) & 0x01; 

    digitalWrite(5, r0); //s0        
    digitalWrite(6, r1); //s1
    digitalWrite(7, r2); //s2

    ir = analogRead( analogPins[i] );
    val_next[input]  = ir / 1024.0;

    //drugi ADC
    input++;
    i = 1;        
    row = multiplexerInput[ mi ];      
    r0 = row & 0x01; 
    r1 = (row>>1) & 0x01; 
    r2 = (row>>2) & 0x01; 

    digitalWrite(5, r0); //s0        
    digitalWrite(6, r1); //s1
    digitalWrite(7, r2); //s2

    ir = analogRead( analogPins[i] );
    val_next[input]  = ir / 1024.0;


    /*
       * Różne warianty skalowania odczytów
     */

    //a) liniowy odczyt

    //odcięcie odczytów poniżej 0.2V. Zakres pomiaru to 3.3V
    //val_next[input] = abs(val_next[input] - (0.2/3.3)*val_next[input]);

    //b) trochę mniej liniowy odczyt
    //val_next[input]  = pow( val_next[input] , 2 );    

    //c) przeskalowanie poprawiające czułość w środku zakresu i odcinające jego brzegi
    //val_next[input]  = pow( sin( 0.5 * 3.1415 * val_next[input] ) , 4 );    

    //d) to jeszcze bardziej eksponuje środek zakresu
    //val_next[input] = pow( sin( 0.5 * 3.14 * val_next[input] ) , 2 );    

    //e) linearyzacja odczytów, słabo to działa
    //dist = ( 2914 / (ir + 5) ) - 1;
    //cdist = constrain( dist, 4.0, 30 );
    //val_next[input]  = (cdist - 4.0) / 26;


    //x) mother of it all - mniej liniowy z odcinaniem brzegów
    //val_next[input]  = ir / 1024.0;      
    //tmp: val_next[input]  = pow( 2.0 * ir / 1024.0 , 2 ) / 4.0 ;    
    //tmp: val_next[input]  = log( 1.0 + pow( sin( 0.5 * 3.14 * val_next[input] ) , 2 ) );
    //tmp: val_next[input]  = log( 1.0 + sin( 0.5 * 3.14 * val_next[input] ) );
    //dzielenie przez 0.7 bo dla danego zakresu log zwraca wartość z przedziału (0.0,0.7);
    //val_next[input]  = log( 1.0 + pow( tan( 0.25 * 3.14 * val_next[input] ) , 2 ) ) / 0.7;

    //}
    /*
    ir = analogRead(0);  
     //dist = ( 2914 / (ir + 5) ) - 1;
     //cdist = constrain( dist, 4.0, 30 );
     //val_next[input]  = (cdist - 4.0) / 26;
     val_next[input]  = ir / 1024.0;
     input++;
     
     //odczyty z drugiego multipleksera są zapisywane od 6 w górę
     ir = analogRead(5);  
     //dist = ( 2914 / (ir + 5) ) - 1;
     //cdist = constrain( dist, 4.0, 30 );
     //val_next[input]  = (cdist - 4.0) / 26;
     
     //to bardzo poprawia dynamikę w środku zakresu działania i odcina szumy na krańcch
     //val_next[input]  = pow( sin( 0.5 * 3.14 * ir / 1024.0 ) , 4 );    
     val_next[input]  = pow( sin( 0.5 * 3.14 * ir / 1024.0 ) , 2 );    
     input++;    
     */
  }


}

/**
 * Wyliczanie nowej wartości na podstawie różnicy między wartością zadaną 
 * a aktualną.
 */
float calcNewValFromDiff( float valNew , float valCurrent ){  
  return valNew;
  float dv = valNew - valCurrent;
  //jesli zmiana mniejsza niż 10% aktualnej wartości to jest pomijana
  //if( abs( dv ) < 0.1 * valCurrent ){ return valCurrent; }
  //zwracana wartośc to wartość aktualna oraz 33% różnicy międyz zadaną a aktualną
  return valCurrent + 0.95 * dv;
  //return valCurrent + (1-0.6*valNew) * dv;
}

