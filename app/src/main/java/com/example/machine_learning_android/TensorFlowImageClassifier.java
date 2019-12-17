package com.example.machine_learning_android;


import android.content.res.AssetManager;
import android.util.Log;

import com.example.machine_learning_android.clasifier.Classifier;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;


import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Vector;

import androidx.core.os.TraceCompat;


public class TensorFlowImageClassifier implements Classifier {

    private static final String TAG = "ImageClassifier";

    private static final int MAX_RESULT = 3;
    private static final float THRESHOLD = 0.1f;

    private String inputName;
    private String outputName;
    private int inputSize;
    private int imageMean;
    private float imageStd;

    private Vector<String> labels = new Vector<String>();
    private int[] intValues;
    private float[] floatValues;
    private float[] outputs;
    private String[] outputNames;

    private TensorFlowInferenceInterface inferenceInterface;
    private boolean runStats = false;

    private TensorFlowImageClassifier() {

    }


    public static Classifier create(
            AssetManager assetManager,
            String modelFilename,
            String labelFilename,
            int inputSize,
            int imageMean,
            float imageStd,
            String inputName,
            String outputName)
            throws IOException {
        TensorFlowImageClassifier c = new TensorFlowImageClassifier();
        c.inputName = inputName;
        c.outputName = outputName;


        String actualFilename = labelFilename.split("file:///android_asset/")[1];

        BufferedReader br = null;
        br = new BufferedReader(new InputStreamReader(assetManager.open(actualFilename)));
        String line;
        while ((line = br.readLine()) != null) {
            c.labels.add(line);
        }
        br.close();

        c.inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFilename);

        int numClasses = (int) c.inferenceInterface.graph().operation(outputName).output(0).shape().size(1);


        c.inputSize = inputSize;
        c.imageMean = imageMean;
        c.imageStd = imageStd;

        c.outputNames = new String[]{outputName};
        c.intValues = new int[inputSize * inputSize];
        c.floatValues = new float[inputSize * inputSize * 3];
        c.outputs = new float[numClasses];

        return c;


    }

@Override
    public List<Recognition> recognizeImage(final Bitmap bitmap)
{
    TraceCompat.beginSection("recognizeImage");

    TraceCompat.beginSection("preprocessBitmap");

    bitmap.getPixels(intValues,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());
    for(int i = 0; i < intValues.length; i++)
    {
        final int val =intValues[i];
        floatValues[i * 3 + 0] = (((val >> 16) & 0xFF) - imageMean) / imageStd;
        floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) - imageMean) / imageStd;
        floatValues[i * 3 + 2] = ((val & 0xFF) - imageMean) / imageStd;
    }
    TraceCompat.endSection();

    TraceCompat.beginSection("feed");
    inferenceInterface.feed(inputName, floatValues, new long[]{1,inputSize, inputSize,3});
    TraceCompat.endSection();
    //kreipiasi i interface class
    TraceCompat.beginSection("run");
    inferenceInterface.run(outputNames,runStats);
    TraceCompat.endSection();
    //kopina outputa i isejimo masyvva...
    TraceCompat.beginSection("fetch");
    inferenceInterface.fetch(outputName,outputs);
    TraceCompat.endSection();


    PriorityQueue<Recognition> pq =
            new PriorityQueue<Recognition>(
                    3,
                    new Comparator<Recognition>() {
                        @Override
                        public int compare(Recognition o1, Recognition o2) {
                            return Float.compare(o2.getConfidence(), o1.getConfidence());
                        }
                    });
    for(int i=0; i<outputs.length; i++)
    {
        if(outputs[i] >THRESHOLD)
        {
            pq.add(
                    new Recognition(
                            "" + i, labels.size() > i ? labels.get(i) : "unknown", outputs[i], null));
        }
    }
    final ArrayList<Recognition> recognitions = new ArrayList<Recognition>();
    int recognitionsSize = Math.min(pq.size(),MAX_RESULT);
    for(int i = 0;i<recognitionsSize; i++)
    {
        recognitions.add(pq.poll());
    }
    TraceCompat.endSection();
    return recognitions;
}



@Override
    public void enableStatLogging(boolean debug)
    {
        runStats = debug;
    }

    @Override
    public String getStatString()
    {
        return inferenceInterface.getStatString();
    }

    @Override
    public void close()
    {
        inferenceInterface.close();
    }




}
