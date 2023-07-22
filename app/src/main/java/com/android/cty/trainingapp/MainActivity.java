package com.android.cty.trainingapp;

import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.Size;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.Manifest;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysis.Analyzer;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;

import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private ExecutorService cameraExecutor;
    private PreviewView previewView;
    private PoseDetector poseDetector;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String currentVideoPath;
    private VideoCapture videoCapture = null;
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化相机执行器
        cameraExecutor = Executors.newSingleThreadExecutor();

        // 获取相机预览视图
        previewView = findViewById(R.id.previewView);

        // 初始化姿势估计器
        poseDetector = getPoseDetector();

        requestExternalStoragePermission();

        // 检查并请求相机权限
        if (hasCameraPermission()) {
            // 如果已经有相机权限，启动
            startCamera();
        } else {
            // 如果没有相机权限，请求相机权限
            requestCameraPermission();
        }



    }
    // 檢查並請求儲存空間權限
    private static final int REQUEST_MANAGE_EXTERNAL_STORAGE_PERMISSION = 1002;
    private void checkStoragePermission() {
        if (!Environment.isExternalStorageManager()) {
            // 尚未授予外部儲存空間管理員權限，請求該權限
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE_PERMISSION);
        } else {
            // 已經有外部儲存空間管理員權限，可以進行需要存取儲存空間的功能
            // 在這裡啟動相機預覽等相關操作
            startCamera();
        }
    }



    // 检查是否拥有相机权限
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    // 请求相机权限
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                REQUEST_CAMERA_PERMISSION);
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 使用者授予相機權限，繼續檢查儲存空間權限
                checkStoragePermission();
            } else {
                // 使用者拒絕相機權限，您可以進行適當處理，例如顯示一個提示訊息
                Log.e("CameraXDemo", "Camera permission denied");
            }
        } else if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 使用者授予儲存空間權限，繼續執行需要存取儲存空間的功能
                // 在這裡啟動相機預覽等相關操作
                startCamera();
            } else {
                // 使用者拒絕儲存空間權限，您可以進行適當處理，例如顯示一個提示訊息
                Log.e("CameraXDemo", "Storage permission denied");
            }
        }
    }

    // 获取姿势估计器实例的方法
    private PoseDetector getPoseDetector() {
        PoseDetectorOptions options = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build();
        return PoseDetection.getClient(options);
    }

    private Camera camera;
    private ProcessCameraProvider cameraProvider;



    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                if(cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)){
                    // 配置相机预览
                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(previewView.getSurfaceProvider());

                    // 找到OverlayView
                    OverlayView overlayView = findViewById(R.id.overlayView);



                    // 配置图像分析
//                    ImageAnalysis imageAnalysis =
//                            new ImageAnalysis.Builder()
//                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                                    .setTargetResolution(new Size(640, 480)) // 调整图像分辨率
//                                    .setTargetRotation(previewView.getDisplay().getRotation())
//                                    .build();
//
//
//                    // 设置图像分析回调
//                    imageAnalysis.setAnalyzer(cameraExecutor, new PoseAnalyzer(overlayView));

                    // 选择后置相机
                    CameraSelector cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build();

                    cameraProvider.unbindAll();
                    // 绑定相机生命周期
                    camera = cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, preview);
                }else {
                    // 处理相机不可用的情况
                    Toast.makeText(this, "后置相机不可用", Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private int frameCounter = 0;
    private int frameSkip = 15; // 每处理 15 帧，跳过一帧
    private class PoseAnalyzer implements Analyzer {

        private OverlayView overlayView;

        public PoseAnalyzer(OverlayView overlayView) {
            this.overlayView = overlayView;
        }

        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            // 每处理 frameSkip 帧，才进行图像处理
            frameCounter++;
            if (frameCounter % frameSkip != 0) {
                imageProxy.close();
                return;
            }
            // 获取图像数据
            ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int width = imageProxy.getWidth();
            int height = imageProxy.getHeight();
            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();

            // 创建InputImage对象
            InputImage image = InputImage.fromByteBuffer(buffer, width, height, rotationDegrees,
                    InputImage.IMAGE_FORMAT_NV21);

            // 进行姿势估计
            poseDetector.process(image)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Pose pose = task.getResult();
                            // 在这里处理姿势估计的结果
                            // 你可以获取关节节点信息，并在预览界面上绘制
                            List<PoseLandmark> landmarks = pose.getAllPoseLandmarks();
                            if (landmarks != null) {
                                for (PoseLandmark landmark : landmarks) {
                                    // 处理关节节点的信息
                                    // landmark.getPosition() 返回关节节点在图像中的位置
                                    overlayView.setPoseLandmarks(landmarks);
                                }
                            }
                        }
                        // 处理完图像后，关闭ImageProxy
                        imageProxy.close();
                    });
        }
    }

    // 在按鈕點擊事件中調用此方法
    public void onButtonClick(View view) {
        // 檢查儲存空間權限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            // 已經有儲存空間權限，執行錄製操作
            if (isRecording) {
                stopRecording();
                ((Button) view).setText("Start Recording");
            } else {
                startRecording();
                ((Button) view).setText("Stop Recording");
            }
        } else {
            // 尚未授予儲存空間權限，請求儲存空間權限
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION);
        }
    }





    private void startRecording() {
        // 创建视频文件路径
        currentVideoPath = getOutputMediaFile().getAbsolutePath();

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(currentVideoPath);
        mediaRecorder.setVideoEncodingBitRate(10000000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(1280, 720);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setOrientationHint(90);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
        } catch (IOException e) {
            Log.e("TAG", "MediaRecorder prepare failed: " + e.getMessage());
            releaseMediaRecorder();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null && isRecording) {
            mediaRecorder.stop();
            releaseMediaRecorder();
            isRecording = false;
        }
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 2;
    private File getOutputMediaFile() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            // 取得Scoped Storage的儲存目錄
            File mediaStorageDir = new File(getExternalFilesDir(Environment.DIRECTORY_DCIM), "Camera");

            // 確保目錄存在
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return null;
                }
            }

            // 現在您可以在此路徑下創建影片檔案，並儲存錄製的影片
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            currentVideoPath = mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4";
            return new File(currentVideoPath); // 返回創建的檔案物件
        } else {
            // 尚未授予權限，請求儲存空間權限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION);
            return null;
        }
    }

        @Override
        protected void onDestroy () {
            super.onDestroy();
            cameraExecutor.shutdown();

            // 释放相机资源
            if (camera != null) {
                camera.getCameraControl().enableTorch(false); // 关闭闪光灯（如果有打开的话）
                cameraProvider.unbindAll(); // 解绑所有用例
                camera = null;
            }
        }
    // 在您的Activity中定義一個請求碼，用於識別您的權限請求
    private static final int REQUEST_EXTERNAL_STORAGE_PERMISSION = 1001;



    // 在適當的地方（例如按鈕點擊事件）執行以下程式碼
    public void requestExternalStoragePermission() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_EXTERNAL_STORAGE_PERMISSION);
    }

    // 處理用戶對話框的回應
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MANAGE_EXTERNAL_STORAGE_PERMISSION) {
            if (Environment.isExternalStorageManager()) {
                // 已經授予外部儲存空間管理員權限，可以進行需要存取儲存空間的功能
                // 在這裡啟動相機預覽等相關操作
                startCamera();
            } else {
                // 用戶拒絕授予外部儲存空間管理員權限，您可以進行適當處理，例如顯示一個提示訊息
                Log.e("StoragePermission", "User denied external storage manager permission");
            }
        }
    }

}
