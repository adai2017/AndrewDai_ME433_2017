package com.example.andrew.me433_final;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;
import static android.graphics.Color.rgb;



public class MainActivity extends Activity implements TextureView.SurfaceTextureListener {

    // USB Initializations
    SeekBar myControlMaxSpeed;
    TextView myTextViewMaxSpeed;

    SeekBar myControlKp;
    TextView myTextViewKp;

    Button buttonStart;
    Button buttonStop;
    Button buttonUpdate;
    TextView myTextView2;
    ScrollView myScrollView;
    TextView myTextView3;

    private UsbManager manager;
    private UsbSerialPort sPort;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private SerialInputOutputManager mSerialIoManager;


    // Camera Initializations
    private Camera mCamera;
    private TextureView mTextureView;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Bitmap bmp = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);
    private Canvas canvas = new Canvas(bmp);
    private Paint paint1 = new Paint();
    private TextView mTextView;

    static long prevtime = 0; // for FPS calculation

    int tot = 0;
    int totY = 0;
    int COM = 0;
    int comArr = 0;

    int PWM1 = 0;
    int PWM2 = 0;
    int RC = 0;


    SeekBar myControl;
    SeekBar myControl2;
    TextView myTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // keeps the screen from turning off

        myControlMaxSpeed = (SeekBar) findViewById(R.id.seekMotor);

        myTextViewMaxSpeed = (TextView) findViewById(R.id.textViewSpeed);
        myTextViewMaxSpeed.setText("Enter whatever you Like!");

        myControlKp = (SeekBar) findViewById(R.id.seekKP);

        myTextViewKp = (TextView) findViewById(R.id.textViewKP);
        myTextViewKp.setText("Enter whatever you Like!");


        myTextView2 = (TextView) findViewById(R.id.textView02);
        myScrollView = (ScrollView) findViewById(R.id.ScrollView01);
        myTextView3 = (TextView) findViewById(R.id.textView03);
        buttonStart = (Button) findViewById(R.id.button3);
        buttonUpdate = (Button) findViewById(R.id.button1);
        buttonStop = (Button) findViewById(R.id.button2);

        buttonUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sendString = String.valueOf(10*myControlMaxSpeed.getProgress()) + ' ' + String.valueOf(COM) + ' ' + String.valueOf(myControlKp.getProgress()/100) + ' ' + String.valueOf(RC) + '\n';
                try {
                    sPort.write(sendString.getBytes(), 10); // 10 is the timeout
                } catch (IOException e) { }
            }
        });

        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sendString = String.valueOf(0) + ' ' + String.valueOf(0) + ' ' + String.valueOf(0) + '\n';
                try {
                    sPort.write(sendString.getBytes(), 10); // 10 is the timeout
                } catch (IOException e) { }
            }
        });

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sendString = String.valueOf(1) + ' ' + String.valueOf(240) + ' ' + String.valueOf(0) + '\n';
                try {
                    sPort.write(sendString.getBytes(), 10); // 10 is the timeout
                } catch (IOException e) { }
            }
        });



        manager = (UsbManager) getSystemService(Context.USB_SERVICE);


        mTextView = (TextView) findViewById(R.id.cameraStatus);

        myControl = (SeekBar) findViewById(R.id.seek1);
        myControl2 = (SeekBar) findViewById(R.id.seek2);



        // see if the app has permission to use the camera
        //ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 1);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            mSurfaceView = (SurfaceView) findViewById(R.id.surfaceview);
            mSurfaceHolder = mSurfaceView.getHolder();

            mTextureView = (TextureView) findViewById(R.id.textureview);
            mTextureView.setSurfaceTextureListener(this);

            // set the paintbrush for writing text on the image
            paint1.setColor(0xffff0000); // red
            paint1.setTextSize(24);

            mTextView.setText("started camera");
        } else {
            mTextView.setText("no camera permissions");
        }

        setMyControlListener();
        setMyControlListener2();
        setMyControlListenerMaxSpeed();
        setMyControlListenerKP();
    }


    private void setMyControlListenerMaxSpeed() {
        myControlMaxSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int progressChanged = 0;
            int progressAc = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
                progressAc = 10*progress;
                myTextViewMaxSpeed.setText("PWM Value: " +progressAc);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void setMyControlListenerKP() {
        myControlKp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            float progressChanged = 0;
            float progressKp = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
                progressKp = progress /  10;
                myTextViewKp.setText("Kp: " +progressKp);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {
                @Override
                public void onRunError(Exception e) {

                }

                @Override
                public void onNewData(final byte[] data) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.updateReceivedData(data);
                        }
                    });
                }
            };

    @Override
    protected void onPause(){
        super.onPause();
        stopIoManager();
        if(sPort != null){
            try{
                sPort.close();
            } catch (IOException e){ }
            sPort = null;
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        ProbeTable customTable = new ProbeTable();
        customTable.addProduct(0x04D8,0x000A, CdcAcmSerialDriver.class);
        UsbSerialProber prober = new UsbSerialProber(customTable);

        final List<UsbSerialDriver> availableDrivers = prober.findAllDrivers(manager);

        if(availableDrivers.isEmpty()) {
            //check
            return;
        }

        UsbSerialDriver driver = availableDrivers.get(0);
        sPort = driver.getPorts().get(0);

        if (sPort == null){
            //check
        }else{
            final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());
            if (connection == null){
                //check
                PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent("com.android.example.USB_PERMISSION"), 0);
                usbManager.requestPermission(driver.getDevice(), pi);
                return;
            }

            try {
                sPort.open(connection);
                sPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

            }catch (IOException e) {
                //check
                try{
                    sPort.close();
                } catch (IOException e1) { }
                sPort = null;
                return;
            }
        }
        onDeviceStateChange();
    }

    private void stopIoManager(){
        if(mSerialIoManager != null) {
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if(sPort != null){
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange(){
        stopIoManager();
        startIoManager();
    }

    private void updateReceivedData(byte[] data) {
        //do something with received data

        //for displaying:
        String rxString = null;
        try {
            rxString = new String(data, "UTF-8"); // put the data you got into a string
            myTextView3.append(rxString);
            myScrollView.fullScroll(View.FOCUS_DOWN);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mCamera = Camera.open();
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(640, 480);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY); // no autofocusing
        parameters.setAutoExposureLock(true); // keep the white balance constant
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90); // rotate to portrait mode

        try {
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
        } catch (IOException ioe) {
            // Something bad happened
        }
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mCamera.stopPreview();
        mCamera.release();
        return true;
    }

    private void setMyControlListener() {
        myControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int progressChanged = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void setMyControlListener2() {
        myControl2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int progressChanged = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    // the important function
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // every time there is a new Camera preview frame
        mTextureView.getBitmap(bmp);

        final Canvas c = mSurfaceHolder.lockCanvas();
        tot = 0;
        totY = 0;
        COM = 0;
        comArr = 0;

//        for (int j = 0; j < bmp.getHeight(); j += 8) {
            if (c != null) {
                int R = myControl.getProgress(); // grey threshold
                int T = myControl2.getProgress(); // green threshold
                int[] pixels = new int[bmp.getWidth()]; // pixels[] is the RGBA data
                int startY = bmp.getHeight()/2; // which row in the bitmap to analyze to read
                bmp.getPixels(pixels, 0, bmp.getWidth(), 0, startY, bmp.getWidth(), 1);

                int sum_mr = 0; // the sum of the mass times the radius
                int sum_m = 0; // the sum of the masses
                for (int i = 0; i < bmp.getWidth(); i++) {
                    if (((green(pixels[i]) - red(pixels[i])) > -R)&&((green(pixels[i]) - red(pixels[i])) < R)&&(green(pixels[i])  > T)) {
//                        pixels[i] = rgb(1, 1, 1); // set the pixel to almost 100% black

                        sum_m = sum_m + green(pixels[i])+red(pixels[i])+blue(pixels[i]);
                        sum_mr = sum_mr + (green(pixels[i])+red(pixels[i])+blue(pixels[i]))*i;
                    }


                }

                // only use the data if there were a few pixels identified, otherwise you might get a divide by 0 error
                if(sum_m>5){
                    COM = sum_mr / sum_m;
                }
                else{
                    COM = 0;
                }

//                tot++;
//                totY = totY + j;

                // update the row
                bmp.setPixels(pixels, 0, bmp.getWidth(), 0, startY, bmp.getWidth(), 1);



        }

        int kp = myControlKp.getProgress() / 10;

        int error = COM - 240; // 240 means the dot is in the middle of the screen
        if (error<0) { // slow down the left motor to steer to the left
            error  = -error;
            PWM2 = 10*myControlMaxSpeed.getProgress() - (kp*4*error/12);
            PWM1 = 10*myControlMaxSpeed.getProgress() + (kp*4*error/12);
            RC = 2000;
            if (PWM2 < 0){
                PWM2 = 0;
            }
        }
        else { // slow down the right motor to steer to the right
            PWM1 = 10*myControlMaxSpeed.getProgress() - (4*kp*error/12);
            PWM2 = 10*myControlMaxSpeed.getProgress() + (4*kp*error/12);
            RC = 7000;
            if (PWM1<0) {
                PWM1 = 0;
            }
        }




        String sendString = String.valueOf(PWM1) + ' ' + String.valueOf(PWM2) + ' ' + String.valueOf(kp) + ' ' + String.valueOf(RC) + '\n';
        try {
            sPort.write(sendString.getBytes(), 10); // 10 is the timeout
        } catch (IOException e) { }

        // draw a circle at some position
        canvas.drawCircle(COM, bmp.getHeight()/2, 10, paint1); // x position, y position, diameter, color

        // write the pos as text
        canvas.drawText("Grey Thresh = " + myControl.getProgress(), 10, 200, paint1);
        canvas.drawText("Green Thresh = " + myControl2.getProgress(), 10, 240, paint1);
        canvas.drawText("COM X: " + (COM), 10, 300, paint1);
        canvas.drawText("COM Y: " + (bmp.getHeight()/2), 10, 340, paint1);
        c.drawBitmap(bmp, 0, 0, null);
        mSurfaceHolder.unlockCanvasAndPost(c);

        // calculate the FPS to see how fast the code is running
        long nowtime = System.currentTimeMillis();
        long diff = nowtime - prevtime;
        mTextView.setText("FPS " + 1000 / diff);
        prevtime = nowtime;
    }
}