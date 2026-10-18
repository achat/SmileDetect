# Smiley

Smiley is a Java desktop app that reads a live webcam feed, classifies the largest visible face as happy, sad, or neutral, and paints a matching expression. Frames are processed locally and are not saved or uploaded.

## Requirements

- Java 17 or newer
- Maven 3.8 or newer
- A webcam
- Windows PowerShell to download the model files (the Java app itself is cross-platform)

## Get the models

From the project root, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\download-models.ps1
```

This downloads the ONNX FER+ emotion classifier and OpenCV's frontal-face cascade into `models/`. The classifier is distributed by the ONNX Model Zoo and the face cascade by OpenCV.

## Run

```powershell
mvn exec:java
```

Click **Start camera** to begin. Use **Stop** to release the webcam. If the camera does not open, check operating-system camera permissions and close any other app using it.

When the detected expression changes to happy or sad, Smiley plays a short cheerful or wavering crying cue. Sounds are synthesized locally and are not saved.

## Build

```powershell
mvn package
```

The application requires the model files at the paths created by the download script when camera analysis starts. The model classifies eight FER+ categories; Smiley maps happiness to a smile, sadness to a sad face, and other categories to a neutral face.
