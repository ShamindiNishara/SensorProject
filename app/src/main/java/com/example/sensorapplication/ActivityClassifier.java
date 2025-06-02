package com.example.sensorapplication;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ActivityClassifier {

    private static final String MODEL_FILE = "model.tflite";  // Your converted model
    private static final String INPUT_NODE = "LSTM_1_input";
    private static final String[] OUTPUT_NODES = {"Dense_2/Softmax"};
    private static final String OUTPUT_NODE = "Dense_2/Softmax";
    private static final long[] INPUT_SIZE = {1, 100, 12};
    private static final int OUTPUT_SIZE = 7;

    private Interpreter tflite;

    public ActivityClassifier(Context context) throws IOException {
        tflite = new Interpreter(loadModelFile(context));
    }

    // Load the .tflite model from assets
    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // Predict activity probabilities
    public float[] predictProbabilities(float[] data) {
        float[][][] input = new float[1][100][12];  // Shape: [1, 100, 12]
        int index = 0;
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 12; j++) {
                input[0][i][j] = data[index++];
            }
        }

        float[][] output = new float[1][OUTPUT_SIZE];  // Shape: [1, 7]
        tflite.run(input, output);  // Run inference

        // Optional: log the output for debugging
        Log.d("ActivityClassifier", "Predicted probabilities: ");
        for (int i = 0; i < OUTPUT_SIZE; i++) {
            Log.d("ActivityClassifier", "Class " + i + ": " + output[0][i]);
        }

        return output[0];  // Return the output probabilities
    }

}
