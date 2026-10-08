$ErrorActionPreference = 'Stop'
$buildTools = 'D:\Android\Sdk\build-tools\35.0.0'
$output = Join-Path $PSScriptRoot '../app/build/providers'
$overlay = Join-Path $PSScriptRoot 'overlay'
$assetDirectory = Join-Path $PSScriptRoot '../app/src/main/assets/providers'
New-Item -ItemType Directory -Path $output -Force | Out-Null
& "$buildTools/aapt2.exe" compile --dir "$overlay/res" -o "$output/resources.zip"
if ($LASTEXITCODE -ne 0) { throw 'Overlay compilation failed' }
& "$buildTools/aapt2.exe" link -o "$output/unsigned.apk" -I D:\Android\Sdk\platforms\android-35\android.jar --manifest "$overlay/AndroidManifest.xml" "$output/resources.zip"
if ($LASTEXITCODE -ne 0) { throw 'Overlay link failed' }
& "$buildTools/zipalign.exe" -f 4 "$output/unsigned.apk" "$output/aligned.apk"
if ($LASTEXITCODE -ne 0) { throw 'Overlay alignment failed' }
& "$buildTools/apksigner.bat" sign --ks "$env:USERPROFILE/.android/debug.keystore" --ks-pass pass:android --key-pass pass:android --out "$assetDirectory/GappuccinoProviders.apk" "$output/aligned.apk"
if ($LASTEXITCODE -ne 0) { throw 'Overlay signing failed' }
& "$buildTools/apksigner.bat" verify "$assetDirectory/GappuccinoProviders.apk"
if ($LASTEXITCODE -ne 0) { throw 'Overlay verification failed' }
