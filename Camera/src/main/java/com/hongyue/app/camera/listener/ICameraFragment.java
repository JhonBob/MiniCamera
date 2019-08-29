package com.hongyue.app.camera.listener;

import androidx.annotation.Nullable;

public interface ICameraFragment {

    void takePicture(@Nullable String directoryPath, @Nullable String fileName, OnCameraResultListener resultListener);

    void startRecordingVideo(@Nullable String directoryPath, @Nullable String fileName);

    void stopRecordingVideo(OnCameraResultListener callback);



    void switchCaptureAction(int actionType);



    void setTextListener(CameraVideoRecordTextListener cameraVideoRecordTextListener);

}
