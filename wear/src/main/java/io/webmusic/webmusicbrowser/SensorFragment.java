package io.webmusic.webmusicbrowser;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import io.webmusic.webmusicbrowser.MainActivity;
import io.webmusic.webmusicbrowser.R;

import javis.wearsyncservice.Constant;

public class SensorFragment extends Fragment implements SensorEventListener {

    private static final float SHAKE_THRESHOLD = 1.1f;
    private static final int SHAKE_WAIT_TIME_MS = 250;
    private static final int SHAKE_WAIT_PLAY_TIME_MS = 140;
    private static final float ROTATION_THRESHOLD = 2.0f;
    private static final int ROTATION_WAIT_TIME_MS = 100;

    private float x,y,z;
    private final float GAIN = 0.9f;
    private final String TAG = MainActivity.class.getName();

    private View mView;
    private TextView mTextTitle;
    private TextView mTextValues;
    private Button mButton;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private int mSensorType;
    private long mShakeTime = 0;
    private long mRotationTime = 0;

    private TextView mTextView;
    private TextView mMotionTextView;
    private TextView mClockView;

    public static SensorFragment newInstance(int sensorType) {
        SensorFragment f = new SensorFragment();

        // Supply sensorType as an argument
        Bundle args = new Bundle();
        args.putInt("sensorType", sensorType);
        f.setArguments(args);

        return f;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if(args != null) {
            mSensorType = args.getInt("sensorType");
        }

        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(mSensorType);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.sensor, container, false);

        mTextTitle = (TextView) mView.findViewById(R.id.text_title);
        mTextTitle.setText(mSensor.getStringType());
        mTextValues = (TextView) mView.findViewById(R.id.text_values);

        mTextView = (TextView) mView.findViewById(R.id.text);
        mMotionTextView = (TextView) mView.findViewById(R.id.motion);
        mClockView = (TextView) mView.findViewById(R.id.clock);

        mButton = (Button) mView.findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)getActivity()).fireMessage("Play by Button");
            }
        });
        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // If sensor is unreliable, then just return
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }

        mTextValues.setText(
                "x = " + Float.toString(event.values[0]) + "\n" +
                        "y = " + Float.toString(event.values[1]) + "\n" +
                        "z = " + Float.toString(event.values[2]) + "\n"
        );

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            detectShake(event);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            detectRotation(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    // References:
    //  - http://jasonmcreynolds.com/?p=388
    //  - http://code.tutsplus.com/tutorials/using-the-accelerometer-on-android--mobile-22125
    private void detectShake(SensorEvent event) {
        Log.i(TAG, "Sensor.TYPE_ACCELEROMETER");

        long now = System.currentTimeMillis();

        // sound
        if((now - mShakeTime) > SHAKE_WAIT_PLAY_TIME_MS) {
            x = (x * GAIN + event.values[0] * (1 - GAIN));
            y = (y * GAIN + event.values[1] * (1 - GAIN));
            z = (z * GAIN + event.values[2] * (1 - GAIN));

            int motion;
            motion = detectMotion(x, y, z);
            if (motion > 0) {
                String message = String.format("motion: %s\nX : %f\nY : %f\nZ : %f", motion, x, y, z);
                ((MainActivity) getActivity()).fireMessage(message);
            }
        }

        // color
        if((now - mShakeTime) > SHAKE_WAIT_TIME_MS) {
            mShakeTime = now;

            float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
            float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
            float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;

            // gForce will be close to 1 when there is no movement
            float gForce = (float) Math.sqrt(gX*gX + gY*gY + gZ*gZ);

            // Change background color if gForce exceeds threshold;
            // otherwise, reset the color
            if(gForce > SHAKE_THRESHOLD) {
                //mView.setBackgroundColor(Color.rgb(0, 100, 0));
                mView.setBackgroundColor(Color.rgb(255, 193, 7));
            }
            else {
                //mView.setBackgroundColor(Color.BLACK);
                mView.setBackgroundColor(Color.WHITE);
            }
        }
    }

    private void detectRotation(SensorEvent event) {
        Log.i(TAG, "Sensor.TYPE_GYROSCOPE");

        long now = System.currentTimeMillis();

        if((now - mRotationTime) > ROTATION_WAIT_TIME_MS) {
            mRotationTime = now;

            // Change background color if rate of rotation around any
            // axis and in any direction exceeds threshold;
            // otherwise, reset the color
            if(Math.abs(event.values[0]) > ROTATION_THRESHOLD ||
                    Math.abs(event.values[1]) > ROTATION_THRESHOLD ||
                    Math.abs(event.values[2]) > ROTATION_THRESHOLD) {
                //mView.setBackgroundColor(Color.rgb(0, 100, 0));
                mView.setBackgroundColor(Color.rgb(0, 0, 100));
            }
            else {
                //mView.setBackgroundColor(Color.BLACK);
                mView.setBackgroundColor(Color.WHITE);
            }
        }
    }


    /**
     * 超適当な判定
     *
     */
    float ox,oy,oz;
    int delay;
    private int detectMotion(float x, float y, float z) {
        int diffX = (int)((x - ox)*10);
        int diffY = (int)((y - oy)*10);
        int diffZ = (int)((z - oz)*10);
        int motion = 0;

        //Log.d(TAG, "s:" + diffX + "/" + diffY + "/" + diffZ + " - " + (int)x + "/" + (int)y + "/" + (int)z);
        if (Math.abs(diffZ) > 20) {
            Log.d(TAG, "Z"); // upper!
            motion = 2;
            delay = 4;
        } else if (Math.abs(diffY) > 20) {
            Log.d(TAG, "Y"); // hook!
            motion = 3;
            delay = 4;
        } else if (diffX > 10) {
            if (delay == 0) {
                Log.d(TAG, "X"); // punch!
                motion = 1;
            }
        }

        if (delay > 0) delay--;
        ox = x;
        oy = y;
        oz = z;
        return motion;
    }
}
