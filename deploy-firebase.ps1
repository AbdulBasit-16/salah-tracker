# Deployment script for Salah Tracker

# 1. Auto-update version in app/build.gradle.kts
$buildGradlePath = "$PSScriptRoot\app\build.gradle.kts"
if (Test-Path $buildGradlePath) {
    $gradleContent = Get-Content $buildGradlePath -Raw
    
    $versionCodeMatch = [regex]::Match($gradleContent, 'versionCode\s*=\s*(\d+)')
    $versionNameMatch = [regex]::Match($gradleContent, 'versionName\s*=\s*"([^"]+)"')
    
    if ($versionCodeMatch.Success -and $versionNameMatch.Success) {
        $oldVersionCode = [int]$versionCodeMatch.Groups[1].Value
        $oldVersionName = $versionNameMatch.Groups[1].Value
        
        $currentDate = Get-Date -Format "MM.dd"
        $newVersionCode = $oldVersionCode + 1
        $newVersionName = "$currentDate.$newVersionCode"
        
        Write-Host "🔄 Updating version from $oldVersionName (Code: $oldVersionCode) to $newVersionName (Code: $newVersionCode)..." -ForegroundColor Yellow
        
        $gradleContent = $gradleContent -replace 'versionCode\s*=\s*\d+', "versionCode = $newVersionCode"
        $gradleContent = $gradleContent -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$newVersionName`""
        
        [System.IO.File]::WriteAllText($buildGradlePath, $gradleContent)
        Write-Host "✅ Updated version in app/build.gradle.kts" -ForegroundColor Green
    } else {
        Write-Host "⚠️ Could not parse version in app/build.gradle.kts" -ForegroundColor Red
    }
} else {
    Write-Host "❌ app/build.gradle.kts not found at $buildGradlePath" -ForegroundColor Red
}

# 2. Setup Gradle Environment
$scratchDir = "C:\Users\Malik\.gemini\antigravity\scratch"
$gradleZip = Join-Path $scratchDir "gradle-8.5-bin.zip"
$gradleDest = Join-Path $scratchDir "gradle-bin"
$gradleBin = Join-Path $gradleDest "gradle-8.5\bin\gradle.bat"

if (-not (Test-Path $gradleZip)) {
    Write-Host "Downloading Gradle 8.5..." -ForegroundColor Cyan
    Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-8.5-bin.zip" -OutFile $gradleZip
}
if (-not (Test-Path $gradleDest)) {
    Write-Host "Extracting Gradle 8.5..." -ForegroundColor Cyan
    New-Item -ItemType Directory -Path $gradleDest -Force | Out-Null
    Expand-Archive -Path $gradleZip -DestinationPath $gradleDest -Force
}

$javaPath = "C:\Program Files\Java\jdk-21"
if (-not (Test-Path $javaPath)) {
    $javaPath = "C:\Program Files\Java\jdk-23"
}
$env:JAVA_HOME = $javaPath
$env:PATH = "$javaPath\bin;" + $env:PATH
$env:ANDROID_HOME = "C:\Users\Malik\AppData\Local\Android\Sdk"

# 3. Build and Deploy
Write-Host "🚀 Starting Android Build..." -ForegroundColor Cyan
Set-Location -Path $PSScriptRoot
& $gradleBin clean assembleDebug

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Build Successful! Starting Firebase Deployment..." -ForegroundColor Green
    & $gradleBin appDistributionUploadDebug
} else {
    Write-Host "❌ Build Failed. Deployment aborted." -ForegroundColor Red
}
