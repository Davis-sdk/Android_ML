package com.example.machine_learning_android;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.machine_learning_android.clasifier.Classifier;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;



import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class TensorMain extends AppCompatActivity {

    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN =200;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME ="input";
    private static final String OUTPUT_NAME = "output";

    private static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    private static final String LABEL_FILE = "file:///android_asset/imagenet_comp_graph_label_strings.txt";


    private Classifier classifier;
    private Executor executer = Executors.newSingleThreadExecutor();
    private TextView textViewResult;
    private ImageButton btnDetectObject, btnToggleCamera;
    private ImageView imageViewResult;
    private CameraView cameraView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Image Detection");

        initItems();


        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {

                Bitmap bitmap = cameraKitImage.getBitmap();
                bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
                imageViewResult.setImageBitmap(bitmap);
                final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);

                textViewResult.setText(results.toString());
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });



        btnToggleCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.toggleFacing();
            }
        });


        btnDetectObject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.captureImage();
            }
        });

    initTensorflowAndLoadModel();


    }

    public void initItems()
    {
        cameraView = (CameraView) findViewById(R.id.cameraView);
        imageViewResult = (ImageView) findViewById(R.id.imageViewResult);
        textViewResult = (TextView) findViewById(R.id.textViewResult);

        btnToggleCamera = (ImageButton) findViewById(R.id.btnToggleCamera);
        btnDetectObject = (ImageButton) findViewById(R.id.btnDetectObject);
    }


    @Override
    protected void onResume()
    {
        super.onResume();
        cameraView.start();
    }


    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        executer.execute(new Runnable()
        {
            @Override
            public void run()
            {
                classifier.close();
            }
        });
    }


    private void initTensorflowAndLoadModel()
    {
        executer.execute(new Runnable ()
        {
            @Override
            public void run()
            {
                try
                {
                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_FILE,
                            LABEL_FILE,
                            INPUT_SIZE,
                            IMAGE_MEAN,
                            IMAGE_STD,
                            INPUT_NAME,
                            OUTPUT_NAME);
                        makeButtonVisible();
                }catch (final Exception e)
                {
                    throw new RuntimeException("Error initializing TensorFlow", e);
                }
            }
        });

    }

    private void makeButtonVisible()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                btnDetectObject.setVisibility(View.VISIBLE);
            }
        });
    }


}
