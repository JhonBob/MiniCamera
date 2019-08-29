package com.hongyue.app.camera.manager;


import android.content.Context;

import com.hongyue.app.camera.config.CameraConfig;
import com.hongyue.app.camera.config.CameraConfigProvider;
import com.hongyue.app.camera.listener.OnCameraResultListener;
import com.hongyue.app.camera.manager.listener.CameraCloseListener;
import com.hongyue.app.camera.manager.listener.CameraOpenListener;
import com.hongyue.app.camera.manager.listener.CameraPictureListener;
import com.hongyue.app.camera.manager.listener.CameraVideoListener;

import java.io.File;

/**
 *  Description: 相机管理外观
 *  Author: Charlie
 *  Data: 2019/3/15  10:25
 *  Declare: None
 */

public interface CameraManager<CameraId, SurfaceListener>{

    void initializeCameraManager(CameraConfigProvider cameraConfigProvider, Context context);

    void releaseCameraManager();

    void openCamera(CameraId cameraId, CameraOpenListener<CameraId, SurfaceListener> cameraOpenListener);

    void closeCamera(CameraCloseListener<CameraId> cameraCloseListener);

    void takePicture(File photoFile, CameraPictureListener cameraPictureListener, OnCameraResultListener callback);

    void startVideoRecord(File videoFile, CameraVideoListener cameraVideoListener);

    void stopVideoRecord(OnCameraResultListener callback);

    boolean isVideoRecording();

    void setCameraId(CameraId cameraId);

    void setFlashMode(@CameraConfig.FlashMode int flashMode);

    CameraId getCameraId();

    CameraId getFaceFrontCameraId();

    CameraId getFaceBackCameraId();

    int getNumberOfCameras();



}
