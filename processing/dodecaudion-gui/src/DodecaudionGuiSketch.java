import processing.core.*;
import processing.serial.*;
import oscP5.*;
import netP5.*;
import gnu.io.*;

public class DodecaudionGuiSketch extends PApplet {

	PGraphics plot;

	Serial port;
	boolean serialConnectionEstablished = false;
	
	int lf = 10;
	float[] val = { 0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
	float[] val_prev = { 0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
	int[] nonZeroVal = { 0,0,0,0,0,0,0,0,0,0,0,0};
	int[] avgLength = { 1,1,1,1,1,1,1,1,1,1,1,1};
	int plotHeight = 50;
	PFont font;

	OscP5 osc;
	int oscListenPort = 3333;
	int oscHostPort = 10000;
	String oscHost = "127.0.0.1";
	String oscP5Event = "oscEvent";	
	
	
	// CALIBRATION
	boolean isCalibrating = true;
	int calibrationTime = 150;
	int calibrationStartTime = 0;
	float[] calibrationOffset = { 0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};


	/**
	 * 
	 * @param args
	 */
	public static void main(String args[]){
		PApplet.main(new String[] {"--present" , "DodecaudionGuiSketch"});
	}
	
	/**
	 * 
	 */
	public void setup(){
		  size( 1000 , 800 );
		  plot = createGraphics( width , height , JAVA2D );
		  
		  try{
			  println( Serial.list() );
			  port = new Serial( this , Serial.list()[6] , 115200 );
			  port.bufferUntil( lf );
		  }catch( RuntimeException e ){
			  PApplet.println( e );
			  System.exit(1);
		  }
			  
		  osc = new OscP5( this , oscHost , oscHostPort , oscListenPort , oscP5Event );

		  font = loadFont( "DejaVuSansMono-16.vlw" );
		  textFont( font );

		  frameRate( 30 );
	}
	
	public void draw(){
	  background( 0 );

	  if( !serialConnectionEstablished ){
		  return; 
	  }
		  
	  if( isCalibrating ){
	    calibrate();
	    return;  
	  }	  
	  
	  //podziałka
	  int w = 2 * 20;//round( width / s );
	  for( int i = 0 ; i < width ; i = i+w ){
	    stroke( 128 );
	    line( i , 0 , i , height );
	  }


	  //przebieg
	  int x = frameCount % width;
	  int step = (plotHeight + 15);

	  plot.beginDraw();  
	  for( int i = 0 ; i < val.length ; i++ ){
	    int y0 = (i+1)* step;
	    float v = plotHeight * val[i];
	    int y = y0 - (int)v;    

	    plot.stroke( 0 );
	    plot.line( x , y0 , x , i*step );
	    plot.stroke( color( 255 * i/(float)val.length , 255 * i/(float)val.length , 255 - 255 * (i+1)/(float)val.length )  );
	    plot.line( x , y0 , x , y );

	    plot.stroke(255,128);
	    plot.line( x + 1 , y0 , x + 1 , y0 - plotHeight );
	  }
	  plot.endDraw();  

	  image( plot , 0 , 0 );

	  //wyświetlanie wartości odczytów
	  for( int i = 0 ; i < val.length ; i++ ){
	    int y = 20 + i * step;
	    text( (i+1) +": " + nf(val[i],1,3) , 10 , y );
	  }

	}

	public void stop(){
		port.stop();
		//Wysyłanie komunikatów OSC
		OscMessage msg = osc.newMsg( "/Instrument01" );
		float[] data = { 0,val[0],val[1] };
		msg.add( data );
		osc.send( msg );  

		msg = osc.newMsg( "/Instrument02" );
		float[] data2 = { 0,val[0],val[1] };
		msg.add( data2 );
		osc.send( msg );  

		super.stop();
	}


	public void serialEvent( Serial p ){
	  try{
	    String buffer = port.readStringUntil( lf );
	    if( buffer != null ){
	    	serialConnectionEstablished = true;
	    	float[] _val = PApplet.parseFloat( PApplet.split(buffer,",") );
	    	for( int i = 0 ; i < _val.length ; i++ ){
	    		if( !Float.isNaN( _val[i] ) ){
	    			val[ i ] = calcValAVG( _val[ i ] , i );
	    			if( val[i] < 0.01 ){ val[i] = 0; }
	    				val[ i ] = PApplet.norm(val[i],0.0f,1.0f);
	    			}
	    		}
	  
		      //Wysyłanie komunikatów OSC
		      OscMessage msg = osc.newMsg( "/Instrument01" );
		      float[] data = { val[1],val[3],val[5],val[7],val[9],val[11] };
		      msg.add( data );
		      osc.send( msg );
		  
		      //Wysyłanie komunikatów OSC
		      msg = osc.newMsg( "/Instrument02" );
		      float[] data2 = { val[0],val[2],val[4],val[6],val[8],val[10] };
		      msg.add( data2 );
		      osc.send( msg );
		  
		    }
	  }catch(Exception e){
	    println(e);
	  }

	}


	public void keyPressed(){
		if( key == 's' ){
			saveFrame( "screen-#####.png" );
		}
		if( key == 'c' ){
			calibrationStartTime = frameCount;
			isCalibrating = true;
		} 	  
	}

	/**
	 * 
	 */
	public void calibrate(){
		if( calibrationTime >= ( frameCount - calibrationStartTime ) ){
			for( int i = 0 ; i < val.length ; i++ ){
				calibrationOffset[ i ] = ( calibrationOffset[ i ] + val[ i ] ) / 2.0f; 
			}
		}else{
		   isCalibrating = false; 
		}
	}

	/**
	 * Wyliczenie wartości przebiegu val[idx] po zadaniu wymuszenia `v`
	 * Bazuje na średniej wartości sygnału
	 */
	float calcValAVG( float v , int idx ){
	  //float val_next;
	  //float signalTimeMinLength = 3.0f;

	  //jeśli za krótka średnia to nie ma czego liczyć
//	  if( avgLength[ idx ] < 2 ){ 
//	      //return v; 
//	  }

	  //poprzednia wartość = aktualnie znana wartość.
//	  val_prev[idx] = val[ idx ];   
//	  
//	  //wyliczenie następnej wartości dla wymuszenia `v`
//	  val_next = val_prev[idx] * ( avgLength[idx] - 1 ) / (float)avgLength[idx]  +  v / (float)avgLength[idx];
//
//	  //tmp:
//	  val_next = sin(PI*pow(val_next,2));
	  //val_next = sqrt(pow( -1+10*val_next , 2 ));
	  //val_next = pow(sin( PI * val_next ),3);

	  //tutaj uzależnienie od długości trwania sygnału
	  //do usuwania peaków kiedy nie ma żadnej wartości.
	  /*
	  if( val_next > 0.01 ){
	   nonZeroVal[ idx ]++;
	   //println( idx + ": " + nonZeroVal[ idx ] );
	   }else{
	   nonZeroVal[ idx ] = 0;
	   }
	   
	   if( val_next > 0.5 * 0.2 ){
	   nonZeroVal[ idx ] = int(0.5*signalTimeMinLength);
	   }
	   
	   if( val_next > 0.2 ){
	   nonZeroVal[ idx ] = int(signalTimeMinLength);
	   }
	   
	   if( nonZeroVal[ idx ] < signalTimeMinLength ){
	   val_next = val_next * nonZeroVal[idx]/signalTimeMinLength;
	   }
	   */
	  return max( 0, v - calibrationOffset[ idx ] );
	}	
	
}
