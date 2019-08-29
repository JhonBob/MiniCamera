package com.hongyue.app.camera.lifecycle;

import androidx.annotation.Nullable;
import android.view.View;

import com.hongyue.app.camera.config.CameraConfig;
import com.hongyue.app.camera.listener.OnCameraResultListener;
import com.hongyue.app.camera.tool.Size;

/**
 *  Description:  相机回调
 *  Author: Charlie
 *  Data: 2019/3/20  12:43
 *  Declare: None
 */

public interface CameraView {

    void updateCameraPreview(Size size, View cameraPreview);

    void updateUiForMediaAction(@CameraConfig.MediaAction int mediaAction);

    void updateCameraSwitcher(int numberOfCameras);

    void onPictureTaken(byte[] bytes, @Nullable OnCameraResultListener callback);

    void onVideoRecordStart(int width, int height);

    void onVideoRecordStop(@Nullable OnCameraResultListener callback);

    void releaseCameraPreview();
}
