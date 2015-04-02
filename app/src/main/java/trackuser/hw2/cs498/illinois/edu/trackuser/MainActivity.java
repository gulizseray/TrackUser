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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
    public static final float ALPHA = (float) 0.7f;
    public static final float STEP_LENGTH = 0.9f;

    // File related variables
    private File readingsFile;
    private FileOutputStream readingsOutputStream;
    private String sensorFileName = "sensorReadings.csv";

    // Cached values for the sensor readings
    float[] cachedAccelerometer = new float[3];
    float[] cachedGyroscope = new float[3];
    float[] cachedMagnetometer = new float[3];
    double cachedAudioLevel = 0;
    float cachedLightSensor = 0;

    // Unknowns for counting steps
    private int numSteps = 0;

    //compass variables
    private float compassAngle = 0;
    private float initialCompassAngle = 0;

    // Unknowns for dead reckoning, all angles are in radian
    private float totalTurn = 0;
    public float angleToInitial = 0;
    public float accumAngle = 0;
    private static final float TURN_THRESHOLD = 0.1745f; // 10 degrees in radians

    // Previous update times for unknowns
    private long lastStepCountTime = startTime;
    private long lastGyroTime = startTime;
    private long lastMagnetTime = startTime;

    // TextViews
    private TextView stepsTextView = null;
    private TextView distanceTextView = null;
    private TextView currentDegreesTextView = null;
    private TextView totalDegreesTextView = null;
    private TextView compassTextView = null;

    private Button resetButton = null;


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

    public float[] lowPassFilter(float[] input, float[] output) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    public void countSteps() {
        long currTime = System.currentTimeMillis();

        if (cachedAccelerometer[2] > 11.4) {
            // There needs to be at least 300ms between two peaks, otherwise it isn't a step.
            if (currTime - lastStepCountTime > 300) {
                numSteps++;
                lastStepCountTime = currTime;
                stepsTextView.setText(String.valueOf(numSteps));
                distanceTextView.setText(String.format("%.1f", numSteps * STEP_LENGTH) + "m");
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

        } else if (mySensor.getType() == Sensor.TYPE_GYROSCOPE) {
            long deltaT = currentTime - lastGyroTime;

            // If there is a sign difference, then this is an extreme point
            if(Math.signum(cachedGyroscope[2]) == Math.signum(sensorEvent.values[2])){
                accumAngle += sensorEvent.values[2] * deltaT / 1000;
            }else{
                if(Math.abs(accumAngle) > TURN_THRESHOLD){
                    totalTurn += Math.abs(accumAngle);
                    angleToInitial += accumAngle;
                }
                accumAngle = 0;
            }

            currentDegreesTextView.setText(String.format("%.1f", Math.toDegrees(angleToInitial + accumAngle) % 360));
            totalDegreesTextView.setText(String.format("%.1f", Math.toDegrees(totalTurn + Math.abs(accumAngle))));

            System.arraycopy(sensorEvent.values, 0, cachedGyroscope, 0, 3);

            //change lastGyroTime to current time
            lastGyroTime = currentTime;

        } else if (mySensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(sensorEvent.values, 0, cachedMagnetometer, 0, 3);

            //calculate the angle using the compass;
            compassAngle = (float) Math.atan2(cachedMagnetometer[2], cachedMagnetometer[1]);

            //Set the last update time to the current time. (Might be a good idea to replace it with += deltaT)
            lastMagnetTime = System.currentTimeMillis();

            //update Gui
            compassTextView.setText(String.format("Mag: %.1f", (compassAngle-initialCompassAngle)*180/Math.PI));
        } else if (mySensor.getType() == Sensor.TYPE_LIGHT) {

            cachedLightSensor = sensorEvent.values[0];
        }

        writeAllReadingsToFile(currentTime - startTime);

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


        String all = timestamp + "," + numSteps + "," + angleToInitial + "," + totalTurn + "," + acc + gyr + mag + String.valueOf(cachedLightSensor) + "," + constructWiFiData() + cachedAudioLevel + "\n";
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
        compassTextView = (TextView) findViewById(R.id.compassValue);
        resetButton = (Button) findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                initializeFile();
                totalTurn = 0;
                accumAngle = 0;
                angleToInitial = 0;
                numSteps = 0;
                cachedAccelerometer = new float[3];
                cachedGyroscope = new float[3];
                cachedMagnetometer = new float[3];
                cachedAudioLevel = 0;
                cachedLightSensor = 0;
                initialCompassAngle = compassAngle;
                wifiList = null;
                stepsTextView.setText("0");
                distanceTextView.setText("0");
            }
        });

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
        }

    }
}