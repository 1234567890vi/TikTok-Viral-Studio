param (
    [Parameter(Mandatory=$false)]
    [string]$Version = "1.0.1",

    [Parameter(Mandatory=$false)]
    [string]$Message = "New Remote Update & Feature Rollout"
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "🚀 TikTok Viral Studio - Automated Release & Remote Update" -ForegroundColor Yellow
Write-Host "Publishing Version: v$Version" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Update remote_config.json
if (Test-Path "remote_config.json") {
    $config = Get-Content "remote_config.json" -Raw | ConvertFrom-Json
    $config.latest_version_code = [int]$config.latest_version_code + 1
    $config.latest_version_name = $Version
    $config.apk_download_url = "https://github.com/1234567890vi/TikTok-Viral-Studio/releases/download/v$Version/TikTok-Viral-Studio-v$Version.apk"
    $config | ConvertTo-Json -Depth 10 | Set-Content "remote_config.json" -Encoding UTF8
    Write-Host "✓ remote_config.json updated with version v$Version and code $($config.latest_version_code)" -ForegroundColor Green
}

# 2. Update app/build.gradle.kts
if (Test-Path "app/build.gradle.kts") {
    $gradleContent = Get-Content "app/build.gradle.kts" -Raw
    $gradleContent = $gradleContent -replace 'versionName = ".*"', "versionName = `"$Version`""
    Set-Content "app/build.gradle.kts" -Value $gradleContent -Encoding UTF8
    Write-Host "✓ app/build.gradle.kts updated to versionName: $Version" -ForegroundColor Green
}

# 3. Commit and Push to GitHub
Write-Host "Pushing changes and tag to GitHub..." -ForegroundColor Cyan
git add .
git commit -m "Release v$Version: $Message"

$ghToken = (gh auth token)
git push "https://1234567890vi:$ghToken@github.com/1234567890vi/TikTok-Viral-Studio.git" main
git tag -f "v$Version"
git push "https://1234567890vi:$ghToken@github.com/1234567890vi/TikTok-Viral-Studio.git" -f "v$Version"

Write-Host ""
Write-Host "🎉 Version v$Version successfully pushed to GitHub!" -ForegroundColor Green
Write-Host "GitHub Actions Monitor: https://github.com/1234567890vi/TikTok-Viral-Studio/actions" -ForegroundColor Yellow
Write-Host "Direct APK Download: https://github.com/1234567890vi/TikTok-Viral-Studio/releases/download/v$Version/TikTok-Viral-Studio-v$Version.apk" -ForegroundColor Cyan
