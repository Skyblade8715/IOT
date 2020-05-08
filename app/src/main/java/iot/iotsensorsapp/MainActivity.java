package iot.iotsensorsapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.UiThread;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.agilie.agmobilegiftinterface.InterfaceInteractorImpl;
import com.agilie.agmobilegiftinterface.gravity.GravityControllerImpl;
import com.agilie.agmobilegiftinterface.shake.ShakeBuilder;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {

    private MyMediaRecorder audio;
    private double lastLevel = 0;
    private Thread thread;
    private static final int SAMPLE_DELAY = 75,
                            sampleRate = 44100;
    private int bufferSize,
            AUDIO_PERMISSION_CODE = 4444,
            STORAGE_PERMISSION_CODE = 5555;
    TextView textView;
    TextView display;
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
        display = findViewById(R.id.display);

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

    public  void controlGravity_Shake(){
        if(shaking){
            shaker.shakeMyActivity();
        } else {
            shaker.stopAnimation();
        }
        if(gravity){
            gravityController.start();
            button1.setText("Eh... Should I help you with that?");
            textView.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_in));
        } else {
            gravityController.stop();
            button1.setText("Life is nice!");
            textView.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_in));
        }
    }

    public void setTextView(int text){
        textView.setText(text);
        textView.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_in));
    }

    public void startRecord(File fFile){
        try{
            audio.setMyRecAudioFile(fFile);
            if (audio.startRecording()) {
                startListenAudio();
            }else{
                Toast.makeText(this, "REEEEEEEEEEE", Toast.LENGTH_SHORT).show();
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (thread != null) {
            thread = null;
        }
        audio.stopRecording();
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

//    private void record(){
//        audio.startRecording();
//        thread = new Thread(new Runnable() {
//            public void run() {
//                while(thread != null && !thread.isInterrupted()){
//                    try{
//                        Thread.sleep(SAMPLE_DELAY);
//                    }
//                    catch(InterruptedException ie){
//                        ie.printStackTrace();
//                    }
//                    runOnUiThread(new Runnable() {
//
//                        @Override
//                        public void run() {
//                            display.setText(lastLevel + "db");
//                            if(lastLevel > 70){
//                                if(!gravity) {
//                                    gravity = true;
//                                    gravityController.start();
//                                    textView.setText(R.string.panic_reaction);
//                                    textView.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_in));
//                                }
//                            } else if(lastLevel > 30){
//                                if(!shaking) {
//                                    shaking = true;
//                                    shaker.shakeMyActivity();
//                                    if (!gravity) {
//                                        textView.setText(R.string.annoyed_reaction);
//                                        textView.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_in));
//                                    }
//                                }
//                            } else{
//                                if(!gravity && shaking){
//                                    textView.setText(R.string.calm_reaction);
//                                    textView.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_in));
//                                    shaker.stopAnimation();
//                                    shaking = false;
//                                }
//                            }
//                        }
//                    });
//                }
//            }
//        });
//        thread.start();
//    }
