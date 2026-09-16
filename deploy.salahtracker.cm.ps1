<#
.SYNOPSIS
Triggers a Codemagic build for Salah Tracker.

.DESCRIPTION
This script uses the Codemagic REST API to trigger the "android-play-store" workflow.
You must insert your Codemagic API Token and your Application ID below.

.EXAMPLE
.\deploy.salahtracker.cm.ps1
#>

# ==========================================
# CONFIGURATION - PLEASE UPDATE THESE VALUES
# ==========================================

# 1. Your Codemagic Personal Access Token
# You can generate this in your Codemagic account settings: https://codemagic.io/teams
$ApiToken = "55_psRL2CD3GuZbN6dFU1MWsZjrRJRwgHxIKwhDMovE"

# 2. Your Application ID
# This is found in the URL when you open your app in the Codemagic dashboard.
# E.g. https://codemagic.io/app/1234567890abcdef -> The App ID is 1234567890abcdef
$AppId = "6a5e72231b95881231eac607"

# 3. Your Workflow ID (from codemagic.yaml)
$WorkflowId = "android-play-store"

# 4. Your Branch Name
$Branch = "main"

# ==========================================
# SCRIPT LOGIC
# ==========================================

Clear-Host
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "      Salah Tracker - Codemagic Deploy       " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

if ($ApiToken -eq "YOUR_CODEMAGIC_API_TOKEN" -or $AppId -eq "YOUR_APP_ID") {
    Write-Host "[ERROR] Configuration missing!" -ForegroundColor Red
    Write-Host "Please edit this script and add your API Token and App ID." -ForegroundColor Yellow
    exit 1
}

$Headers = @{
    "x-auth-token" = $ApiToken
    "Content-Type" = "application/json"
}

$Body = @{
    appId = $AppId
    workflowId = $WorkflowId
    branch = $Branch
} | ConvertTo-Json

Write-Host "[INFO] Triggering workflow '$WorkflowId' on branch '$Branch'..." -ForegroundColor White

try {
    $Response = Invoke-RestMethod -Uri "https://api.codemagic.io/builds" -Method Post -Headers $Headers -Body $Body
    
    if ($Response.buildId) {
        Write-Host ""
        Write-Host ">>> SUCCESS! Build Triggered. <<<" -ForegroundColor Green
        Write-Host "Build ID: $($Response.buildId)" -ForegroundColor Green
        Write-Host ""
        Write-Host "View live progress at:" -ForegroundColor Yellow
        Write-Host "https://codemagic.io/app/$AppId/build/$($Response.buildId)" -ForegroundColor Cyan
    } else {
        Write-Host "[ERROR] Unexpected response from Codemagic API." -ForegroundColor Red
        Write-Host ($Response | ConvertTo-Json)
    }
} catch {
    Write-Host "[ERROR] Failed to trigger build!" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    
    if ($_.ErrorDetails) {
        Write-Host $_.ErrorDetails.Message -ForegroundColor Red
    }
    exit 1
}
