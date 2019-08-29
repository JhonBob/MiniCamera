package com.hongyue.app.camera.manager;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.hongyue.app.camera.config.CameraConfig;
import com.hongyue.app.camera.config.CameraConfigProvider;
import com.hongyue.app.camera.listener.OnCameraResultListener;
import com.hongyue.app.camera.manager.listener.CameraCloseListener;
import com.hongyue.app.camera.manager.listener.CameraOpenListener;
import com.hongyue.app.camera.manager.listener.CameraPictureListener;
import com.hongyue.app.camera.manager.listener.CameraVideoListener;
import com.hongyue.app.camera.tool.CameraUtils;
import com.hongyue.app.camera.tool.Size;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 *  Description:  相机管理 ( Android API1 )
 *  Author: Charlie
 *  Data: 2019/3/15  10:29
 *  Declare: None
 */

public class Camera1Manager extends BaseCameraManager<Integer, SurfaceHolder.Callback>{

    private static final String TAG = "Camera1Manager";

    private Camera camera;
    private Surface surface;

    private int orientation;
    private int displayRotation = 0;

    private File outputPath;

    private CameraVideoListener videoListener;
    private CameraPictureListener photoListener;

    private Integer futurFlashMode;


    @Override
    public void initializeCameraManager(CameraConfigProvider cameraConfigProvider, Context context) {
        super.initializeCameraManager(cameraConfigProvider, context);

        mNumberOfCameras = Camera.getNumberOfCameras();

        for (int i = 0; i < mNumberOfCameras; i++){
            final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mFaceBackCameraId = i;
                mFaceBackCameraOrientation = cameraInfo.orientation;
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mFaceFrontCameraId = i;
                mFaceFrontCameraOrientation = cameraInfo.orientation;
            }
        }
    }

    @Override
    public void releaseCameraManager() {
        super.releaseCameraManager();
    }

    @Override
    public void openCamera(final Integer cameraId, final CameraOpenListener<Integer, SurfaceHolder.Callback> cameraOpenListener) {
        this.mCameraId = cameraId;
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {

                    camera = Camera.open(cameraId); //打开相机
                    prepareCameraOutputs(); //准备相机
                    if (futurFlashMode != null) {
                        setFlashMode(futurFlashMode); //设置闪光灯模式
                        futurFlashMode = null;
                    }
                    if (cameraOpenListener != null){
                        mUiiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                cameraOpenListener.onCameraOpened(cameraId, mPreviewSize, new SurfaceHolder.Callback() {
                                    @Override
                                    public void surfaceCreated(SurfaceHolder surfaceHolder) {

                                        if (surfaceHolder.getSurface() == null) {
                                            return;
                                        }

                                        surface = surfaceHolder.getSurface(); //取一块Surface用于预览

                                        try {
                                            camera.stopPreview();
                                        } catch (Exception e){
                                            e.printStackTrace();
                                        }

                                        startPreView(surfaceHolder); //开始预览


                                    }

                                    @Override
                                    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {

                                        if (surfaceHolder.getSurface() == null) {
                                            return;
                                        }

                                        surface = surfaceHolder.getSurface();

                                        try {
                                            camera.stopPreview();
                                        } catch (Exception ignore) {
                                        }

                                        startPreView(surfaceHolder);
                                    }

                                    @Override
                                    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

                                    }
                                });
                            }
                        });
                    }

                }catch (Exception e) {

                    if (cameraOpenListener != null) {
                        mUiiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                cameraOpenListener.onCameraOpenError();
                            }
                        });
                    }

                }
            }
        });
    }

    @Override
    protected void prepareCameraOutputs() {

        //为了保证图片不失真，要保证预览Surfaceview、Previewsize、Picturesize的长宽比率要一致

        try {

            if (mCameraConfigProvider.getMediaQuality() == CameraConfig.MEDIA_QUALITY_AUTO) { //多媒体质量为自动模式
                mCamcorderProfile = CameraUtils.getCamcorderProfile(mCameraId, mCameraConfigProvider.getVideoFileSize(), mCameraConfigProvider.getMinimumVideoDuration());
            } else {
                mCamcorderProfile = CameraUtils.getCamcorderProfile(mCameraConfigProvider.getMediaQuality(), mCameraId);
            }

            final List<Size> previewSizes = Size.fromList(camera.getParameters().getSupportedPreviewSizes());
            final List<Size> pictureSizes = Size.fromList(camera.getParameters().getSupportedPictureSizes());

            List<Size> videoSizes;
            if (Build.VERSION.SDK_INT > 10)
                videoSizes = Size.fromList(camera.getParameters().getSupportedVideoSizes());
            else {
                videoSizes = previewSizes;
            }

            //获取最佳尺寸
            mVideoSize = CameraUtils.getSizeWithClosestRatio(
                    (videoSizes == null || videoSizes.isEmpty()) ? previewSizes : videoSizes,
                    mCamcorderProfile.videoFrameWidth, mCamcorderProfile.videoFrameHeight);

            mPhotoSize = CameraUtils.getPictureSize(
                    (pictureSizes == null || pictureSizes.isEmpty()) ? previewSizes : pictureSizes,
                    mCameraConfigProvider.getMediaQuality() == CameraConfig.MEDIA_QUALITY_AUTO
                            ? CameraConfig.MEDIA_QUALITY_HIGHEST : mCameraConfigProvider.getMediaQuality());

            if (mCameraConfigProvider.getMediaAction() == CameraConfig.MEDIA_ACTION_PHOTO
                    || mCameraConfigProvider.getMediaAction() == CameraConfig.MEDIA_ACTION_UNSPECIFIED) {
                mPreviewSize = CameraUtils.getSizeWithClosestRatio(previewSizes, mPhotoSize.getWidth(), mPhotoSize.getHeight());
            } else {
                mPreviewSize = CameraUtils.getSizeWithClosestRatio(previewSizes, mVideoSize.getWidth(), mVideoSize.getHeight());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while setup camera sizes.");
        }
    }


    private void startPreView(SurfaceHolder surfaceHolder){

        try {

            final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraId, cameraInfo);
            //相机相对空间y轴正方向的图像采集位置，固定的，不随手机旋转而变化 后置摄像头为90度， 前置摄像头为270度， 影响拍照的返回图片的方向
            int cameraRotationOffset = cameraInfo.orientation;

            final Camera.Parameters parameters = camera.getParameters();
            setAutoFocus(camera, parameters); //自动聚焦
            setFlashMode(mCameraConfigProvider.getFlashMode()); //闪光模式

            if (mCameraConfigProvider.getMediaAction() == CameraConfig.MEDIA_ACTION_PHOTO
                    || mCameraConfigProvider.getMediaAction() == CameraConfig.MEDIA_ACTION_UNSPECIFIED){
                turnPhotoCameraFeaturesOn(camera, parameters);
            } else if (mCameraConfigProvider.getMediaAction() == CameraConfig.MEDIA_ACTION_VIDEO){
                turnVideoCameraFeaturesOn(camera, parameters);
            }


            //手机预览方向

            final int rotation = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();

            int degrees = 0;

            switch (rotation){
                case Surface.ROTATION_0: //手机竖屏
                    degrees = 0;
                    break;
                case Surface.ROTATION_90://逆时针旋转90度放置，左横屏
                    degrees = 90;
                    break;
                case Surface.ROTATION_180://倒立
                    degrees = 180;
                    break;
                case Surface.ROTATION_270://右横屏
                    degrees = 270;
                    break;
            }

            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){ //前置摄像头

                displayRotation = (cameraRotationOffset + degrees) % 360;
                displayRotation = (360 - displayRotation) % 360; // 补偿计算，例如：顺时针旋转270度 == 逆时针旋转90度

            } else { //后置摄像头

                displayRotation = (cameraRotationOffset - degrees + 360) % 360;

            }

            this.camera.setDisplayOrientation(displayRotation); //设置摄像头采集的数据需要旋转的角度


            if (Build.VERSION.SDK_INT > 13
                    && (mCameraConfigProvider.getMediaAction() == CameraConfig.MEDIA_ACTION_VIDEO
                    || mCameraConfigProvider.getMediaAction() == CameraConfig.MEDIA_ACTION_UNSPECIFIED)) {
//                parameters.setRecordingHint(true);
            }

            if (Build.VERSION.SDK_INT > 14
                    && parameters.isVideoStabilizationSupported()
                    && (mCameraConfigProvider.getMediaAction() == CameraConfig.MEDIA_ACTION_VIDEO
                    || mCameraConfigProvider.getMediaAction() == CameraConfig.MEDIA_ACTION_UNSPECIFIED)) {
                parameters.setVideoStabilization(true);
            }

            parameters.setPreviewSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            parameters.setPictureSize(mPhotoSize.getWidth(), mPhotoSize.getHeight());

            camera.setParameters(parameters);
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();


        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void setAutoFocus(Camera camera, Camera.Parameters parameters) {
        try {
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                camera.setParameters(parameters);
            }
        } catch (Exception ignore) {
        }
    }

    private void turnPhotoCameraFeaturesOn(Camera camera, Camera.Parameters parameters) {
        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        camera.setParameters(parameters);
    }

    private void turnVideoCameraFeaturesOn(Camera camera, Camera.Parameters parameters) {
        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        camera.setParameters(parameters);
    }

    @Override
    public void closeCamera(final CameraCloseListener<Integer> cameraCloseListener) {

        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                if (camera != null) {
                    camera.release();
                    camera = null;
                    if (cameraCloseListener != null) {
                        mUiiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                cameraCloseListener.onCameraClosed(mCameraId);
                            }
                        });
                    }
                }
            }
        });

    }

    @Override
    public void setFlashMode(int flashMode) {

        if (camera != null){
            setFlashMode(camera, camera.getParameters(), flashMode);
        } else {
            futurFlashMode = flashMode;
        }

    }


    private void setFlashMode(Camera camera, Camera.Parameters parameters, @CameraConfig.FlashMode int flashMode) {
        try {
            switch (flashMode) {
                case CameraConfig.FLASH_MODE_AUTO:
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                    break;
                case CameraConfig.FLASH_MODE_ON:
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                    break;
                case CameraConfig.FLASH_MODE_OFF:
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    break;
                default:
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                    break;
            }
            camera.setParameters(parameters);
        } catch (Exception ignore) {
        }
    }

    @Override
    public void takePicture(File photoFile, CameraPictureListener cameraPictureListener, final OnCameraResultListener callback) {
        this.outputPath = photoFile;
        this.photoListener = cameraPictureListener;
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                setCameraPhotoQuality(camera);
                camera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        Camera1Manager.this.onPictureTaken(data, camera, callback);
                    }
                });
            }
        });
    }

    private void setCameraPhotoQuality(Camera camera) {
        final Camera.Parameters parameters = camera.getParameters();

        parameters.setPictureFormat(PixelFormat.JPEG);

        if (mCameraConfigProvider.getMediaQuality() == CameraConfig.MEDIA_QUALITY_LOW) {
            parameters.setJpegQuality(50);
        } else if (mCameraConfigProvider.getMediaQuality() == CameraConfig.MEDIA_QUALITY_MEDIUM) {
            parameters.setJpegQuality(75);
        } else if (mCameraConfigProvider.getMediaQuality() == CameraConfig.MEDIA_QUALITY_HIGH) {
            parameters.setJpegQuality(100);
        } else if (mCameraConfigProvider.getMediaQuality() == CameraConfig.MEDIA_QUALITY_HIGHEST) {
            parameters.setJpegQuality(100);
        }
        parameters.setPictureSize(mPhotoSize.getWidth(), mPhotoSize.getHeight());

        camera.setParameters(parameters);
    }


    private void onPictureTaken(final byte[] bytes, Camera camera, final OnCameraResultListener callback) {
        final File pictureFile = outputPath;
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions.");
            return;
        }

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(pictureFile); //写文件
            fileOutputStream.write(bytes);
            fileOutputStream.close();
        } catch (FileNotFoundException error) {
            Log.e(TAG, "File not found: " + error.getMessage());
        } catch (IOException error) {
            Log.e(TAG, "Error accessing file: " + error.getMessage());
        } catch (Throwable error) {
            Log.e(TAG, "Error saving file: " + error.getMessage());
        }

        try {
            //ExifInterface 修正图片方向
            final ExifInterface exif = new ExifInterface(pictureFile.getAbsolutePath());
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, "" + getPhotoOrientation(mCameraConfigProvider.getSensorPosition()));
            exif.saveAttributes();

            if (photoListener != null) {
                mUiiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        photoListener.onPictureTaken(bytes, outputPath, callback);
                    }
                });
            }
            camera.startPreview();
        } catch (Throwable error) {
            Log.e(TAG, "Can't save exif info: " + error.getMessage());
        }
    }


    @Override
    protected int getPhotoOrientation(@CameraConfig.SensorPosition int sensorPosition) {

        final int rotate;

        if (mCameraId.equals(mFaceFrontCameraId)) {//前置摄像头
            rotate = (360 + mFaceFrontCameraOrientation + mCameraConfigProvider.getDegrees()) % 360; //顺势针旋转
        } else {//后置摄像头
            rotate = (360 + mFaceBackCameraOrientation - mCameraConfigProvider.getDegrees()) % 360; //顺时针旋转
        }

        if (rotate == 0) {
            orientation = ExifInterface.ORIENTATION_NORMAL;
        } else if (rotate == 90) {
            orientation = ExifInterface.ORIENTATION_ROTATE_90;
        } else if (rotate == 180) {
            orientation = ExifInterface.ORIENTATION_ROTATE_180;
        } else if (rotate == 270) {
            orientation = ExifInterface.ORIENTATION_ROTATE_270;
        }

        return orientation;

    }

    @Override
    public void startVideoRecord(File videoFile, CameraVideoListener cameraVideoListener) {

        if (mIsVideoRecording) return;

        this.outputPath = videoFile;
        this.videoListener = cameraVideoListener;

        if (videoListener != null){
            mBackgroundHandler.post(new Runnable() {
                @Override
                public void run() {

                    if (mContext == null) return;

                    if (prepareVideoRecorder()){
                        mMediaRecorder.start();
                        mIsVideoRecording = true;
                        mUiiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                videoListener.onVideoRecordStarted(mVideoSize);
                            }
                        });
                    }

                }
            });
        }

    }


    @Override
    protected boolean prepareVideoRecorder() {
        mMediaRecorder = new MediaRecorder();
        try {
            camera.lock();
            camera.unlock();
            mMediaRecorder.setCamera(camera);

            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

            mMediaRecorder.setOutputFormat(mCamcorderProfile.fileFormat);
            mMediaRecorder.setVideoFrameRate(mCamcorderProfile.videoFrameRate);
            mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
            mMediaRecorder.setVideoEncodingBitRate(mCamcorderProfile.videoBitRate);
            mMediaRecorder.setVideoEncoder(mCamcorderProfile.videoCodec);

            mMediaRecorder.setAudioEncodingBitRate(mCamcorderProfile.audioBitRate);
            mMediaRecorder.setAudioChannels(mCamcorderProfile.audioChannels);
            mMediaRecorder.setAudioSamplingRate(mCamcorderProfile.audioSampleRate);
            mMediaRecorder.setAudioEncoder(mCamcorderProfile.audioCodec);

            mMediaRecorder.setOutputFile(outputPath.toString());

            if (mCameraConfigProvider.getVideoFileSize() > 0) {
                mMediaRecorder.setMaxFileSize(mCameraConfigProvider.getVideoFileSize());

                mMediaRecorder.setOnInfoListener(this);
            }
            if (mCameraConfigProvider.getVideoDuration() > 0) {
                mMediaRecorder.setMaxDuration(mCameraConfigProvider.getVideoDuration());

                mMediaRecorder.setOnInfoListener(this);
            }

            mMediaRecorder.setOrientationHint(getVideoOrientation(mCameraConfigProvider.getSensorPosition()));
            mMediaRecorder.setPreviewDisplay(surface);

            mMediaRecorder.prepare();

            return true;
        } catch (IllegalStateException error) {
            Log.e(TAG, "IllegalStateException preparing MediaRecorder: " + error.getMessage());
        } catch (IOException error) {
            Log.e(TAG, "IOException preparing MediaRecorder: " + error.getMessage());
        } catch (Throwable error) {
            Log.e(TAG, "Error during preparing MediaRecorder: " + error.getMessage());
        }

        releaseVideoRecorder();
        return false;
    }


    @Override
    protected int getVideoOrientation(@CameraConfig.SensorPosition int sensorPosition) {
        int degrees = 0;
        switch (sensorPosition) {
            case CameraConfig.SENSOR_POSITION_UP:
                degrees = 0;
                break; // Natural orientation
            case CameraConfig.SENSOR_POSITION_LEFT:
                degrees = 90;
                break; // Landscape left
            case CameraConfig.SENSOR_POSITION_UP_SIDE_DOWN:
                degrees = 180;
                break;// Upside down
            case CameraConfig.SENSOR_POSITION_RIGHT:
                degrees = 270;
                break;// Landscape right
        }

        final int rotate;
        if (mCameraId.equals(mFaceFrontCameraId)) {
            rotate = (360 + mFaceFrontCameraOrientation + degrees) % 360;
        } else {
            rotate = (360 + mFaceBackCameraOrientation - degrees) % 360;
        }
        return rotate;
    }


    protected void releaseVideoRecorder() {
        super.releaseVideoRecorder();

        try {
            // lock camera for later use
            camera.lock();
        } catch (Exception ignore) {
        }
    }


    @Override
    public void stopVideoRecord(final OnCameraResultListener callback) {

        if (mIsVideoRecording){
            mBackgroundHandler.post(new Runnable() {
                @Override
                public void run() {

                    try {
                        if (mMediaRecorder != null) mMediaRecorder.stop();
                    } catch (Exception e){
                        //文件大小超过限制或，已经调用停止
                        e.printStackTrace();
                    }

                    mIsVideoRecording = false;

                    releaseVideoRecorder();

                    if (videoListener != null){
                        mUiiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                videoListener.onVideoRecordStopped(outputPath, callback);
                            }
                        });
                    }
                }
            });
        }

    }

    @Override
    protected void onMaxDurationReached() {
        stopVideoRecord(null);
    }

    @Override
    protected void onMaxFileSizeReached() {
        stopVideoRecord(null);
    }

}
