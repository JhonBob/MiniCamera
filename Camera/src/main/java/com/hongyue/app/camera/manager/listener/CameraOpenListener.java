package com.hongyue.app.camera.manager.listener;


import com.hongyue.app.camera.tool.Size;

public interface CameraOpenListener<CameraId, SurfaceListener> {

    void onCameraOpened(CameraId openedCameraId, Size previewSize, SurfaceListener surfaceListener);

    void onCameraOpenError();
}
