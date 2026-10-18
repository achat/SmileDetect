package com.smiley;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

final class EmotionAnalyzer implements AutoCloseable {
    enum Expression {
        NEUTRAL("Neutral"), HAPPY("Happy"), SAD("Sad");

        private final String label;

        Expression(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }

    record Analysis(Expression expression, String detail) { }

    private static final String[] LABELS = {
            "neutral", "happiness", "surprise", "sadness",
            "anger", "disgust", "fear", "contempt"
    };

    private final OrtSession session;
    private final String inputName;
    private final CascadeClassifier faceDetector;

    EmotionAnalyzer(Path modelPath, Path cascadePath) throws OrtException, IOException {
        if (!Files.isRegularFile(modelPath)) {
            throw new IOException("Emotion model not found: " + modelPath.toAbsolutePath());
        }
        if (!Files.isRegularFile(cascadePath)) {
            throw new IOException("Face detector not found: " + cascadePath.toAbsolutePath());
        }

        faceDetector = new CascadeClassifier(cascadePath.toString());
        if (faceDetector.empty()) {
            throw new IOException("Could not load face detector: " + cascadePath.toAbsolutePath());
        }

        OrtEnvironment environment = OrtEnvironment.getEnvironment();
        session = environment.createSession(modelPath.toString(), new OrtSession.SessionOptions());
        inputName = session.getInputNames().iterator().next();
    }

    Analysis analyze(Mat frame) throws OrtException {
        Mat gray = new Mat();
        MatOfRect detections = new MatOfRect();
        try {
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            faceDetector.detectMultiScale(gray, detections, 1.1, 4, 0, new Size(64, 64), new Size());
            Rect faceBounds = java.util.Arrays.stream(detections.toArray())
                    .max(Comparator.comparingLong(rectangle -> (long) rectangle.width * rectangle.height))
                    .orElse(null);
            if (faceBounds == null) {
                return new Analysis(Expression.NEUTRAL, "No face detected");
            }

            Mat face = gray.submat(faceBounds);
            Mat resized = new Mat();
            try {
                Imgproc.resize(face, resized, new Size(64, 64), 0, 0, Imgproc.INTER_AREA);
                byte[] pixels = new byte[64 * 64];
                resized.get(0, 0, pixels);
                float[] input = new float[pixels.length];
                for (int index = 0; index < pixels.length; index++) {
                    input[index] = (pixels[index] & 0xff) / 255.0f;
                }
                return classify(input);
            } finally {
                face.release();
                resized.release();
            }
        } finally {
            gray.release();
            detections.release();
        }
    }

    private Analysis classify(float[] input) throws OrtException {
        try (OnnxTensor tensor = OnnxTensor.createTensor(
                OrtEnvironment.getEnvironment(), FloatBuffer.wrap(input), new long[]{1, 1, 64, 64});
             OrtSession.Result result = session.run(Map.of(inputName, tensor))) {
            Object value = result.get(0).getValue();
            float[] scores = value instanceof float[][] batch ? batch[0] : (float[]) value;
            int bestIndex = 0;
            for (int index = 1; index < scores.length; index++) {
                if (scores[index] > scores[bestIndex]) {
                    bestIndex = index;
                }
            }
            Expression expression = switch (LABELS[bestIndex]) {
                case "happiness" -> Expression.HAPPY;
                case "sadness" -> Expression.SAD;
                default -> Expression.NEUTRAL;
            };
            return new Analysis(expression, LABELS[bestIndex]);
        }
    }

    @Override
    public void close() throws OrtException {
        session.close();
    }
}
