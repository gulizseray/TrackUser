package trackuser.hw2.cs498.illinois.edu.trackuser;

import android.net.wifi.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.net.wifi.WifiManager;
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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by gulizseray on 3/24/15.
 */

/* HW2 - Track User
* */


public class MainActivity extends ActionBarActivity implements SensorEventListener {

    // Sensors
    private SensorManager mSensorManager;
    private Sensor senAccelerometer;
    private Sensor senGyroscope;
    private Sensor senMagnetometer;
    private Sensor senLight;

    //Wi-Fi
    private WifiManager wifiManager;
    WifiReceiver receiverWifi; //Broadcast receiver for Wi-Fi
    List<ScanResult> wifiList;
    private int numDiscoveredWiFiDevices = 0;
    public static final int WIFI_SCAN_PERIOD = 5; //in seconds
    public static final int AUDIO_SCAN_PERIOD = 10; //in mili seconds

    private long startTime = System.currentTimeMillis();
    public static final float ALPHA = (float) 0.7;
    public static final float FILTER_COEFFICIENT = 0.98f;
    private static final float rToD = (float) (180 / Math.PI);

    // File related variables
    private File readingsFile;
    private FileOutputStream readingsOutputStream;
    private String sensorFileName = "sensorReadings.csv";

    // Cached values for the sensor readings
    float[] cachedAccelerometer = new float[3];
    float cachedAcceleration = 0;
    float[] cachedGyroscope = new float[3];
    float[] cachedMagnetometer = new float[3];
    double cachedAudioLevel = 0;
    float cachedLightSensor = 0;

    private boolean isFusedAngleUpdated = false;
    // Unknowns
    private int numSteps = 0;
    private float totalTurn = 0;
    private float oldFusedAngle = 0;
    //All angles in radians
    private float angleGyro = 0;
    float angleMag = 0;
    Float angleMagInitial = null;
    public float fusedAngle = 0;

    // Previous update times for unknowns
    private long lastStepCountTime = startTime;
    private long lastGyroTime = startTime;
    private long lastMagnetTime = startTime;
    private long lastAccMagTime = startTime;
    private long lastTotalTurnUpdateTime = startTime;
    // TextViews
    private TextView stepsTextView = null;
    private TextView distanceTextView = null;
    private TextView currentDegreesTextView = null;
    private TextView totalDegreesTextView = null;
    private TextView gyroMeasurementTextView = null;
    private TextView magMeasurementTextView = null;
    private TextView errorDetectionTextView = null;
    private TextView accMagTextView = null;


    public void initializeFile() {
        try {
            readingsFile = new File(Environment.getExternalStorageDirectory(), sensorFileName);
            readingsOutputStream = new FileOutputStream(readingsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initializeSensors() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //initialize accelerometer
        senAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);

        //initialize gyroscope
        senGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(this, senGyroscope, SensorManager.SENSOR_DELAY_FASTEST);

        //initialize magnetometer
        senMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, senMagnetometer, SensorManager.SENSOR_DELAY_FASTEST);

        //initialize light sensor
        senLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener(this, senLight, SensorManager.SENSOR_DELAY_FASTEST);

        //initialize wifi
        initializeWifi();

        //initialize microphone
        recordAudioInBackGround.start();

        // start writing total turn angle
        //calculateTotalTurn.start();
    }

    public boolean isAllZeros(float[] a) {
        for (int i = 0; i < a.length; i++)
            if (a[i] != 0)
                return false;
        return true;

    }

    public float[] lowPassFilter(float[] input, float[] output) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    public void calculateAccMagOrientation() {

        float R[] = new float[9];
        float I[] = new float[9];

        if (SensorManager.getRotationMatrix(R, I, cachedAccelerometer, cachedMagnetometer)) {

            // orientation contains azimut, pitch and roll
            float orientation[] = new float[3];
            SensorManager.getOrientation(R, orientation);

            float azimut = orientation[0] * (-1); //needs to be multiplied by -1 to get correct result?

            if (angleMagInitial == null)
                angleMagInitial = new Float(azimut);

            angleMag = (FILTER_COEFFICIENT) * angleMag + (1 - FILTER_COEFFICIENT) * azimut;

            accMagTextView.setText(String.format("%.2f", Math.toDegrees(angleMag - angleMagInitial)));
        }
    }

    public void calculateMagOrientation() {

        //calculate the angle between phone and north (assuming phone's orientation is parallel to the ground)
        float angleNew = (float) Math.atan2((double) cachedMagnetometer[0], cachedMagnetometer[1]);

        if (angleMagInitial == null)
            angleMagInitial = new Float(angleNew);

        if (angleNew < -0.5 * Math.PI && angleMag > 0.5 * Math.PI) {
            angleMag = FILTER_COEFFICIENT * (angleMag) + (1 - FILTER_COEFFICIENT) * (angleNew + (float) (2 * Math.PI));
            angleMag -= (angleMag > Math.PI) ? 2.0 * Math.PI : 0;
        } else if (angleNew > 0.5 * Math.PI && angleMag < -0.5 * Math.PI) {
            angleMag = FILTER_COEFFICIENT * (float) (angleMag + 2 * Math.PI) + (1 - FILTER_COEFFICIENT) * angleNew;
            angleMag -= (angleMag > Math.PI) ? 2.0 * Math.PI : 0;
        } else {
            angleMag = (FILTER_COEFFICIENT) * angleMag + (1 - FILTER_COEFFICIENT) * angleNew;
        }

        //update display of the angle
        magMeasurementTextView.setText(String.format("Cal: %.3f %n Uncal: %.3f", Math.toDegrees(angleMag - angleMagInitial), Math.toDegrees(angleMag)));

    }

    public void calculateGyroOrientation(long deltaT) {
        float fusedAngleNew = 0;

        //perform numeric integration
        float addedTurn = cachedGyroscope[2] * deltaT / 1000;

        //add original value to angle from initial and make sure, it doesn't exceed 360º (361º=1º)
        //angleGyro += addedTurn;
        angleGyro = addedTurn;


        float angleMagToInitial = (angleMag - ((angleMagInitial == null) ? 0 : angleMagInitial));

        if (angleMag < -0.5 * Math.PI && fusedAngle > 0.5 * Math.PI) {
            fusedAngleNew = FILTER_COEFFICIENT * (fusedAngle + angleGyro) + (1 - FILTER_COEFFICIENT) * (angleMag + (float) (2 * Math.PI));
            fusedAngleNew -= (fusedAngleNew > Math.PI) ? 2.0 * Math.PI : 0;
        } else if (angleMag > 0.5 * Math.PI && fusedAngle < -0.5 * Math.PI) {
            fusedAngleNew = FILTER_COEFFICIENT * (float) (fusedAngle + angleGyro + 2 * Math.PI) + (1 - FILTER_COEFFICIENT) * angleMag;
            fusedAngleNew -= (fusedAngleNew > Math.PI) ? 2.0 * Math.PI : 0;
        } else {
            fusedAngleNew = FILTER_COEFFICIENT * (fusedAngle + angleGyro) + (1 - FILTER_COEFFICIENT) * angleMag;
        }


        //add absolute value to total turn
        //if( deltaT > 7)
        float diff = Math.abs(Math.abs(fusedAngleNew) - Math.abs(fusedAngle));
        if( Math.abs(cachedGyroscope[2]) > 0.03 && System.currentTimeMillis() - startTime > 5000 )
            totalTurn += diff;
//
        fusedAngle = fusedAngleNew;

//    fusedAngle += 2 * Math.PI;
//    fusedAngle %= 2 * Math.PI;


        float angleToDisplay = (float) (fusedAngle - ((angleMagInitial == null) ? 0 : angleMagInitial));
        if (angleToDisplay < - Math.PI)
            angleToDisplay += 2.0 * Math.PI;
        else if (angleToDisplay > Math.PI)
            angleToDisplay -= 2.0  * Math.PI;


//        if(angleMagInitial != null && !isFusedAngleUpdated){
//            isFusedAngleUpdated = true;
//            oldFusedAngle = fusedAngle;
//            lastTotalTurnUpdateTime = System.currentTimeMillis();
//        }
//        currentDegreesTextView.setText(String.format("%.2f", Math.toDegrees(angleToDisplay)) + ", " + String.format("%.2f", Math.toDegrees(totalTurn)));
        currentDegreesTextView.setText(String.format("%.2f", Math.toDegrees(angleToDisplay)));
        totalDegreesTextView.setText(String.format("%.2f", Math.toDegrees(totalTurn)));
        gyroMeasurementTextView.setText(String.format("%.3f updating %n %d, %.8f", cachedGyroscope[2], deltaT, addedTurn));
    }

    public void countSteps() {
        long currTime = System.currentTimeMillis();

        if (cachedAccelerometer[2] > 11.4) {
            // There needs to be at least 300ms between two peaks, otherwise it isn't a step.
            if (currTime - lastStepCountTime > 300) {
                numSteps++;
                lastStepCountTime = currTime;
                stepsTextView.setText(String.valueOf(numSteps));
                distanceTextView.setText(String.format("%.1f", numSteps * 0.9) + "m");
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        long currentTime = System.currentTimeMillis();


        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            cachedAccelerometer = lowPassFilter(sensorEvent.values.clone(), cachedAccelerometer);
            countSteps();
            //calculateAccMagOrientation();
        } else if (mySensor.getType() == Sensor.TYPE_GYROSCOPE) {
//            long currentTime = System.currentTimeMillis();
            System.arraycopy(sensorEvent.values, 0, cachedGyroscope, 0, 3);
            long deltaT = (currentTime - lastGyroTime);

            calculateGyroOrientation(deltaT);

            //change lastGyroTime to current time
            lastGyroTime = currentTime;

        } else if (mySensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(sensorEvent.values, 0, cachedMagnetometer, 0, 3);

            float deltaT = System.currentTimeMillis() - lastMagnetTime;

            calculateMagOrientation();

            //Set the last update time to the current time. (Might be a good idea to replace it with += deltaT)
            lastMagnetTime = System.currentTimeMillis();
        } else if (mySensor.getType() == Sensor.TYPE_LIGHT) {

            cachedLightSensor = sensorEvent.values[0];
        }

        writeAllReadingsToFile(currentTime - startTime);

//        if(isFusedAngleUpdated && (currentTime - lastTotalTurnUpdateTime) > 2000){
//            totalTurn += Math.abs(Math.abs(oldFusedAngle) - Math.abs(fusedAngle));
//            oldFusedAngle = fusedAngle;
//
//            float angleToDisplay;
//
//            lastTotalTurnUpdateTime = currentTime;
//
//        }

    }

    public String constructWiFiData() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(numDiscoveredWiFiDevices + ",");

        //Print MAC address and RSSIs for discovered APs
        if (wifiList != null) {
            for (int i = 0; i < wifiList.size(); i++) {
                stringBuilder.append(wifiList.get(i).BSSID + "," + wifiList.get(i).level + ",");
            }
        }

        return stringBuilder.toString();
    }

    public void writeAllReadingsToFile(long timestamp) {
        String acc = cachedAccelerometer[0] + "," + cachedAccelerometer[1] + "," + cachedAccelerometer[2] + ",";
        String gyr = cachedGyroscope[0] + "," + cachedGyroscope[1] + "," + cachedGyroscope[2] + ",";
        String mag = cachedMagnetometer[0] + "," + cachedMagnetometer[1] + "," + cachedMagnetometer[2] + ",";


        String all = timestamp + "," + numSteps + "," + fusedAngle + "," + angleMag + "," + acc + gyr + mag + String.valueOf(cachedLightSensor) + "," + constructWiFiData() + cachedAudioLevel + "\n";
        try {
            readingsOutputStream.write(all.getBytes());
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
        mSensorManager.registerListener(this, senMagnetometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, senLight, SensorManager.SENSOR_DELAY_FASTEST);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stepsTextView = (TextView) findViewById(R.id.stepCounter);
        distanceTextView = (TextView) findViewById(R.id.distanceValue);
        totalDegreesTextView = (TextView) findViewById(R.id.degreeCounter);
        currentDegreesTextView = (TextView) findViewById(R.id.currentDegreeCounter);
        gyroMeasurementTextView = (TextView) findViewById(R.id.gyroValues);
        magMeasurementTextView = (TextView) findViewById(R.id.degreeCounterM);
        errorDetectionTextView = (TextView) findViewById(R.id.error_indicator);
        accMagTextView = (TextView) findViewById(R.id.acc_magneto_count);
        //audioTextView = (TextView) findViewById(R.id.audio_level);

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


    float newAngle = 0, oldAngle = 0;

    Thread calculateTotalTurn = new Thread(new Runnable() {
        @Override
        public void run() {

            while (true) {
                oldAngle = newAngle;
                newAngle = fusedAngle;
                try {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            totalDegreesTextView.setText(String.format("%.2f", newAngle - oldAngle));
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    });

    //Audio level recording
    private MediaRecorder mRecorder = null;

    public void startRecordingAudio() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile("/dev/null");
        try {
            mRecorder.prepare();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
        mRecorder.start();
    }

    public double getAudioAmplitude() {
        if (mRecorder != null)
            return mRecorder.getMaxAmplitude();
        else
            return 0;

    }

    Thread recordAudioInBackGround = new Thread(new Runnable() {
        @Override
        public void run() {
            startRecordingAudio();

            while (true) {
                cachedAudioLevel = getAudioAmplitude();
                try {
                    Thread.sleep(AUDIO_SCAN_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    //Wi-Fi Scanning
    public void initializeWifi() {
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled() == false) {
            Toast.makeText(getApplicationContext(), "Wi-Fi is disabled..Enabling it now.", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        receiverWifi = new WifiReceiver();
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        //setup timer to schedule WiFi scanning periodically
        Timer timer = new Timer(true);
        TimerTask wiFiScanTask = new WiFiScanTask();
        timer.scheduleAtFixedRate(wiFiScanTask, 0, WIFI_SCAN_PERIOD * 1000);
    }

    class WiFiScanTask extends TimerTask {
        @Override
        public void run() {
            wifiManager.startScan();
        }
    }


    class WifiReceiver extends BroadcastReceiver {

        // This method is called when number of wifi connections changes
        public void onReceive(Context c, Intent intent) {

            StringBuilder sb = new StringBuilder();
            wifiList = wifiManager.getScanResults();
            sb.append("\nNumber Of Wi-Fi connections :" + wifiList.size() + "\n\n");
            numDiscoveredWiFiDevices = wifiList.size();

//            for(int i = 0; i < wifiList.size(); i++){
//
//                sb.append(new Integer(i+1).toString() + ". ");
//                sb.append((wifiList.get(i)).toString());
//                sb.append("\n\n");
//            }
//            Toast.makeText(getApplicationContext(), sb.toString() , Toast.LENGTH_LONG).show();
        }

    }
}