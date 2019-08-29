package com.hongyue.app.camera.manager.listener;


import com.hongyue.app.camera.listener.OnCameraResultListener;
import com.hongyue.app.camera.tool.Size;

import java.io.File;

public interface CameraVideoListener {

    void onVideoRecordStarted(Size videoSize);

    void onVideoRecordStopped(File videoFile, OnCameraResultListener callback);

    void onVideoRecordError();
}
