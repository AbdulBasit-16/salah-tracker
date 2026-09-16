$flutterZipUrl = "https://storage.googleapis.com/flutter_infra_release/releases/stable/windows/flutter_windows_3.24.0-stable.zip"
$scratchDir = "C:\Users\Malik\.gemini\antigravity\scratch"
$zipPath = Join-Path $scratchDir "flutter.zip"
$extractPath = $scratchDir

Write-Host "Downloading Flutter SDK..." -ForegroundColor Cyan
Invoke-WebRequest -Uri $flutterZipUrl -OutFile $zipPath

Write-Host "Extracting Flutter SDK..." -ForegroundColor Cyan
Expand-Archive -Path $zipPath -DestinationPath $extractPath -Force

$flutterBinPath = Join-Path $extractPath "flutter\bin"
Write-Host "Adding Flutter to PATH: $flutterBinPath" -ForegroundColor Cyan

$userPath = [Environment]::GetEnvironmentVariable("PATH", "User")
if ($userPath -notlike "*$flutterBinPath*") {
    $newPath = $userPath + ";" + $flutterBinPath
    [Environment]::SetEnvironmentVariable("PATH", $newPath, "User")
    Write-Host "Flutter added to User PATH permanently." -ForegroundColor Green
} else {
    Write-Host "Flutter is already in the User PATH." -ForegroundColor Yellow
}

Write-Host "Installation complete!" -ForegroundColor Green
