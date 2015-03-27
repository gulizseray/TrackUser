package trackuser.hw2.cs498.illinois.edu.trackuser;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by gulizseray on 3/24/15.
 */

/* HW2 - Track User
* */


public class MainActivity extends ActionBarActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor senAccelerometer;
    private Sensor senLinearAccelerometer;
    private Sensor senGyroscope;
    private Sensor senMagnetometer;
    private Sensor senLight;
    private Sensor senRotation;
    private long startTime = System.currentTimeMillis();



    public static final float ALPHA = (float) 0.7;
    public static final float FILTER_COEFFICIENT = 0.98f;
    private static final float rToD = (float) (180 / Math.PI);

    private File readingsFile;
    private FileOutputStream readingsOutputStream;
    private String sensorFileName = "sensorReadings.csv";

    // Cached values for the sensor readings
    float [] cachedAccelerometer = new float[3];
    float cachedAcceleration = 0;
    float [] cachedGyroscope = new float[3];
    float [] cachedMagnetometer= new float[3];

    float[] magDifference = new float[3];

    float cachedLightSensor = 0;

    private int numSteps = 0;
    private float totalTurn = 0;

    //All angles in radians
    private float angleGyro = 0;
    float angleMag = 0;
    Float angleMagInitial = null;
    public float fusedAngle = 0;

    private long lastStepCountTime = startTime;
    private long lastGyroTime = startTime;
    private long lastMagnetTime = startTime;
    private long lastAccMagTime = startTime;

    private TextView stepsTextView = null;
    private TextView distanceTextView = null;
    private TextView degreesTextView = null;

    private TextView gyroMeasurementTextView = null;
    private TextView magMeasurementTextView = null;
    private TextView errorDetectionTextView = null;
    private TextView accMagTextView = null;


    public void initializeFile(){
        try {
            readingsFile = new File(Environment.getExternalStorageDirectory(), sensorFileName);
            readingsOutputStream = new FileOutputStream(readingsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initializeSensors(){
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //initialize accelerometer
        senAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_FASTEST);

        //initialize gyroscope
        senGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(this, senGyroscope, SensorManager.SENSOR_DELAY_FASTEST);

        //initialize magnetometer
        senMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, senMagnetometer, SensorManager.SENSOR_DELAY_FASTEST);

        //initialize light sensor
        senLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener(this, senLight, SensorManager.SENSOR_DELAY_FASTEST);


        //initialize linear accelerometer
        senLinearAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_FASTEST);

        //initialize rotation sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

    }
    public boolean isAllZeros(float [] a){
        for(int i=0;i<a.length;i++)
            if(a[i] != 0)
                return false;
        return true;

    }

    public float[] lowPassFilter( float[] input, float[] output ) {
        if ( output == null ) return input;
        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

//    @Override
//    public void onSensorChanged(SensorEvent sensorEvent) {
//        Sensor mySensor = sensorEvent.sensor;
//        long currTime = System.currentTimeMillis();
//
//        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//            cachedAccelerometer = lowPassFilter(sensorEvent.values.clone(), cachedAccelerometer);
//
//            if(cachedAccelerometer[2] > 11.4)
//            {
//                // There needs to be at least 300ms between two peaks, otherwise it isn't a step.
//                if (currTime - lastStepCountTime > 300)
//                {
//                    numSteps++;
//                    lastStepCountTime = currTime;
//                    stepsTextView.setText(String.valueOf(numSteps));
//                    distanceTextView.setText(String.format("%.1f", numSteps*0.9) + "m");
//                }
//            }
//        }
//        else if (mySensor.getType() == Sensor.TYPE_GYROSCOPE){
//            long currentTime = System.currentTimeMillis();
//            //cachedGyroscope = sensorEvent.values;
//            System.arraycopy(sensorEvent.values, 0, cachedGyroscope, 0, 3);
//            long deltaT = (currentTime - lastGyroTime);
//
//            //Update position, when gyroscope exceed threshold and a minimum time has passed
//            if(Math.abs(cachedGyroscope[2]) > 0.05 && deltaT > 50) {
//
//                //This checks, if Gyroscope and the differentiation of the angle to north,
//                // obtained from the magnetometer are similar within a certain tolerance range
//
////                if(Math.abs(magDifference[2] - cachedGyroscope[2]) < 0.3) {
//
//                //perform numeric integration
//                float addedTurn = cachedGyroscope[2] * deltaT / 1000 * rToD;
//
//                //add absolute value to total turn
//                totalTurn += Math.abs(addedTurn);
//
//                //add original value to angle from initial and make sure, it doesn't exceed 360º (361º=1º)
//                angleGyro += addedTurn;
////                angleGyro =  ALPHA * addedTurn + (1-ALPHA) * angleM;
//
//
//                //angleGyro = angleGyro % 360;
//                degreesTextView.setText(String.format("%.2f", angleGyro) + ", " + String.format("%.2f", totalTurn));
//                gyroMeasurementTextView.setText(String.format("%.3f updating %n %d, %.8f", cachedGyroscope[2], deltaT, addedTurn));
//
//
//                //show the user, that gyro and magnetometer were consistent, by coloring the info-area green
//                errorDetectionTextView.setBackgroundColor(0xff19ff14);
////                } else {
////                    //show the user, that gyro and magnetometer were NOT consistent, by coloring the info-area red
////                    errorDetectionTextView.setBackgroundColor(0xFFFF4E25);
////                }
//                //print the difference between gyro and compass in the info-area
//                errorDetectionTextView.setText(String.valueOf(magDifference[2] - cachedGyroscope[2]));
//                //change lastGyroTime to current time
//                lastGyroTime = currentTime;
//            } else if (deltaT>50) {
//                //In this case, the time to update the angles has arrived, but there was no significant measurement on the gyro
//                lastGyroTime = currentTime;
//            }
//
//        }
//        else if (mySensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
//            float deltaT= System.currentTimeMillis() - lastMagnetTime;
//
//            //update the magnetometer readings every 50ms
//            if(deltaT > 50) {
//                cachedMagnetometer = lowPassFilter(sensorEvent.values.clone(), cachedMagnetometer);
//
//                //calculate the angle between phone and north (assuming phone's orientation is parallel to the ground)
//                float angleNew = (float) Math.atan2((double) cachedMagnetometer[0], cachedMagnetometer[1]) * rToD;
//
//                //differentiate the angle numerically.
//                magDifference[2] = (angleNew - angleM)/(deltaT*1000);
//
////                if(angleMInitial == null)
////                    angleMInitial = new Float(angleNew);
//
//                //The measurement of this iteration will be the old one in the next iteration
//                angleM = angleNew;
//                //angleM = (1 - ALPHA) * angleM + ALPHA * angleNew;
//
//                //update display of the angle
//                magMeasurementTextView.setText(String.format("%.3f", angleM ));
//
//                //Set the last update time to the current time. (Might be a good idea to replace it with += deltaT)
//                lastMagnetTime = System.currentTimeMillis();
//            }
//        }
//        else if (mySensor.getType() == Sensor.TYPE_LIGHT){
//
//            cachedLightSensor = sensorEvent.values[0];
//        }
//
////        if (cachedAccelerometer != null && cachedMagnetometer != null) {
//        if (!isAllZeros(cachedAccelerometer) && !isAllZeros(cachedMagnetometer)) {
//
//            long currentTime = System.currentTimeMillis();
//            long deltaT = (currentTime - lastAccMagTime);
//
//            if(deltaT > 50) {
//
//                float R[] = new float[9];
//                float I[] = new float[9];
//
//                if (SensorManager.getRotationMatrix(R, I, cachedAccelerometer, cachedMagnetometer)) {
//
//                    // orientation contains azimut, pitch and roll
//                    float orientation[] = new float[3];
//                    SensorManager.getOrientation(R, orientation);
//
//                    float azimut = orientation[0] * (-1) ; //needs to be multiplied by -1 to get correct result?
//
//                    if(angleMInitial == null)
//                        angleMInitial = new Float(Math.toDegrees(azimut));
//
//                    accMagTextView.setText(String.format("%.2f", Math.toDegrees(azimut) - angleMInitial));
//                }
//            }
//        }
//
//        //writeAllReadingsToFile(currTime - startTime);
//
//    }

    public void calculateAccMagOrientation(){

        float R[] = new float[9];
        float I[] = new float[9];

        if (SensorManager.getRotationMatrix(R, I, cachedAccelerometer, cachedMagnetometer)) {

            // orientation contains azimut, pitch and roll
            float orientation[] = new float[3];
            SensorManager.getOrientation(R, orientation);

            float azimut = orientation[0] * (-1) ; //needs to be multiplied by -1 to get correct result?

            if(angleMagInitial == null)
                angleMagInitial = new Float(Math.toDegrees(azimut));

            angleMag = (FILTER_COEFFICIENT) * angleMag + (1 - FILTER_COEFFICIENT) * azimut;

            accMagTextView.setText(String.format("%.2f", Math.toDegrees(angleMag) - angleMagInitial));
        }
    }

    public void countSteps(){
        long currTime = System.currentTimeMillis();

        if(cachedAccelerometer[2] > 11.4)
        {
            // There needs to be at least 300ms between two peaks, otherwise it isn't a step.
            if (currTime - lastStepCountTime > 300)
            {
                numSteps++;
                lastStepCountTime = currTime;
                stepsTextView.setText(String.valueOf(numSteps));
                distanceTextView.setText(String.format("%.1f", numSteps*0.9) + "m");
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            cachedAccelerometer = lowPassFilter(sensorEvent.values.clone(), cachedAccelerometer);
            countSteps();
            calculateAccMagOrientation();
        }
        else if (mySensor.getType() == Sensor.TYPE_GYROSCOPE){
            long currentTime = System.currentTimeMillis();
            System.arraycopy(sensorEvent.values, 0, cachedGyroscope, 0, 3);
            long deltaT = (currentTime - lastGyroTime);

            //This checks, if Gyroscope and the differentiation of the angle to north,
            // obtained from the magnetometer are similar within a certain tolerance range

            //perform numeric integration
//            float addedTurn = cachedGyroscope[2] * deltaT / 1000 * rToD;
            float addedTurn = cachedGyroscope[2] * deltaT / 1000;
//
            //add absolute value to total turn
            totalTurn += Math.abs(addedTurn);

            //add original value to angle from initial and make sure, it doesn't exceed 360º (361º=1º)
            angleGyro += addedTurn;
            angleGyro %= 360;

            fusedAngle =  FILTER_COEFFICIENT * angleGyro + (1-FILTER_COEFFICIENT) * (angleMag - ( (angleMagInitial == null) ? 0 : angleMagInitial));
//
            //angleGyro = angleGyro % 360;
            degreesTextView.setText(String.format("%.2f", Math.toDegrees(fusedAngle)) + ", " + String.format("%.2f", totalTurn));
            gyroMeasurementTextView.setText(String.format("%.3f updating %n %d, %.8f", cachedGyroscope[2], deltaT, addedTurn));

            //change lastGyroTime to current time
            lastGyroTime = currentTime;


        }
        else if (mySensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            System.arraycopy(sensorEvent.values, 0, cachedMagnetometer, 0, 3);
        }
        else if (mySensor.getType() == Sensor.TYPE_LIGHT){

            cachedLightSensor = sensorEvent.values[0];
        }
    }

    public void writeAllReadingsToFile(long timestamp){
        String acc = cachedAccelerometer[0] + "," + cachedAccelerometer[1] + "," + cachedAccelerometer[2] + ",";
        String gyr = cachedGyroscope[0] + "," + cachedGyroscope[1] + "," + cachedGyroscope[2] + ",";
        String mag = cachedMagnetometer[0] + "," + cachedMagnetometer[1] + "," + cachedMagnetometer[2] + ",";

        //String all = timestamp + "," + acc + gyr + mag + String.valueOf(cachedLightSensor) + " , " + cachedAcceleration + "\n";
        String all = timestamp + "," + acc + gyr + mag + String.valueOf(cachedLightSensor) + "," + cachedAcceleration + "\n";
        try {
            readingsOutputStream.write( all.getBytes() );
            readingsOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, senGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, senLinearAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, senRotation, SensorManager.SENSOR_DELAY_FASTEST);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stepsTextView = (TextView) findViewById(R.id.stepCounter);
        distanceTextView = (TextView) findViewById(R.id.distanceValue);
        degreesTextView = (TextView) findViewById(R.id.degreeCounter);
        gyroMeasurementTextView = (TextView) findViewById(R.id.gyroValues);
        magMeasurementTextView = (TextView) findViewById(R.id.degreeCounterM);
        errorDetectionTextView = (TextView) findViewById(R.id.error_indicator);
        accMagTextView = (TextView) findViewById(R.id.acc_magneto_count);

        initializeSensors();
        initializeFile();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}