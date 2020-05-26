package iot.iotsensorsapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.agilie.agmobilegiftinterface.InterfaceInteractorImpl;
import com.agilie.agmobilegiftinterface.gravity.GravityControllerImpl;
import com.agilie.agmobilegiftinterface.shake.ShakeBuilder;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;

    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];

    private MyMediaRecorder audio;
    private double lastLevel = 0;
    private Thread thread;
    private static final int SAMPLE_DELAY = 75,
                            sampleRate = 44100;
    private int bufferSize,
            AUDIO_PERMISSION_CODE = 4444,
            STORAGE_PERMISSION_CODE = 5555;
    TextView textView,
            orient_x,
            orient_y,
            orient_z;
    boolean permission = false,
            gravity = false,
            shaking = false;
    GravityControllerImpl gravityController;
    ShakeBuilder shaker;
    Button button1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewGroup group = findViewById(R.id.mainRootLayout);
        gravityController = new GravityControllerImpl(this, group);
        shaker = new InterfaceInteractorImpl().shake(this).build();

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

         if(!checkPermission()){
             requestPermission();
         }

        File dir = new File(this.getExternalFilesDir(null).getAbsolutePath() + "/IOTSensors");
        try {
            if (dir.mkdirs()) {
                Log.e("Dirs creator", "Directory 'IOTSensors' created");
            } else {
                Log.e("Dirs creator", "Directory 'IOTSensors' already existed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        audio = new MyMediaRecorder();
        button1 = findViewById(R.id.button);
        textView = findViewById(R.id.tv);
        orient_x = findViewById(R.id.orientation_x);
        orient_y = findViewById(R.id.orientation_y);
        orient_z = findViewById(R.id.orientation_z);

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(shaking) {
                    shaking = false;
                }
                if(gravity){
                    textView.setText(R.string.calm_reaction);
                    textView.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_in));
                    gravity = false;
                } else {
                    textView.setText(R.string.calm_reaction);
                    textView.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_in));
                }
                controlGravity_Shake();
            }
        });
    }


    private void startListenAudio() {
        thread = new Thread(new Runnable() {
            public void run() {
                while(thread != null) {
                    try {
                        Thread.sleep(SAMPLE_DELAY);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            lastLevel = audio.getMaxAmplitude();
                            if (lastLevel > 0 && lastLevel < 1000000) {
                                lastLevel = (20 * (float) (Math.log10(lastLevel)));
                                if (lastLevel > 70) {
                                    if (!gravity) {
                                        gravity = true;
                                        controlGravity_Shake();
                                        setTextView(R.string.panic_reaction);
                                    }
                                } else if (lastLevel > 50) {
                                    if (!shaking) {
                                        if (!gravity) {
                                            setTextView(R.string.annoyed_reaction);
                                        }
                                        shaking = true;
                                        controlGravity_Shake();
                                    }
                                } else {
                                    if (!gravity && shaking) {
                                        setTextView(R.string.calm_reaction);
                                        shaking = false;
                                        controlGravity_Shake();
                                    }
                                }
                            }
                        }
                    });
                }
            }
        });
        thread.start();
    }

    public void controlGravity_Shake(){
        if(shaking){
            shaker.shakeMyActivity();
        } else {
            shaker.stopAnimation();
        }
        if(gravity){
            gravityController.start();
            button1.setText("Eh... Should I help you with that?");
            button1.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_in));
        } else {
            gravityController.stop();
            button1.setText("Life is nice!");
            button1.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_in));
            orient_x.setText("I'm a compass!");
            orient_x.setTextColor(Color.BLACK);
            orient_y.setText("I measure pitch!");
            orient_y.setTextColor(Color.BLACK);
            orient_z.setText("I measure roll!");
            orient_z.setTextColor(Color.BLACK);
        }
    }

    public void setTextView(int text){
        textView.setText(text);
        textView.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_in));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            Log.i("OrientationTestActivity", String.format("Orientation: %f, %f, %f",
                    mOrientation[0], mOrientation[1], mOrientation[2]));
            if(gravity) {
                orientationSensorsHotCold(mOrientation[0], mOrientation[1], mOrientation[2]);
                if (mOrientation[0] > 1.2 && mOrientation[0] < 2.0) {
                    if (mOrientation[1] > 0.3 && mOrientation[1] < 1.2) {
                        if (mOrientation[2] > -0.8 && mOrientation[2] < 0.7) {
                            gravity = false;
                            controlGravity_Shake();
                            Log.e("OrientationTestActivity", String.format("Orientation: %f, %f, %f",
                                    mOrientation[0], mOrientation[1], mOrientation[2]));
                        }
                    }
                }
            }
        }
    }

    void orientationSensorsHotCold(double compass, double pitch, double roll){
        if(compass < 2.4 && compass > 0.8){
            orient_x.setText("Compass: hoot...");
            orient_x.setTextColor(Color.RED);
        } else {
            orient_x.setText("Compass: cold");
            orient_x.setTextColor(Color.BLUE);
        }
        if(pitch < 1.4 && pitch > 0.1){
            orient_y.setText("Pitch: hoot...");
            orient_y.setTextColor(Color.RED);
        } else {
            orient_y.setText("Pitch: cold");
            orient_y.setTextColor(Color.BLUE);
        }
        if(roll < 0.9  && roll > -1.0){
            orient_z.setText("Roll: hoot...");
            orient_z.setTextColor(Color.RED);
        } else {
            orient_z.setText("Roll: cold");
            orient_z.setTextColor(Color.BLUE);
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    public void startRecord(File fFile){
        try{
            audio.setMyRecAudioFile(fFile);
            if (audio.startRecording()) {
                startListenAudio();
            }else{
                Toast.makeText(this, "Error with audio recoding!", Toast.LENGTH_SHORT).show();
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }


    public File createFile(String fileName) {
        File myCaptureFile = new File(this.getExternalFilesDir(null).getAbsolutePath() + "/IOTSensors/"+ fileName);
        if (!myCaptureFile.exists()) {
            try {
                myCaptureFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return myCaptureFile;
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    AUDIO_PERMISSION_CODE);
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("BRAK UPRAWNIEŃ. Przydziel aplikacji uprawnienia do modyfikowania i usuwania zawartości karty SD.")
                    .setPositiveButton("Zamknij", dialogClickListener)
                    .show();

        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        File file = createFile("temp.amr");
        startRecord(file);
        mLastAccelerometerSet = false;
        mLastMagnetometerSet = false;
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (thread != null) {
            thread = null;
        }
        audio.stopRecording();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (thread != null) {
            thread = null;
        }
        audio.stopRecording();
    }

}
