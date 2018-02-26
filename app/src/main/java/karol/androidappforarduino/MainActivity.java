package karol.androidappforarduino;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.View;
import android.widget.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener{
    private static final String TAG = "MainActivity";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;
    private ConnectThread mConnectThread;
    private BluetoothSocket mmSocket;

    private SensorManager mSensorManager;
    private Sensor mLux;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mMagnetic;
    private int selectedSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if ((mLux = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)) == null)
        {
            message("Light sensor not found");
            RadioButton mRadio = (RadioButton) findViewById(R.id.radio1);
            mRadio.setVisibility(View.GONE);
        }
        if ((mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)) == null)
        {
            message("Accelerometer not found");
            RadioButton mRadio = (RadioButton) findViewById(R.id.radio2);
            mRadio.setVisibility(View.GONE);
        }
        if ((mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)) == null)
        {
            message("Gyroscope not found");
            RadioButton mRadio = (RadioButton) findViewById(R.id.radio3);
            mRadio.setVisibility(View.GONE);
        }
        if ((mMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)) == null)
        {
            message("Magnetometer not found");
            RadioButton mRadio = (RadioButton) findViewById(R.id.radio4);
            mRadio.setVisibility(View.GONE);
        }

        TextView mtext = (TextView) findViewById(R.id.textView1);
        String tmpString = "Connection status: not connected";
        mtext.setText(tmpString);

        if (mBluetoothAdapter == null) {
            message("Device does not support Bluetooth");
            finish();
        }

        final Button btnConnect = (Button) findViewById(R.id.buttonBtResume);
        btnConnect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (!mBluetoothAdapter.isEnabled()) {
                    Log.d(TAG, "Bluetooth is off - trying to turn it on");
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, 1);
                    message("To connect press button \"Connect\" again");
                }
                else if (mmSocket == null)
                {
                    ImageView img = (ImageView) findViewById(R.id.imageView);
                    img.setImageResource(R.drawable.no);
                    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                    if (pairedDevices.size() > 0) {
                        // assumed there is only one available
                        for (BluetoothDevice device : pairedDevices) {
                            mDevice = device;
                        }
                    }

                    Button btnOn = (Button) findViewById(R.id.buttonOn) ;
                    Button btnOFF = (Button) findViewById(R.id.buttonOFF) ;

                    mConnectThread = new ConnectThread(mDevice);
                    mConnectThread.start();
                    try {
                        mConnectThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(mmSocket.isConnected())
                    {
                        Log.d(TAG, "Connected to socket");
                        TextView mText = (TextView) findViewById(R.id.textView1);
                        String tmpString = "Connection status: connected to " + mDevice.getName();
                        mText.setText(tmpString);
                        img.setImageResource(R.drawable.yes);
                        btnConnect.setVisibility(View.GONE);

                        btnOn.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View view) {
                                // turn on LED
                                mConnectThread.mConnectedThread.write("a".getBytes());
                            }
                        });
                        btnOFF.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View view) {
                                // turn off LED
                                mConnectThread.mConnectedThread.write("b".getBytes());
                            }
                        });
                    }
                    else
                    {
                        Log.d(TAG, "Unable to connect");
                        TextView mText = (TextView) findViewById(R.id.textView1);
                        String tmpString = "Connection status: could not connect";
                        mText.setText(tmpString);
                    }
                }
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (selectedSensor == 2) {
                if(mConnectThread != null && mConnectThread.mConnectedThread != null) {
                    // In this example, alpha is calculated as t / (t + dT),
                    // where t is the low-pass filter's time-constant and
                    // dT is the event delivery rate.

                    final double alpha = 0.8;
                    double gravity[] = new double[3];
                    gravity[2] = 9.8;
                    double linear_acceleration[] = new double[3];

                    // Isolate the force of gravity with the low-pass filter.
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                    // Remove the gravity contribution with the high-pass filter.
                    linear_acceleration[0] = event.values[0] - gravity[0];
                    linear_acceleration[1] = event.values[1] - gravity[1];
                    linear_acceleration[2] = event.values[2] - gravity[2];

                    String tmpString = "t=" + event.timestamp + "x=" + linear_acceleration[0] + "y=" + linear_acceleration[1] + "z=" + linear_acceleration[2] + ";";
                    mConnectThread.mConnectedThread.write(tmpString.getBytes());
                    Log.d(TAG, "Sending accelerometer data\n" + tmpString);
                    TextView mText = (TextView) findViewById(R.id.textView4);
                    String tmp = "Sending data [accelerometer]: \n\n" + tmpString;
                    mText.setText(tmp);
                }
            }
        } else if (sensor.getType() == Sensor.TYPE_LIGHT) {
            if(selectedSensor == 1) {
                float luxValue = event.values[0];
                String tmpString = "t=" + event.timestamp + "x=" + luxValue + ";";
                if(mConnectThread != null && mConnectThread.mConnectedThread != null) {
                    mConnectThread.mConnectedThread.write(tmpString.getBytes());
                    Log.d(TAG, "Sending light sensor data\n" + luxValue);
                    TextView mText = (TextView) findViewById(R.id.textView4);
                    String tmp = "Sending data [light]: \n\n" + tmpString;
                    mText.setText(tmp);
                }
            }
        } else if(sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            if (selectedSensor == 3) {
                if(mConnectThread != null && mConnectThread.mConnectedThread != null) {
                    double myValues[] = new double[3];

                    myValues[0] = event.values[0];
                    myValues[1] = event.values[1];
                    myValues[2] = event.values[2];

                    String tmpString = "t=" + event.timestamp + "x=" + myValues[0] + "y=" + myValues[1] + "z=" + myValues[2] + ";";
                    mConnectThread.mConnectedThread.write(tmpString.getBytes());
                    Log.d(TAG, "Sending gyroscope data " + tmpString);
                    TextView mText = (TextView) findViewById(R.id.textView4);
                    String tmp = "Sending data [gyroscope]: \n\n" + tmpString;
                    mText.setText(tmp);
                }
            }
        } else if(sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            if (selectedSensor == 4) {
                if(mConnectThread != null && mConnectThread.mConnectedThread != null) {
                    double myValues[] = new double[3];

                    myValues[0] = event.values[0];
                    myValues[1] = event.values[1];
                    myValues[2] = event.values[2];

                    String tmpString = "t=" + event.timestamp + "x=" + myValues[0] + "y=" + myValues[1] + "z=" + myValues[2] + ";";
                    mConnectThread.mConnectedThread.write(tmpString.getBytes());
                    Log.d(TAG, "Sending magnetometer data " + tmpString);
                    TextView mText = (TextView) findViewById(R.id.textView4);
                    String tmp = "Sending data [magnetometer]: \n\n" + tmpString;
                    mText.setText(tmp);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "In onAccurancyChanged function");
    }

    @Override
    protected void onResume() {
        // Register a listener for the sensor
        super.onResume();
        Log.d(TAG, "In onResume function");
        mSensorManager.registerListener(this, mLux, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // Unregister the sensor when the activity pauses
        Log.d(TAG, "In onPause function");
        super.onPause();
        mSensorManager.unregisterListener(this);
    }


    private class ConnectThread extends Thread {
        private final BluetoothDevice mmDevice;
        private final UUID MY_UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        ConnectedThread mConnectedThread;

        ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmpSocket = null;
            mmDevice = device;
            try {
                tmpSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            }
            catch (IOException e) {
                Log.d(TAG, "Exception - granting and creating the socket", e);
            }
            mmSocket = tmpSocket;
        }
        public void run() {
            Log.d(TAG, "Running Connect Thread");
            mBluetoothAdapter.cancelDiscovery();
            try {
                Log.d(TAG, "Before connecting to socket");
                mmSocket.connect();
                Log.d(TAG, "After connecting to socket");
                mConnectedThread = new ConnectedThread(mmSocket);
                mConnectedThread.start();
            }
            catch (IOException connectException) {
                Log.d(TAG, "Exception - connectiong to socket", connectException);
                try {
                    mmSocket.close();
                    Log.d(TAG, "Closing socket");
                }
                catch (IOException closeException) {
                    Log.d(TAG, "Exception - closing socket", connectException);
                }
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final OutputStream mmOutStream;
        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            OutputStream tmpOut = null;
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.d(TAG, "Exception - getOutputStream", e);
            }
            mmOutStream = tmpOut;
        }
        public void run() {
            Log.d(TAG, "Running Connected Thread");
        }
        void write(byte[] bytes) {
            Log.d(TAG, "Function write() " + new String(bytes));
            try {
                mmOutStream.write(bytes);
            } catch
                    (IOException e) {
                Log.d(TAG, "Exception - writing to OutputStream", e);
            }
        }
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio1:
                if (checked) {
                    selectedSensor = 1;
                    break;
                }
            case R.id.radio2:
                if (checked){
                    selectedSensor = 2;
                    break;
                }
            case R.id.radio3:
                if (checked){
                    selectedSensor = 3;
                    break;
                }
            case R.id.radio4:
                if (checked){
                    selectedSensor = 4;
                    break;
                }
        }
    }

    private void message(String message) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }

    public void defaultMessage(View v) {
        message("First make the connection");
    }
}