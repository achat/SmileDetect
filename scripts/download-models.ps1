$ErrorActionPreference = 'Stop'
$modelDirectory = Join-Path $PSScriptRoot '..\models'
New-Item -ItemType Directory -Force -Path $modelDirectory | Out-Null

$downloads = @(
    @{
        Url = 'https://github.com/onnx/models/raw/main/validated/vision/body_analysis/emotion_ferplus/model/emotion-ferplus-8.onnx'
        Destination = Join-Path $modelDirectory 'emotion-ferplus-8.onnx'
    },
    @{
        Url = 'https://raw.githubusercontent.com/opencv/opencv/4.x/data/haarcascades/haarcascade_frontalface_default.xml'
        Destination = Join-Path $modelDirectory 'haarcascade_frontalface_default.xml'
    }
)

foreach ($download in $downloads) {
    if (-not (Test-Path $download.Destination)) {
        Write-Host "Downloading $([IO.Path]::GetFileName($download.Destination))..."
        Invoke-WebRequest -Uri $download.Url -OutFile $download.Destination
    }
}

Write-Host "Model files are ready in $modelDirectory"
