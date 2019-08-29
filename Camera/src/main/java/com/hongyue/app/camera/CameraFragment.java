package com.hongyue.app.camera;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaActionSound;
import android.os.Build;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hongyue.app.camera.config.Camera;
import com.hongyue.app.camera.config.CameraConfig;
import com.hongyue.app.camera.config.CameraConfigProvider;
import com.hongyue.app.camera.config.CameraConfigProviderImpl;
import com.hongyue.app.camera.config.Flash;
import com.hongyue.app.camera.config.MediaAction;
import com.hongyue.app.camera.config.Record;
import com.hongyue.app.camera.lifecycle.Camera1LifeCycle;
import com.hongyue.app.camera.lifecycle.Camera2LifeCycle;
import com.hongyue.app.camera.lifecycle.CameraLifeCycle;
import com.hongyue.app.camera.lifecycle.CameraView;
import com.hongyue.app.camera.listener.CameraVideoRecordTextListener;
import com.hongyue.app.camera.listener.ICameraFragment;
import com.hongyue.app.camera.listener.OnCameraResultListener;
import com.hongyue.app.camera.tool.CameraUtils;
import com.hongyue.app.camera.tool.DeviceUtils;
import com.hongyue.app.camera.tool.Size;
import com.hongyue.app.camera.tool.TimerTask;
import com.hongyue.app.camera.tool.TimerTaskBase;
import com.hongyue.app.camera.widget.AutoFitFrameLayout;

import java.io.File;

/**
 *  Description:  相机UI
 *  Author: Charlie
 *  Data: 2019/3/20  12:45
 *  Declare: None
 */

public class CameraFragment extends Fragment implements ICameraFragment {

    public static final String ARG_CONFIGURATION = "ARG_CONFIGURATION";
    public static final int MIN_VERSION_ICECREAM = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1;

    private AutoFitFrameLayout mPreviewContainer;

    private SensorManager mSensorManager;
    private CameraLifeCycle mCameraLifecycle;

    private CameraConfig mCameraConfig;
    private CameraConfigProvider mCameraConfigProvider;

    @Flash.FlashMode
    private int mFlashMode = Flash.FLASH_AUTO;
    @Camera.CameraType
    private int mCameraType = Camera.CAMERA_TYPE_REAR;
    @MediaAction.MediaActionState
    private int mMediaActionState = MediaAction.ACTION_PHOTO;
    @Record.RecordState
    private int mRecordState = Record.TAKE_PHOTO_STATE;

    private String mMediaFilePath;
    private FileObserver mFileObserver;
    private long mMaxVideoFileSize = 0;
    private TimerTaskBase mCountDownTimer;

    private CameraVideoRecordTextListener mCameraVideoRecordTextListener;

    private final TimerTaskBase.Callback mTimerCallBack = new TimerTaskBase.Callback() {
        @Override
        public void setText(String text) {
            if (mCameraVideoRecordTextListener != null) {
                mCameraVideoRecordTextListener.setRecordDurationText(text);
            }
        }

        @Override
        public void setTextVisible(boolean visible) {
            if (mCameraVideoRecordTextListener != null) {
                mCameraVideoRecordTextListener.setRecordDurationTextVisible(visible);
            }
        }
    };

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            synchronized (this) {
                if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    if (sensorEvent.values[0] < 4 && sensorEvent.values[0] > -4) {
                        if (sensorEvent.values[1] > 0) {
                            // UP
                            mCameraConfigProvider.setSensorPosition(CameraConfig.SENSOR_POSITION_UP);
                            mCameraConfigProvider.setDegrees(mCameraConfigProvider.getDeviceDefaultOrientation() == CameraConfig.ORIENTATION_PORTRAIT ? 0 : 90);
                        } else if (sensorEvent.values[1] < 0) {
                            // UP SIDE DOWN
                            mCameraConfigProvider.setSensorPosition(CameraConfig.SENSOR_POSITION_UP_SIDE_DOWN);
                            mCameraConfigProvider.setDegrees(mCameraConfigProvider.getDeviceDefaultOrientation() == CameraConfig.ORIENTATION_PORTRAIT ? 180 : 270);
                        }
                    } else if (sensorEvent.values[1] < 4 && sensorEvent.values[1] > -4) {
                        if (sensorEvent.values[0] > 0) {
                            // LEFT
                            mCameraConfigProvider.setSensorPosition(CameraConfig.SENSOR_POSITION_LEFT);
                            mCameraConfigProvider.setDegrees(mCameraConfigProvider.getDeviceDefaultOrientation() == CameraConfig.ORIENTATION_PORTRAIT ? 90 : 180);
                        } else if (sensorEvent.values[0] < 0) {
                            // RIGHT
                            mCameraConfigProvider.setSensorPosition(CameraConfig.SENSOR_POSITION_RIGHT);
                            mCameraConfigProvider.setDegrees(mCameraConfigProvider.getDeviceDefaultOrientation() == CameraConfig.ORIENTATION_PORTRAIT ? 270 : 0);
                        }
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    protected static CameraFragment newInstance(CameraConfig cameraConfig) {
        CameraFragment fragment = new CameraFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CONFIGURATION, cameraConfig);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.phoenix_fragment_camera, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle arguments = getArguments();
        if (arguments != null) {
            mCameraConfig = (CameraConfig) arguments.getSerializable(ARG_CONFIGURATION);
        }
        this.mCameraConfigProvider = new CameraConfigProviderImpl();
        this.mCameraConfigProvider.setCameraConfig(mCameraConfig);

        this.mSensorManager = (SensorManager) getContext().getSystemService(Activity.SENSOR_SERVICE);

        final CameraView cameraView = new CameraView() {

            @Override
            public void updateCameraPreview(Size size, View cameraPreview) {
                setCameraPreview(cameraPreview, size);
            }

            @Override
            public void updateUiForMediaAction(@CameraConfig.MediaAction int mediaAction) {

            }

            @Override
            public void updateCameraSwitcher(int numberOfCameras) {
            }

            @Override
            public void onPictureTaken(byte[] bytes, OnCameraResultListener callback) {
                final String filePath = mCameraLifecycle.getOutputFile().toString();
                if (callback != null) {
                    callback.onPhotoTaken(bytes, filePath);
                }
            }

            @Override
            public void onVideoRecordStart(int width, int height) {
                final File outputFile = mCameraLifecycle.getOutputFile();
                onStartVideoRecord(outputFile);
            }

            @Override
            public void onVideoRecordStop(@Nullable OnCameraResultListener callback) {
                //CameraFragment.this.onStopVideoRecord(callback);
            }

            @Override
            public void releaseCameraPreview() {
                clearCameraPreview();
            }
        };

        if (CameraUtils.hasCamera2(getContext())) {
            mCameraLifecycle = new Camera2LifeCycle(getContext(), cameraView, mCameraConfigProvider);
        } else {
            mCameraLifecycle = new Camera1LifeCycle(getContext(), cameraView, mCameraConfigProvider);
        }
        mCameraLifecycle.onCreate(savedInstanceState);

        //onProcessBundle
        mMediaActionState = mCameraConfigProvider.getMediaAction() == CameraConfig.MEDIA_ACTION_VIDEO ?
                MediaAction.ACTION_VIDEO : MediaAction.ACTION_PHOTO;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPreviewContainer = (AutoFitFrameLayout) view.findViewById(R.id.previewContainer);

        final int defaultOrientation = DeviceUtils.getDeviceDefaultOrientation(getContext());
        switch (defaultOrientation) {
            case android.content.res.Configuration.ORIENTATION_LANDSCAPE:
                mCameraConfigProvider.setDeviceDefaultOrientation(CameraConfig.ORIENTATION_LANDSCAPE);
                break;
            default:
                mCameraConfigProvider.setDeviceDefaultOrientation(CameraConfig.ORIENTATION_PORTRAIT);
                break;
        }

        switch (mCameraConfigProvider.getFlashMode()) {
            case CameraConfig.FLASH_MODE_AUTO:
                setFlashMode(Flash.FLASH_AUTO);
                break;
            case CameraConfig.FLASH_MODE_ON:
                setFlashMode(Flash.FLASH_ON);
                break;
            case CameraConfig.FLASH_MODE_OFF:
                setFlashMode(Flash.FLASH_OFF);
                break;
        }

        setCameraTypeFrontBack(mCameraConfigProvider.getCameraFace());
        notifyListeners();
    }

    public void notifyListeners() {
        onFlashModeChanged();
    }



    @Override
    public void onResume() {
        super.onResume();

        mCameraLifecycle.onResume();
        mSensorManager.registerListener(mSensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();

        mCameraLifecycle.onPause();
        mSensorManager.unregisterListener(mSensorEventListener);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mCameraLifecycle.onDestroy();
    }

    @Override
    public void takePicture(@Nullable String directoryPath, @Nullable String fileName, OnCameraResultListener callback) {
        if (Build.VERSION.SDK_INT > MIN_VERSION_ICECREAM) {
            new MediaActionSound().play(MediaActionSound.SHUTTER_CLICK);
        }
        setRecordState(Record.TAKE_PHOTO_STATE);
        this.mCameraLifecycle.takePhoto(callback, directoryPath, fileName);
    }

    @Override
    public void startRecordingVideo(@Nullable String directoryPath, @Nullable String fileName) {
        setRecordState(Record.RECORD_IN_PROGRESS_STATE);
        this.mCameraLifecycle.startVideoRecord(directoryPath, fileName);
    }

    @Override
    public void stopRecordingVideo(OnCameraResultListener callback) {
        setRecordState(Record.READY_FOR_RECORD_STATE);
        this.mCameraLifecycle.stopVideoRecord(callback);

        this.onStopVideoRecord(callback);

    }

    protected void setCameraTypeFrontBack(@CameraConfig.CameraFace int cameraFace) {
        switch (cameraFace) {
            case CameraConfig.CAMERA_FACE_FRONT:
                mCameraType = Camera.CAMERA_TYPE_FRONT;
                cameraFace = CameraConfig.CAMERA_FACE_FRONT;
                break;
            case CameraConfig.CAMERA_FACE_REAR:
                mCameraType = Camera.CAMERA_TYPE_REAR;
                cameraFace = CameraConfig.CAMERA_FACE_REAR;
                break;
        }
        this.mCameraLifecycle.switchCamera(cameraFace);

    }

    @Override
    public void switchCaptureAction(int actionType) {
        mMediaActionState = actionType;
    }


    private void onFlashModeChanged() {
        switch (mFlashMode) {
            case Flash.FLASH_AUTO:
                mCameraConfigProvider.setFlashMode(CameraConfig.FLASH_MODE_AUTO);
                this.mCameraLifecycle.setFlashMode(CameraConfig.FLASH_MODE_AUTO);
                break;
            case Flash.FLASH_ON:
                mCameraConfigProvider.setFlashMode(CameraConfig.FLASH_MODE_ON);
                this.mCameraLifecycle.setFlashMode(CameraConfig.FLASH_MODE_ON);
                break;
            case Flash.FLASH_OFF:
                mCameraConfigProvider.setFlashMode(CameraConfig.FLASH_MODE_OFF);
                this.mCameraLifecycle.setFlashMode(CameraConfig.FLASH_MODE_OFF);
                break;
        }
    }

    protected void setRecordState(@Record.RecordState int recordState) {
        this.mRecordState = recordState;
    }

    protected void setFlashMode(@Flash.FlashMode int mode) {
        this.mFlashMode = mode;
        onFlashModeChanged();
    }

    protected void clearCameraPreview() {
        if (mPreviewContainer != null)
            mPreviewContainer.removeAllViews();
    }

    protected void setCameraPreview(View preview, Size previewSize) {
        if (mPreviewContainer == null || preview == null) return;
        mPreviewContainer.removeAllViews();
        mPreviewContainer.addView(preview);

        mPreviewContainer.setAspectRatio(previewSize.getHeight() / (double) previewSize.getWidth());

    }

    protected void setMediaFilePath(final File mediaFile) {
        this.mMediaFilePath = mediaFile.toString();
    }

    protected void onStartVideoRecord(final File mediaFile) {
        setMediaFilePath(mediaFile);
        if (mMaxVideoFileSize > 0) {

            if (mCameraVideoRecordTextListener != null) {
                mCameraVideoRecordTextListener.setRecordSizeText(mMaxVideoFileSize, "1Mb" + " / " + mMaxVideoFileSize / (1024 * 1024) + "Mb");
                mCameraVideoRecordTextListener.setRecordSizeTextVisible(true);
            }
            try {
                mFileObserver = new FileObserver(this.mMediaFilePath) {
                    private long lastUpdateSize = 0;

                    @Override
                    public void onEvent(int event, String path) {
                        final long fileSize = mediaFile.length() / (1024 * 1024);
                        if ((fileSize - lastUpdateSize) >= 1) {
                            lastUpdateSize = fileSize;
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mCameraVideoRecordTextListener != null) {
                                        mCameraVideoRecordTextListener.setRecordSizeText(mMaxVideoFileSize, fileSize + "Mb" + " / " + mMaxVideoFileSize / (1024 * 1024) + "Mb");
                                    }
                                }
                            });
                        }
                    }
                };
                mFileObserver.startWatching();
            } catch (Exception e) {
                Log.e("FileObserver", "setMediaFilePath: ", e);
            }
        }

        if (mCountDownTimer == null) {
            this.mCountDownTimer = new TimerTask(mTimerCallBack);
        }
        mCountDownTimer.start();

    }

    protected void onStopVideoRecord(@Nullable OnCameraResultListener callback) {

        setRecordState(Record.READY_FOR_RECORD_STATE);

        if (mFileObserver != null)
            mFileObserver.stopWatching();

        if (mCountDownTimer != null) {
            mCountDownTimer.stop();
        }

        final String filePath = this.mCameraLifecycle.getOutputFile().toString();
        if (callback != null) {
            callback.onVideoRecorded(filePath);
        }
    }


    @Override
    public void setTextListener(CameraVideoRecordTextListener cameraVideoRecordTextListener) {
        this.mCameraVideoRecordTextListener = cameraVideoRecordTextListener;
    }

}
