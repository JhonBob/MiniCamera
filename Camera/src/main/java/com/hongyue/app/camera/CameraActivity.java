package com.hongyue.app.camera;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hongyue.app.camera.config.CameraConfig;
import com.hongyue.app.camera.config.MediaAction;
import com.hongyue.app.camera.listener.CameraVideoRecordTextAdapter;
import com.hongyue.app.camera.listener.ICameraFragment;
import com.hongyue.app.camera.listener.OnCameraResultAdapter;
import com.hongyue.app.camera.widget.RecordButton;

import java.util.ArrayList;
import java.util.List;

public class CameraActivity extends FragmentActivity implements View.OnClickListener {
    private static final String TAG_CAMERA_FRAGMENT = "CameraFragment";
    private static final String DIRECTORY_NAME = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()
            + "/Camera";

    View mCameraLayout;
    RecordButton mRecordButton;
    TextView mRecordDurationText;
    TextView mRecordSizeText;
    ImageView iv_close;
    TextView mRecordTip;

    private boolean enableRecord = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //透明状态栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            //透明导航栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置成全屏模式
        }


        setContentView(R.layout.activity_cameras);

        enableRecord = getIntent().getBooleanExtra("enableRecord", true);

        setupView();
        setupCameraFragment();
    }



    @Override
    public void onClick(View v) {
        final ICameraFragment cameraFragment = getCameraFragment();
        if (cameraFragment == null) {
            return;
        }
    }

    private void setupView() {
        mRecordButton = (RecordButton) findViewById(R.id.record_button);
        mRecordDurationText = (TextView) findViewById(R.id.record_duration_text);
        mRecordSizeText = (TextView) findViewById(R.id.record_size_mb_text);
        mRecordTip = findViewById(R.id.record_tip);
        iv_close = findViewById(R.id.iv_close);
        mCameraLayout = findViewById(R.id.rl_camera_control);
        mCameraLayout.setOnClickListener(this);
        iv_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        showTips(enableRecord ? "录制模式" : "拍照模式，不能录制视频");
        mRecordTip.setText(enableRecord ? "轻触拍照，长按拍摄" : "轻触拍照");
        mRecordButton.setTimeLimit(15 * 1000);
        mRecordButton.setRecordable(enableRecord);
        mRecordButton.setOnRecordButtonListener(new RecordButton.OnRecordButtonListener() {
            @Override
            public void onClick() {
                final ICameraFragment cameraFragment = getCameraFragment();
                cameraFragment.switchCaptureAction(MediaAction.ACTION_PHOTO);
                cameraFragment.takePicture(DIRECTORY_NAME, "IMG_" + System.currentTimeMillis(),
                        new OnCameraResultAdapter() {
                            @Override
                            public void onPhotoTaken(byte[] bytes, String filePath) {

                                try {
                                    MediaScannerConnection.scanFile(CameraActivity.this, new String[]{filePath}, null,
                                            new MediaScannerConnection.OnScanCompletedListener() {
                                                @Override
                                                public void onScanCompleted(String path, Uri uri) {
                                                    Intent intent = new Intent();
                                                    intent.putExtra("path", path);
                                                    intent.putExtra("type", 0);
                                                    setResult(101, intent);
                                                    finish();
                                                }
                                            });
                                } catch (Exception ignore) {
                                    fullBack();
                                }

                            }
                        }
                );
            }

            @Override
            public void onLongClickStart() {
                if (!enableRecord) return;
                final ICameraFragment cameraFragment = getCameraFragment();
                cameraFragment.switchCaptureAction(MediaAction.ACTION_VIDEO);
                cameraFragment.startRecordingVideo(DIRECTORY_NAME, "VID_" + System.currentTimeMillis());
            }

            @Override
            public void onLongClickEnd() {
                if (!enableRecord) return;
                final ICameraFragment cameraFragment = getCameraFragment();
                cameraFragment.stopRecordingVideo(new OnCameraResultAdapter() {
                    @Override
                    public void onVideoRecorded(String filePath) {

                        try {
                            MediaScannerConnection.scanFile(CameraActivity.this, new String[]{filePath}, null,
                                    new MediaScannerConnection.OnScanCompletedListener() {
                                        @Override
                                        public void onScanCompleted(String path, Uri uri) {
                                            Intent intent = new Intent();
                                            intent.putExtra("path", path);
                                            intent.putExtra("type", 1);
                                            setResult(101, intent);
                                            finish();
                                        }
                                    });
                        } catch (Exception ignore) {
                            fullBack();
                        }

                    }
                });
                cameraFragment.switchCaptureAction(MediaAction.ACTION_PHOTO);
            }
        });
    }

    private void setupCameraFragment() {
        if (Build.VERSION.SDK_INT > 15) {
            final String[] permissions = {
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE};

            final List<String> permissionsToRequest = new ArrayList<>();
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }
            if (!permissionsToRequest.isEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(
                        new String[permissionsToRequest.size()]), CameraConstant.REQUEST_CODE_CAMERA_PERMISSIONS);
            } else addCameraFragment();
        } else {
            addCameraFragment();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length != 0) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            addCameraFragment();
        }
    }

    public void addCameraFragment() {
        mCameraLayout.setVisibility(View.VISIBLE);
        final CameraFragment cameraFragment = CameraFragment.newInstance(new CameraConfig.Builder()
                .setCamera(CameraConfig.CAMERA_FACE_REAR).build());
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, cameraFragment, TAG_CAMERA_FRAGMENT)
                .commitAllowingStateLoss();

        if (cameraFragment != null && enableRecord) {
            cameraFragment.setTextListener(new CameraVideoRecordTextAdapter() {
                @Override
                public void setRecordSizeText(long size, String text) {
                    mRecordSizeText.setText(text);
                }

                @Override
                public void setRecordSizeTextVisible(boolean visible) {
                    mRecordSizeText.setVisibility(visible ? View.VISIBLE : View.GONE);
                }

                @Override
                public void setRecordDurationText(String text) {
                    mRecordDurationText.setText(text);
                }

                @Override
                public void setRecordDurationTextVisible(boolean visible) {
                    mRecordDurationText.setVisibility(visible ? View.VISIBLE : View.GONE);
                }
            });
        }
    }


    private ICameraFragment getCameraFragment() {
        return (ICameraFragment) getSupportFragmentManager().findFragmentByTag(TAG_CAMERA_FRAGMENT);
    }


    public static void startAction(Context mContext){
        Intent intent = new Intent(mContext, CameraActivity.class);
        mContext.startActivity(intent);
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //判断是否有焦点
        if(hasFocus && Build.VERSION.SDK_INT >= 19){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            |View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            |View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            |View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            |View.SYSTEM_UI_FLAG_FULLSCREEN
                            |View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );

        }
    }

    private void fullBack(){

        setResult(101);
        finish();

    }

    private void showTips(String s){
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }


}
