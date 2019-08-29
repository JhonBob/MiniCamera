package com.hongyue.app.camera.lifecycle;

import android.os.Bundle;
import androidx.annotation.Nullable;

import com.hongyue.app.camera.config.CameraConfig;
import com.hongyue.app.camera.listener.OnCameraResultListener;
import com.hongyue.app.camera.manager.CameraManager;

import java.io.File;

/**
 *  Description:  相机生命周期外观
 *  Author: Charlie
 *  Data: 2019/3/15  10:20
 *  Declare: None
 */

public interface CameraLifeCycle<CameraId> {


    void onCreate(Bundle savedInstanceState);

    void onResume();

    void onPause();

    void onDestroy();


    void takePhoto(OnCameraResultListener callback, @Nullable String direcoryPath, @Nullable String fileName);

    void startVideoRecord();

    void startVideoRecord(@Nullable String direcoryPath, @Nullable String fileName);

    void stopVideoRecord(OnCameraResultListener callback);

    boolean isVideoRecording();

    void switchCamera(@CameraConfig.CameraFace int cameraFace);

    void switchQuality();

    void setFlashMode(@CameraConfig.FlashMode int flashMode);

    int getNumberOfCameras();

    @CameraConfig.MediaAction
    int getMediaAction();

    CameraId getCameraId();

    File getOutputFile();

    CameraManager getCameraManager();


}
