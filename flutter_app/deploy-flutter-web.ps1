# Deployment script for Salah Tracker (Flutter Web)

# Auto-update version in pubspec.yaml
$pubspecPath = "$PSScriptRoot\pubspec.yaml"
if (Test-Path $pubspecPath) {
    $pubspecContent = Get-Content $pubspecPath -Raw
    if ($pubspecContent -match 'version:\s*(\d{2}\.\d{2}\.\d{2})\+(\d+)') {
        $oldVersion = $Matches[0]
        $oldDate = $Matches[1]
        $oldBuild = [int]$Matches[2]
        
        $currentDate = Get-Date -Format "MM.dd"
        $newBuild = $oldBuild + 1
        $newVersion = "$currentDate.$newBuild+$newBuild"
        
        Write-Host "🔄 Updating version from $oldDate+$oldBuild to $newVersion..." -ForegroundColor Yellow
        
        $pubspecContent = $pubspecContent -replace 'version:\s*\d{2}\.\d{2}\.\d{2}\+\d+', "version: $newVersion"
        [System.IO.File]::WriteAllText($pubspecPath, $pubspecContent)
        Write-Host "✅ Updated version in pubspec.yaml" -ForegroundColor Green
    } elseif ($pubspecContent -match 'version:\s*(\d+\.\d+\.\d+)\+(\d+)') {
        # Fallback for initial version format like 1.0.0+1
        $oldBuild = [int]$Matches[2]
        $currentDate = Get-Date -Format "MM.dd"
        $newBuild = $oldBuild + 1
        $newVersion = "$currentDate.$newBuild+$newBuild"
        
        Write-Host "🔄 Updating version to $newVersion..." -ForegroundColor Yellow
        
        $pubspecContent = $pubspecContent -replace 'version:\s*\d+\.\d+\.\d+\+\d+', "version: $newVersion"
        [System.IO.File]::WriteAllText($pubspecPath, $pubspecContent)
        Write-Host "✅ Updated version in pubspec.yaml" -ForegroundColor Green
    } else {
        Write-Host "⚠️ Could not parse version in pubspec.yaml" -ForegroundColor Red
    }
} else {
    Write-Host "❌ pubspec.yaml not found at $pubspecPath" -ForegroundColor Red
}

Write-Host "🚀 Starting Web Build..." -ForegroundColor Cyan
Set-Location -Path $PSScriptRoot
$env:PATH += ";C:\Users\Malik\.gemini\antigravity\scratch\flutter\bin"
flutter build web --release

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Build Successful! Starting Firebase Deployment..." -ForegroundColor Green
    firebase deploy --only hosting
} else {
    Write-Host "❌ Build Failed. Deployment aborted." -ForegroundColor Red
}
