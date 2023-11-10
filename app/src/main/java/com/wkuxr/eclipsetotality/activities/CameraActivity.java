package com.wkuxr.eclipsetotality.activities;

import static com.wkuxr.eclipsetotality.activities.SendConfirmationActivity.prefs;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.InetAddresses;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.wkuxr.eclipsetotality.R;
import com.wkuxr.eclipsetotality.database.Metadata;
import com.wkuxr.eclipsetotality.database.MetadataDB;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;


public class CameraActivity extends AppCompatActivity {

    static CameraActivity singleton;

    private static final String TAG = "Camera2VideoImageActivi"; // The name of debug channel

    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private int mCaptureState = STATE_PREVIEW;
    private TextureView mTextureView;
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setupCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
    private CameraDevice mCameraDevice;
    private final CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            mMediaRecorder = new MediaRecorder();
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };
    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;
    private String mCameraId;
    private Size mPreviewSize;
    private ImageReader mImageReader;
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new
            ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    mBackgroundHandler.post(new ImageSaver(reader.acquireLatestImage()));
                }
            };

    // This class saves images
    private class ImageSaver implements Runnable {

        private final Image mImage;

        public ImageSaver(Image image) {
            mImage = image;
        }

        @Override
        public void run() {
            try {
                createImageFileName();
            } catch (IOException e) {
                e.printStackTrace();
            }

            ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);

            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(mImageFileName);
                fileOutputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();

                Intent mediaStoreUpdateIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaStoreUpdateIntent.setData(Uri.fromFile(new File(mImageFileName)));
                sendBroadcast(mediaStoreUpdateIntent);

                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private MediaRecorder mMediaRecorder;
    private int mTotalRotation;
    private CameraCaptureSession mPreviewCaptureSession;
    private final CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult captureResult) {
            switch (mCaptureState) {
                case STATE_PREVIEW:
                    // Do nothing
                    break;
                case STATE_WAIT_LOCK:
                    mCaptureState = STATE_PREVIEW;
                    Integer afState = captureResult.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                        //This line displays the popup
                        // that is shown after an image is taken. we don't particularly need it in the final product, but it is good for testing.
                        Toast.makeText(getApplicationContext(), "AF Locked!", Toast.LENGTH_SHORT).show();
                                /*try {
                                    CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                                    startStillCaptureRequest(builder);
                                    builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                                    builder.set(CaptureRequest.SENSOR_SENSITIVITY,100);
                                    startStillCaptureRequest(builder);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }*/
                    }
                    break;
            }
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

            process(result);
        }
    };

    private CaptureRequest.Builder mCaptureRequestBuilder;

    private File mImageFolder;
    private String mImageFileName;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private static class CompareSizeByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum(((long) lhs.getWidth() * lhs.getHeight()) -
                    ((long) rhs.getWidth() * rhs.getHeight()));
        }
    }

    /*@RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createImageFolder();

        mTextureView = findViewById(R.id.textureView);
        ImageButton mStillImageButton = findViewById(R.id.cameraImageButton2);
        mStillImageButton.setOnClickListener(v -> {
            //checkWriteStoragePermission();  // this was taken out because it causes the app to crash by asking
            // for permission twice. may be an issue that causes the app to crash upon startup currently.
            //try{
            //    startStillCaptureRequest(mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE));
            //} catch (CameraAccessException e){
            //    e.printStackTrace();
            //}

            lockFocus();
            try {
                CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                builder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF);
                builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
                builder.set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_OFF);
                builder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
                builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
                builder.set(CaptureRequest.CONTROL_AE_LOCK, true);
                builder.set(CaptureRequest.CONTROL_AWB_LOCK, true);

                startStillCaptureRequest(builder);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        });
    }*/

    //NTPUDPClient NTPClient;
    //long offset = 0;

    long startTime;
    long endTime;

    MetadataDB db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        singleton = this;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Thread NTPClientThread = new Thread(ntpClientRunnable){};
        //NTPClientThread.start();

        db = MetadataDB.Companion.createDB(this);

        //checkWriteStoragePermission();

        createImageFolder();

        mTextureView = findViewById(R.id.textureView);

        //get the start and end time of eclipse totality from SharedPreferences, default to Long.MAX_VALUE if not present so the camera sequence doesn't falsely trigger.
        SharedPreferences prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE);
        startTime = prefs.getLong("startTime", Long.MAX_VALUE);
        endTime = prefs.getLong("endTime", Long.MAX_VALUE);
    }

    /*Runnable ntpClientRunnable = () -> {
        NTPClient = new NTPUDPClient();
        NTPClient.setDefaultTimeout(2_000);
        InetAddress inetAddress;
        TimeInfo timeInfo;
        try {
            inetAddress = InetAddress.getByName("pool.ntp.org");
            timeInfo = NTPClient.getTime(inetAddress);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        timeInfo.computeDetails();

        offset = timeInfo.getOffset();
        Log.d("NTPTimingOffset","Offset is " + offset);
    };*/

    @Override
    protected void onStart() {
        super.onStart();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        prefs = singleton.getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE);
        prefs.edit().putInt("upload", -2).apply();

        //timer that takes images every 1 seconds for 20 seconds starting 15 seconds before t[c2], then another timer for images every 1s for 20s starting 5s before t[c3]
        //the next line is a testcase to make sure functionality works
        startTime = System.currentTimeMillis() + 60000; //TODO: remove for actual app releases
        endTime = startTime + 60000 * 5; //2 minutes after startTime TODO: remove for actual app releases
        long midTime = (endTime + startTime) / 2; //set time for midpoint photo for cropping basis
        Date mid = new Date(midTime);
        Date startC2 = new Date(startTime - 7000);
        Date endC2 = new Date(startTime + 3400);
        Date startC3 = new Date(endTime - 3000);
        Date endC3 = new Date(endTime + 7400);
        sequenceTimer = new Timer();
        //set timer to start captures at t[c2] - 7
        sequenceTimer.schedule(new StartSequenceTask(),startC2);
        //set timer to stop captures at t[c2] + 3
        sequenceTimer.schedule(new StopSequenceTask(), endC2);
        //set timer to take a single capture at midpoint
        sequenceTimer.schedule(new MidpointCaptureTask(), mid);
        //set timer to start captures at t[c3] - 3
        sequenceTimer.schedule(new StartSequenceTask(), startC3);
        //set timer to stop captures at t[c3] + 7
        sequenceTimer.schedule(new StopSequenceTask(), endC3);

        //set timer to switch to SendConfirmationActivity
        Date activityTimer = new Date(endTime + 20000);
        sequenceTimer.schedule(new SwitchActivityTask(), activityTimer);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(sequenceTimer != null){
            sequenceTimer.cancel();
        }
    }

    Timer sequenceTimer = null;
    Handler sequenceHandler = new Handler();
    Runnable sequenceRunnable = new Runnable(){
        @Override
        public void run(){
            startStillCaptureRequest();
            sequenceHandler.postDelayed(this, 500);//TODO: Lower delay if possible
        }
    };

    static class StartSequenceTask extends TimerTask {
        public void run(){
            singleton.sequenceHandler.postDelayed(singleton.sequenceRunnable, 500);//TODO: Lower delay if possible
        }
    }

    static class StopSequenceTask extends TimerTask {
        public void run(){
            singleton.sequenceHandler.removeCallbacks(singleton.sequenceRunnable);
            Log.d("STOP_CAPTURES", "Captures have stopped, close app and check gallery.");
        }
    }

    Runnable midpointRunnable = this::startStillCaptureRequest;

    static class MidpointCaptureTask extends TimerTask {
        public void run(){
            singleton.sequenceHandler.post(singleton.midpointRunnable);
            Log.d("MIDPOINT_CAPTURE", "Midpoint photo of eclipse has been taken.");
        }
    }

    static class SwitchActivityTask extends TimerTask {
        public void run(){
            Log.d("ACTIVITYSWITCH", "To " + SendConfirmationActivity.class.getName());
            Intent intent = new Intent(CameraActivity.singleton, SendConfirmationActivity.class);
            CameraActivity.singleton.startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
            connectCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();

        stopBackgroundThread();

        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if (hasFocus) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    boolean aeModeOffAvailable = false;
    @SuppressWarnings("SuspiciousNameCombination")
    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // sets the value of the highest resolution camera as the first one on the device, then iterates through every camera,
            // and determines if it is larger than the largest quality camera
            // and if it is, sets the currentLargest to the new largest camera
            // currentLargest stores the resolution value
            // currentLarge stores the cameraID value
            // camera1 stores the list of cameraDevices
            int index = 0;
            String[] camera1 = cameraManager.getCameraIdList();
            Size currentLargest = getResolution(cameraManager,camera1[index]);
            String currentLarge = camera1[index];
            // add a catch to ensure that camera id 0 is not front facing. if it is, and is the highest resolution, it will stay as the front camera.
            // it is very rare that a front camera will have the highest resolution, but it is better to be safe.
            if (cameraManager.getCameraCharacteristics(currentLarge).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT){
                currentLargest = getResolution(cameraManager, camera1[index+1]);
                currentLarge = camera1[index+1];
                index++;
            }
            for (int i = index+1; i < cameraManager.getCameraIdList().length; i++) {
                if (cameraManager.getCameraCharacteristics(camera1[i]).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                Size temp = getResolution(cameraManager, camera1[i]);
                if (Long.signum((currentLargest.getWidth() * (long)currentLargest.getHeight()) - (temp.getWidth() * (long)temp.getHeight())) < 0) {
                    currentLargest = temp;
                    currentLarge = camera1[i];
                }
            }
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(currentLarge);
            final int[] availableAeModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
            Log.d("AE_MODE", "Target mode ID: " + CameraCharacteristics.CONTROL_AE_MODE_OFF + "\nAvailable IDs:");
            for(int mode : availableAeModes){
                if (mode == CameraCharacteristics.CONTROL_AE_MODE_OFF) {
                    aeModeOffAvailable = true;
                    break;
                }
            }
            final Range<Integer> isoRange = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
            if(null != isoRange) {
                Log.d("AE_MODE","iso range => lower : " + isoRange.getLower() + ", higher : " + isoRange.getUpper());
            }
            final Range<Long>  exposureTimeRange = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
            if(null!=exposureTimeRange){
                Log.d("AE_MODE","exposure time range => lower : " + exposureTimeRange.getLower() + ", higher : " + exposureTimeRange.getUpper());
            }

            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
            mTotalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
            boolean swapRotation = mTotalRotation == 90 || mTotalRotation == 270;
            int rotatedWidth = width;
            int rotatedHeight = height;
            if (swapRotation) {
                rotatedWidth = height; // this is for rotation, it is possible it may call a warning, its no issue
                rotatedHeight = width; // this too
            }
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
            Size mImageSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.JPEG), rotatedWidth, rotatedHeight);
            mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, 1);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
            mCameraId = currentLarge;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // first checks for permission then opens the camera with the selected cameraID found in the setupCamera method.
    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    Toast.makeText(this,
                            "Video app required access to camera", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
                }, REQUEST_CAMERA_PERMISSION_RESULT);
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // the method that displays what the camera sees on screen. Since the resolution is currently stretched, in the xml,
    // we can fix it to display the image correctly. Not a huge deal since the user will not be looking at the screen during operation
    // and the image saved looks correct, but could be fixed for quality purposes.
    private void startPreview() {
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            Log.d(TAG, "onConfigured: startPreview");
                            mPreviewCaptureSession = session;
                            try {
                                mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),
                                        null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Log.d(TAG, "onConfigureFailed: startPreview");

                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // this is the code that actually captures an image. if you need it to take a burst photo, call this function multiple times.
    //private void startStillCaptureRequest(CaptureRequest.Builder builder) {
    private void startStillCaptureRequest() {
        try {
            //sometimes the cameraDevice just... becomes null??? Have no idea why it happens, but it causes this to crash on my phone (but interestingly, not on Starr's)
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //mCaptureRequestBuilder = builder;

            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, mTotalRotation);
            if(aeModeOffAvailable) {
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
                mCaptureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 64);  // 63 ISO
                mCaptureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 5000000L); // 1/200s
            }

            CameraCaptureSession.CaptureCallback stillCaptureCallback = new
                    CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                            super.onCaptureStarted(session, request, timestamp, frameNumber);
                        }
                    };
            //single request
            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), stillCaptureCallback, null);

            //repeating request
            /*mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), stillCaptureCallback, null);
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        mPreviewCaptureSession.stopRepeating();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            };
            Handler h = new Handler();
            h.postDelayed(r, 250);*/

            //burst request
            /*CaptureRequest request = mCaptureRequestBuilder.build();
            List<CaptureRequest> requests = new ArrayList<>();
            for(int i = 0; i < 30; i++){
                requests.add(request);
            }
            mPreviewCaptureSession.captureBurst(requests,stillCaptureCallback, null);*/
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // not closing the camera can cause a memory leak. as well as create privacy issues that can
    // develop into legal issues. when utilizing the camera always make sure to close it when not in use.
    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    private void startBackgroundThread() {
        mBackgroundHandlerThread = new HandlerThread("Camera2VideoImage");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // this determines which direction the phone is oriented. im assuming that we will want our images to be vertical so it may be possible
    // to remove this if not necessary. i would leave it for now though because removing it may cause further issues we would have
    // to fix
    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        List<Size> bigEnough = new ArrayList<>();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * height / width &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }

    // creates a folder in local files to store images at. it also stores them in the gallery
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createImageFolder() {
        //external directory at `/Pictures/`, will not be deleted upon uninstall
        File imageFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        //internal directory, will be deleted upon uninstall
        //File imageFile = getFilesDir().getAbsoluteFile();

        //make folder at above location named camera2VideoImage
        mImageFolder = new File(imageFile, "SunSketcher");
        if (!mImageFolder.exists()) {
            mImageFolder.mkdirs();
            prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE);
            SharedPreferences.Editor prefEdit = prefs.edit();
            prefEdit.putString("imageFolderDirectory", mImageFolder.getAbsolutePath());
            prefEdit.apply();
        }
    }

    // names the files. in here you may be able to change the file type based on the extension, might want to look into that if we want to save
    // RAW filetypes instead of jpg, which are lossy.
    private void createImageFileName() throws IOException {
        long timestampLong = System.currentTimeMillis();// + offset;
        String timestamp = "" + timestampLong;// + "NTP";

        //create image metadata
        SharedPreferences prefs = getSharedPreferences("eclipseDetails", Context.MODE_PRIVATE);
        float lat = prefs.getFloat("lat", 0f);
        float lon = prefs.getFloat("lon", 0f);
        float alt = prefs.getFloat("alt", 0f);

        //String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()); // also saves a timestamp which we can use to
        // create metadata files. Additionally, saves some weird number to the end of the filename. Not sure how to prevent that.
        String prepend = "IMAGE_" + timestamp + "_";
        File imageFile = File.createTempFile(prepend, ".jpg", mImageFolder);
        mImageFileName = imageFile.getAbsolutePath();

        db.addMetadata(new Metadata(mImageFileName, (double)lat, (double)lon, (double)alt, timestampLong));
    }

    private void lockFocus() {
        mCaptureState = STATE_WAIT_LOCK;
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), mPreviewCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    // Iterates through all the camera devices in the phone and returns the largest resolution camera and returns a resolution.
    // could not change it to return a cameraID so when implementing the method to find a cameraID be sure to create a string array
    // that stores the values of the cameraID's based on an iterator.

    @NonNull
    public Size getResolution(@NonNull final CameraManager cameraManager, @NonNull final String cameraId) throws CameraAccessException
    {
        final CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
        final StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        // if there is no camera
        if (map == null)
        {
            throw new IllegalStateException("Failed to get configuration map.");
        }

        // stores the output sizes in JPEG format. im not sure if this will cause an issue if we try to store RAW type files.
        // if errors occur when trying to set up RAW file types try changing this line to ImageFormat.RAW
        final Size[] choices = map.getOutputSizes(ImageFormat.JPEG);

        Arrays.sort(choices, Collections.reverseOrder((lhs, rhs) -> {
            // Cast to ensure the multiplications won't overflow
            return Long.signum((lhs.getWidth() * (long)lhs.getHeight()) - (rhs.getWidth() * (long)rhs.getHeight()));
        }));

        return choices[0];
    }

}
