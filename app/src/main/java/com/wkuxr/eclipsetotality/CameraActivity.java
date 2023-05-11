package com.wkuxr.eclipsetotality;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.wkuxr.eclipsetotality.databinding.ActivityCameraBinding;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraActivity extends AppCompatActivity {

    private ActivityCameraBinding binding;

    CameraDevice cameraDevice;

    SurfaceView surfaceView;
    Surface previewSurface;
    ImageReader imageReader;
    ImageReader mJpegImageReader;
    Size mJpegImageSize;
    Surface imageSurface;

    HandlerThread mBackgroundThread;
    Handler mBackgroundHandler;

    CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            createCameraSession();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
        }
    };

    CameraCaptureSession mCameraCaptureSession;

    ActivityResultLauncher<String[]> mPermissionResultLauncher;
    private boolean isReadPermissionGranted = false;
    private boolean isWritePermissionGranted = false;
    CameraManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mPermissionResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            if (result.get(Manifest.permission.READ_EXTERNAL_STORAGE != null)) {
                isReadPermissionGranted = result.get(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (result.get(Manifest.permission.WRITE_EXTERNAL_STORAGE != null)) {
                isWritePermissionGranted = result.get(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        });

        surfaceView = findViewById(R.id.surfaceView);
        previewSurface = surfaceView.getHolder().getSurface();

        manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        String camID = null;
        try {
            for (String cameraID : manager.getCameraIdList()) {
                CameraCharacteristics cc = manager.getCameraCharacteristics(cameraID);
                if (cc.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                camID = cameraID;
                StreamConfigurationMap map = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mJpegImageSize = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new Comparator<Size>() {
                            @Override
                            public int compare(Size lhs, Size rhs) {
                                return Long.signum(lhs.getWidth() * lhs.getHeight() -
                                        rhs.getWidth() * rhs.getHeight());
                            }
                        }
                );

                mJpegImageReader = ImageReader.newInstance(mJpegImageSize.getWidth(), mJpegImageSize.getHeight(), ImageFormat.JPEG, 50);

                break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


        mBackgroundThread = new HandlerThread("Camera2 background thread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            manager.openCamera(camID, mCameraDeviceStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        createCameraSession();

        CaptureRequest.Builder captureRequest = null;
        try {
            captureRequest = mCameraCaptureSession.getDevice().createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        captureRequest.addTarget(previewSurface);
        try {
            mCameraCaptureSession.setRepeatingRequest(captureRequest.build(),null,null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    @SuppressLint("NewApi")
    void createCameraSession(){
        CameraCharacteristics characteristics = null;
        try {
            characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        Size[] jpegSizes = null;
        if (characteristics != null) {
            jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
        }

        int width = 640;
        int height = 480;
        if (jpegSizes != null && 0 < jpegSizes.length) {
            width = jpegSizes[0].getWidth();
            height = jpegSizes[0].getHeight();
        }

        imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
        imageSurface = imageReader.getSurface();

        List<OutputConfiguration> targets = Arrays.asList(new OutputConfiguration(previewSurface),new OutputConfiguration(imageSurface));

        @SuppressLint({"NewApi", "LocalSuppress"}) SessionConfiguration sessionConfiguration = new SessionConfiguration(SessionConfiguration.SESSION_REGULAR, targets, null, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                mCameraCaptureSession = cameraCaptureSession;
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                Toast.makeText(getApplicationContext(), "create camera session failed", Toast.LENGTH_SHORT).show();
            }
        });

        try {
            cameraDevice.createCaptureSession(sessionConfiguration);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    void saveImage(View v) throws CameraAccessException {
        CaptureRequest.Builder captureRequest = mCameraCaptureSession.getDevice().createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);
        captureRequest.addTarget(imageReader.getSurface());
        mCameraCaptureSession.capture(captureRequest.build(),null,null);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return false;
    }
}