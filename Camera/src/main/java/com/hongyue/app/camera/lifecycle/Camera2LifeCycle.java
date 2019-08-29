package com.hongyue.app.camera.lifecycle;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.TextureView;

import com.hongyue.app.camera.config.CameraConfig;
import com.hongyue.app.camera.config.CameraConfigProvider;
import com.hongyue.app.camera.listener.OnCameraResultListener;
import com.hongyue.app.camera.manager.Camera2Manager;
import com.hongyue.app.camera.manager.CameraManager;
import com.hongyue.app.camera.manager.listener.CameraCloseListener;
import com.hongyue.app.camera.manager.listener.CameraOpenListener;
import com.hongyue.app.camera.manager.listener.CameraPictureListener;
import com.hongyue.app.camera.manager.listener.CameraVideoListener;
import com.hongyue.app.camera.tool.CameraUtils;
import com.hongyue.app.camera.tool.Size;
import com.hongyue.app.camera.widget.AutoFitTextureView;

import java.io.File;

/**
 *  Description:  相机生命周期管理
 *  Author: Charlie
 *  Data: 2019/3/20  12:42
 *  Declare: None
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2LifeCycle implements CameraLifeCycle<String>, CameraOpenListener<String, TextureView.SurfaceTextureListener>
        ,CameraCloseListener<String>, CameraPictureListener, CameraVideoListener {

    private final static String TAG = "Camera2LifeCycle";

    private final Context mContext;
    private String mCameraId;
    private File mOutputFile;
    private CameraView mCameraView;
    private CameraConfigProvider mCameraConfigProvider;
    private CameraManager<String, TextureView.SurfaceTextureListener> mCamera2Manager;

    public Camera2LifeCycle(Context context, CameraView cameraView, CameraConfigProvider cameraConfigProvider) {
        this.mContext = context;
        this.mCameraView = cameraView;
        this.mCameraConfigProvider = cameraConfigProvider;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mCamera2Manager = new Camera2Manager();
        mCamera2Manager.initializeCameraManager(mCameraConfigProvider, mContext);
        setCameraId(mCamera2Manager.getFaceBackCameraId());
    }

    @Override
    public void onResume() {
        mCamera2Manager.openCamera(mCameraId, this);
    }

    @Override
    public void onPause() {
        mCamera2Manager.closeCamera(null);
        mCameraView.releaseCameraPreview();
    }

    @Override
    public void onDestroy() {
        mCamera2Manager.releaseCameraManager();
    }


    @Override
    public void takePhoto(OnCameraResultListener callback, @Nullable String direcoryPath, @Nullable String fileName) {
        mOutputFile = CameraUtils.getOutputMediaFile(mContext, CameraConfig.MEDIA_ACTION_PHOTO, direcoryPath, fileName);
        mCamera2Manager.takePicture(mOutputFile, this, callback);
    }

    @Override
    public void startVideoRecord() {
        startVideoRecord(null, null);
    }

    @Override
    public void startVideoRecord(@Nullable String direcoryPath, @Nullable String fileName) {
        mOutputFile = CameraUtils.getOutputMediaFile(mContext, CameraConfig.MEDIA_ACTION_VIDEO, direcoryPath, fileName);
        mCamera2Manager.startVideoRecord(mOutputFile, this);
    }

    @Override
    public void stopVideoRecord(OnCameraResultListener callback) {
        mCamera2Manager.stopVideoRecord(callback);
    }

    @Override
    public boolean isVideoRecording() {
        return mCamera2Manager.isVideoRecording();
    }

    @Override
    public void switchCamera(final @CameraConfig.CameraFace int cameraFace) {
        final String cameraId = mCamera2Manager.getCameraId();
        final String faceFrontCameraId = mCamera2Manager.getFaceFrontCameraId();
        final String faceBackCameraId = mCamera2Manager.getFaceBackCameraId();

        if (cameraFace == CameraConfig.CAMERA_FACE_REAR && faceBackCameraId != null) {
            setCameraId(faceBackCameraId);
            mCamera2Manager.closeCamera(this);
        } else if (faceFrontCameraId != null) {
            setCameraId(faceFrontCameraId);
            mCamera2Manager.closeCamera(this);
        }

    }

    private void setCameraId(String currentCameraId) {
        this.mCameraId = currentCameraId;
        mCamera2Manager.setCameraId(currentCameraId);
    }

    @Override
    public void setFlashMode(@CameraConfig.FlashMode int flashMode) {
        mCamera2Manager.setFlashMode(flashMode);
    }

    @Override
    public void switchQuality() {
        mCamera2Manager.closeCamera(this);
    }

    @Override
    public int getNumberOfCameras() {
        return mCamera2Manager.getNumberOfCameras();
    }

    @Override
    public int getMediaAction() {
        return mCameraConfigProvider.getMediaAction();
    }

    @Override
    public File getOutputFile() {
        return mOutputFile;
    }

    @Override
    public String getCameraId() {
        return mCameraId;
    }

    @Override
    public void onCameraOpened(String openedCameraId, Size previewSize, TextureView.SurfaceTextureListener surfaceTextureListener) {
        mCameraView.updateUiForMediaAction(CameraConfig.MEDIA_ACTION_UNSPECIFIED);
        mCameraView.updateCameraPreview(previewSize, new AutoFitTextureView(mContext, surfaceTextureListener));
        mCameraView.updateCameraSwitcher(mCamera2Manager.getNumberOfCameras());
    }

    @Override
    public void onCameraOpenError() {
        Log.e(TAG, "onCameraOpenError");
    }

    @Override
    public void onCameraClosed(String closedCameraId) {
        mCameraView.releaseCameraPreview();

        mCamera2Manager.openCamera(mCameraId, this);
    }

    @Override
    public void onPictureTaken(byte[] bytes, File photoFile, OnCameraResultListener callback) {
        mCameraView.onPictureTaken(bytes, callback);
    }

    @Override
    public void onPictureTakeError() {
    }

    @Override
    public void onVideoRecordStarted(Size videoSize) {
        mCameraView.onVideoRecordStart(videoSize.getWidth(), videoSize.getHeight());
    }

    @Override
    public void onVideoRecordStopped(File videoFile, @Nullable OnCameraResultListener callback) {
        mCameraView.onVideoRecordStop(callback);
    }

    @Override
    public void onVideoRecordError() {

    }

    @Override
    public CameraManager getCameraManager() {
        return mCamera2Manager;
    }

}
