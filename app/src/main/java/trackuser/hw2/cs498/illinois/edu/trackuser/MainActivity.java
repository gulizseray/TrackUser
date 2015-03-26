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
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import static android.util.FloatMath.sqrt;

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
    long lastUpdate = 0;


    public static final float ALPHA = (float) 0.7;

    private File readingsFile;
    private FileOutputStream readingsOutputStream;
    private String sensorFileName = "sensorReadings.csv";

    // Cached values for the sensor readings
    float [] cachedAccelerometer;
    float cachedAcceleration = 0;
    float [] cachedGyroscope;
    float [] cachedMagnetometer;
    float cachedLightSensor = 0;

    private int numSteps = 0;
    private long lastStepCountTime = 0;

    private TextView stepsTextView = null;
    private TextView degreesTextView = null;


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

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;
        long currTime = System.currentTimeMillis();

//        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//            float x = sensorEvent.values[0];
//            float y = sensorEvent.values[1];
//            float z = sensorEvent.values[2];
//
//            float gravity [] = new float[3];
//            final float alpha = (float) 0.1;// Doesn't work with .8 for some reason
//
//            // Isolate the force of gravity with the low-pass filter.
//            gravity[0] = alpha * gravity[0] + (1 - alpha) * x;
//            gravity[1] = alpha * gravity[1] + (1 - alpha) * y;
//            gravity[2] = alpha * gravity[2] + (1 - alpha) * z;
//
//            // Remove the gravity contribution with the high-pass filter.
//            cachedAccelerometer[0] = x - gravity[0];
//            cachedAccelerometer[1] = y - gravity[1];
//            cachedAccelerometer[2] = z - gravity[2];
//
//            cachedAcceleration = sqrt(cachedAccelerometer[0]*cachedAccelerometer[0] + cachedAccelerometer[1]*cachedAccelerometer[1]
//                    + cachedAccelerometer[2]*cachedAccelerometer[2]);
//
//            // Count steps
//            // Peak is substantial enough to be correlated to a step
//            if(cachedAcceleration > 2.5)
//            {
//                // There needs to be at least 300ms between two peaks, otherwise it isn't a step.
//                if (currTime - lastStepCountTime > 300)
//                {
//                    numSteps++;
//                    lastStepCountTime = currTime;
//                    stepsTextView.setText(String.valueOf(numSteps));
//                }
//            }
//
////            Toast.makeText(getApplicationContext(), cachedAcceleration + " " + cachedAccelerometer[0] + " " + cachedAccelerometer[1] + " " + cachedAccelerometer[2], Toast.LENGTH_SHORT).show();
//        }
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//            float x = sensorEvent.values[0];
//            float y = sensorEvent.values[1];
//            float z = sensorEvent.values[2];
//
//            // Low-pass filter to remove noise
//            cachedAccelerometer[0] = (1-ALPHA) * cachedAccelerometer[0] + ALPHA * x;
//            cachedAccelerometer[1] = (1-ALPHA) * cachedAccelerometer[1] + ALPHA * y;
//            cachedAccelerometer[2] = (1-ALPHA) * cachedAccelerometer[2] + ALPHA * z;
            cachedAccelerometer = lowPassFilter(sensorEvent.values.clone(), cachedAccelerometer);

            // Copy new values into the cachedAccelerometer array, for consistency in the written file
            //System.arraycopy(sensorEvent.values, 0, cachedAccelerometer, 0, sensorEvent.values.length);

            if(cachedAccelerometer[2] > 11.4)
            {
                // There needs to be at least 300ms between two peaks, otherwise it isn't a step.
                if (currTime - lastStepCountTime > 300)
                {
                    numSteps++;
                    lastStepCountTime = currTime;
                    stepsTextView.setText(String.valueOf(numSteps));
                }
            }
        }
//        else if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
//            float x = sensorEvent.values[0];
//            float y = sensorEvent.values[1];
//            float z = sensorEvent.values[2];
//
//            // Low-pass filter to remove noise
//            cachedAccelerometer[0] = (1-ALPHA) * cachedAccelerometer[0] + ALPHA * x;
//            cachedAccelerometer[1] = (1-ALPHA) * cachedAccelerometer[1] + ALPHA * y;
//            cachedAccelerometer[2] = (1-ALPHA) * cachedAccelerometer[2] + ALPHA * z;
//
//            // Copy new values into the cachedAccelerometer array, for consistency in the written file
//            //System.arraycopy(sensorEvent.values, 0, cachedAccelerometer, 0, sensorEvent.values.length);
//
//            if(cachedAccelerometer[2] > 1.69)
//            {
//                // There needs to be at least 300ms between two peaks, otherwise it isn't a step.
//                if (currTime - lastStepCountTime > 300)
//                {
//                    numSteps++;
//                    lastStepCountTime = currTime;
//                    stepsTextView.setText(String.valueOf(numSteps));
//                }
//            }
//            cachedAcceleration = sqrt(cachedAccelerometer[0]*cachedAccelerometer[0] + cachedAccelerometer[1]*cachedAccelerometer[1]
//                    + cachedAccelerometer[2]*cachedAccelerometer[2]);

            //Toast.makeText(getApplicationContext(), cachedAcceleration + " " + cachedAccelerometer[0] + " " + cachedAccelerometer[1] + " " + cachedAccelerometer[2], Toast.LENGTH_SHORT).show();
            //double angle = Math.atan2(x, y)/(Math.PI/180);
            //Toast.makeText(getApplicationContext(), "Angle: " + angle, Toast.LENGTH_SHORT).show();
//        }
        else if (mySensor.getType() == Sensor.TYPE_GYROSCOPE){
            cachedGyroscope = sensorEvent.values;
        }
        else if (mySensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
//            float x = sensorEvent.values[0];
//            float y = sensorEvent.values[1];
//            float z = sensorEvent.values[2];
//            System.arraycopy(sensorEvent.values, 0, cachedMagnetometer, 0, sensorEvent.values.length);
            cachedMagnetometer = lowPassFilter(sensorEvent.values.clone(), cachedMagnetometer);

        }
        else if (mySensor.getType() == Sensor.TYPE_LIGHT){

            cachedLightSensor = sensorEvent.values[0];
        }
//        else if (mySensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
//            float x = sensorEvent.values[0];
//            float y = sensorEvent.values[1];
//            float z = sensorEvent.values[2];
//
//            float a = (float) Math.toDegrees(x);
//            float b = (float) Math.toDegrees(y);
//            float c = (float) Math.toDegrees(z);
//
//            if ( a > 45 || b > 45 || c > 45)
//                Toast.makeText(getApplicationContext(), a + " " + b + " " + c, Toast.LENGTH_SHORT).show();
//        }

        if (cachedAccelerometer != null && cachedMagnetometer != null) {
            float R[] = new float[9];
            float I[] = new float[9];

            if (SensorManager.getRotationMatrix(R, I, cachedAccelerometer, cachedMagnetometer)) {

                // orientation contains azimut, pitch and roll
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);

                float azimut = orientation[0];
            }
        }

        //writeAllReadingsToFile(currTime - startTime);

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
        degreesTextView = (TextView) findViewById(R.id.degreeCounter);

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
