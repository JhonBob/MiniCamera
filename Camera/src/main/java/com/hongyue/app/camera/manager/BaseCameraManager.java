package com.hongyue.app.camera.manager;

import android.content.Context;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import com.hongyue.app.camera.config.CameraConfig;
import com.hongyue.app.camera.config.CameraConfigProvider;
import com.hongyue.app.camera.tool.Size;

/**
 *  Description:  相机统一管理封装
 *  Author: Charlie
 *  Data: 2019/3/15  10:35
 *  Declare: None
 */

abstract class BaseCameraManager<CameraId, SurfaceListener> implements CameraManager<CameraId, SurfaceListener>, MediaRecorder.OnInfoListener {

    private static final String TAG = "BaseCameraManager";

    protected Context mContext;
    protected CameraConfigProvider mCameraConfigProvider;
    protected MediaRecorder mMediaRecorder; //系统多媒体录制

    boolean mIsVideoRecording = false;

    CameraId mCameraId = null;
    CameraId mFaceFrontCameraId = null;
    CameraId mFaceBackCameraId = null;
    int mNumberOfCameras = 0;
    int mFaceFrontCameraOrientation;
    int mFaceBackCameraOrientation;

    Size mPhotoSize;
    Size mVideoSize;
    Size mPreviewSize;
    Size mWindowSize;
    CamcorderProfile mCamcorderProfile; //相机视频输出配置

    HandlerThread mBackgroundThread; //后台线程
    Handler mBackgroundHandler; //处理后台线程消息
    Handler mUiiHandler = new Handler(Looper.getMainLooper()); //处理UI线程消息

    @Override
    public void initializeCameraManager(CameraConfigProvider cameraConfigProvider, Context context) {
        this.mCameraConfigProvider = cameraConfigProvider;
        this.mContext = context;
        startBackgroundThread(); //HandlerThread 待命
    }

    @Override
    public void releaseCameraManager() {
        this.mContext = null;
        stopBackgroundThread();
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }


    private void stopBackgroundThread() {
        if (Build.VERSION.SDK_INT > 17) {
            mBackgroundThread.quitSafely();
        } else mBackgroundThread.quit();

        try {
            mBackgroundThread.join(); //主线程等待后台线程运行结束
            //a.并非子线程主动采取了措施去唤醒父线程。父线程重新运行，都是由底层的调度引起的
            //b.在调用 join() 方法的程序中，原来的多个线程仍然多个线程，并没有发生“合并为一个单线程”。
            // 真正发生的是调用 join() 的线程进入 TIMED_WAITING 状态，等待 join() 所属线程运行结束后再继续运行
        } catch (InterruptedException e) {
            Log.e(TAG, "stopBackgroundThread: ", e);
        } finally {
            mBackgroundThread = null;
            mBackgroundHandler = null;
        }
    }


    protected abstract void prepareCameraOutputs();

    protected abstract boolean prepareVideoRecorder();

    protected abstract void onMaxDurationReached();

    protected abstract void onMaxFileSizeReached();

    protected abstract int getPhotoOrientation(@CameraConfig.SensorPosition int sensorPosition);

    protected abstract int getVideoOrientation(@CameraConfig.SensorPosition int sensorPosition);


    protected void releaseVideoRecorder() {
        try {
            if (mMediaRecorder != null) {
                mMediaRecorder.reset();
                mMediaRecorder.release();
            }
        } catch (Exception ignore) {

        } finally {
            mMediaRecorder = null;
        }
    }


    @Override
    public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
        if (MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED == what) {
            onMaxDurationReached();
        } else if (MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED == what) {
            onMaxFileSizeReached();
        }
    }

    public boolean isVideoRecording() {
        return mIsVideoRecording;
    }

    public CameraId getCameraId() {
        return mCameraId;
    }

    public CameraId getFaceFrontCameraId() {
        return mFaceFrontCameraId;
    }

    public CameraId getFaceBackCameraId() {
        return mFaceBackCameraId;
    }

    public int getNumberOfCameras() {
        return mNumberOfCameras;
    }

    public void setCameraId(CameraId currentCameraId) {
        this.mCameraId = currentCameraId;
    }





}
