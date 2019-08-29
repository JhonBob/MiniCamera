package com.hongyue.app.camera.manager.listener;


import com.hongyue.app.camera.listener.OnCameraResultListener;

import java.io.File;

public interface CameraPictureListener {

    void onPictureTaken(byte[] bytes, File photoFile, OnCameraResultListener callback);

    void onPictureTakeError();
}
