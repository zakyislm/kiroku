# Kiroku Desktop Standalone Executable & Installer Builder
# This script compiles and packages the Java desktop application into a standalone folder or a professional installer.

Param(
    [string]$Type = "app-image"  # Options: app-image (standalone folder), msi (MSI installer), exe (EXE installer wrapper)
)

$ErrorActionPreference = "Stop"

Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "         Kiroku Build & Packaging Tool             " -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Locate Java Development Kit Tools (jpackage, javac, jar)
Write-Host "[1/5] Locating JDK tools..." -ForegroundColor Yellow
$jpackagePath = ""
$cmd = Get-Command jpackage -ErrorAction SilentlyContinue
if ($cmd) {
    $jpackagePath = $cmd.Source
} elseif ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\jpackage.exe")) {
    $jpackagePath = "$env:JAVA_HOME\bin\jpackage.exe"
} else {
    $javaDirs = Get-ChildItem -Path "C:\Program Files\Java" -Filter "jpackage.exe" -Recurse -ErrorAction SilentlyContinue
    if ($javaDirs) {
        $jpackagePath = $javaDirs | Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
    }
}

if (-not $jpackagePath -or -not (Test-Path $jpackagePath)) {
    Write-Error "Could not locate jpackage.exe. Please ensure JDK 14+ is installed and JAVA_HOME is configured."
}

$binDir = [System.IO.Path]::GetDirectoryName($jpackagePath)
$javacPath = Join-Path $binDir "javac.exe"
$jarPath = Join-Path $binDir "jar.exe"

Write-Host "Found JDK Binaries at: $binDir" -ForegroundColor Green

# 2. Locate Maven Dependencies
Write-Host ""
Write-Host "[2/5] Checking dependencies..." -ForegroundColor Yellow
$m2Repo = Join-Path $env:USERPROFILE ".m2\repository"
$jnaJar = Join-Path $m2Repo "net\java\dev\jna\jna\5.14.0\jna-5.14.0.jar"
$jnaPlatformJar = Join-Path $m2Repo "net\java\dev\jna\jna-platform\5.14.0\jna-platform-5.14.0.jar"
$gsonJar = Join-Path $m2Repo "com\google\code\gson\gson\2.10.1\gson-2.10.1.jar"

# Verify all dependency jars exist
$missing = @()
if (-not (Test-Path $jnaJar)) { $missing += "JNA 5.14.0" }
if (-not (Test-Path $jnaPlatformJar)) { $missing += "JNA Platform 5.14.0" }
if (-not (Test-Path $gsonJar)) { $missing += "Gson 2.10.1" }

if ($missing.Count -gt 0) {
    Write-Host "Some dependencies are missing from local Maven repository: $($missing -join ', ')" -ForegroundColor Red
    Write-Host "Attempting to download via maven..." -ForegroundColor Yellow
    
    $mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvnCmd) {
        & mvn dependency:resolve
    } else {
        Write-Error "Maven is not installed. Please build/run the application in your IDE first to download dependencies."
    }
}

Write-Host "All dependencies verified." -ForegroundColor Green

# Auto-copy latest icons from Downloads if present
if (Test-Path "$env:USERPROFILE\Downloads\desktop-app-logo-started.png") {
    Copy-Item "$env:USERPROFILE\Downloads\desktop-app-logo-started.png" -Destination "src\main\resources\" -Force
} elseif (Test-Path "$env:USERPROFILE\Downloads\icons\desktop-app-logo-started.png") {
    Copy-Item "$env:USERPROFILE\Downloads\icons\desktop-app-logo-started.png" -Destination "src\main\resources\" -Force
}

if (Test-Path "$env:USERPROFILE\Downloads\icon-app-desktop-1044.ico") {
    Copy-Item "$env:USERPROFILE\Downloads\icon-app-desktop-1044.ico" -Destination "src\main\resources\icon-app-desktop.ico" -Force
} elseif (Test-Path "$env:USERPROFILE\Downloads\icons\icon-app-desktop-1044.ico") {
    Copy-Item "$env:USERPROFILE\Downloads\icons\icon-app-desktop-1044.ico" -Destination "src\main\resources\icon-app-desktop.ico" -Force
} elseif (Test-Path "$env:USERPROFILE\Downloads\icon-app-desktop-hd.ico") {
    Copy-Item "$env:USERPROFILE\Downloads\icon-app-desktop-hd.ico" -Destination "src\main\resources\icon-app-desktop.ico" -Force
} elseif (Test-Path "$env:USERPROFILE\Downloads\icons\icon-app-desktop-hd.ico") {
    Copy-Item "$env:USERPROFILE\Downloads\icons\icon-app-desktop-hd.ico" -Destination "src\main\resources\icon-app-desktop.ico" -Force
} elseif (Test-Path "$env:USERPROFILE\Downloads\icon-app-desktop.ico") {
    Copy-Item "$env:USERPROFILE\Downloads\icon-app-desktop.ico" -Destination "src\main\resources\icon-app-desktop.ico" -Force
} elseif (Test-Path "$env:USERPROFILE\Downloads\icons\icon-app-desktop.ico") {
    Copy-Item "$env:USERPROFILE\Downloads\icons\icon-app-desktop.ico" -Destination "src\main\resources\icon-app-desktop.ico" -Force
}

if (Test-Path "$env:USERPROFILE\Downloads\icon-app-desktop.png") {
    Copy-Item "$env:USERPROFILE\Downloads\icon-app-desktop.png" -Destination "src\main\resources\" -Force
} elseif (Test-Path "$env:USERPROFILE\Downloads\icons\icon-app-desktop.png") {
    Copy-Item "$env:USERPROFILE\Downloads\icons\icon-app-desktop.png" -Destination "src\main\resources\" -Force
}

# 3. Clean and Compile
Write-Host ""
Write-Host "[3/5] Compiling source files..." -ForegroundColor Yellow

# Clean target folders (keep target/wix so we don't redownload Wix Toolset)
if (Test-Path target/classes) { Remove-Item target/classes -Recurse -Force }
if (Test-Path target/libs) { Remove-Item target/libs -Recurse -Force }
if (Test-Path target/dist) { Remove-Item target/dist -Recurse -Force }

New-Item -ItemType Directory -Path target/classes -Force | Out-Null
New-Item -ItemType Directory -Path target/libs -Force | Out-Null

$classpath = "$jnaJar;$jnaPlatformJar;$gsonJar"
$sources = Get-ChildItem -Path src/main/java -Filter *.java -Recurse | Resolve-Path | ForEach-Object { $_.ProviderPath }

Write-Host "Compiling $($sources.Count) Java classes..." -ForegroundColor Gray
& $javacPath -cp $classpath -d target/classes -encoding UTF-8 $sources
Write-Host "Compilation complete." -ForegroundColor Green

# Copy resource files to target/classes
if (Test-Path src/main/resources) {
    Copy-Item src/main/resources\* -Destination target/classes/ -Force -ErrorAction SilentlyContinue
}

# 4. Package JAR and copy dependencies
Write-Host ""
Write-Host "[4/5] Packaging classes and dependencies..." -ForegroundColor Yellow

# Create MANIFEST.MF
$manifestContent = "Manifest-Version: 1.0`r`nMain-Class: io.github.zakyislm.kiroku.Main`r`n`r`n"
$manifestContent | Out-File -FilePath target/MANIFEST.MF -Encoding ascii

# Build application JAR
& $jarPath cfm target/Kiroku-desktop.jar target/MANIFEST.MF -C target/classes .

# Copy JARs to target/libs for jpackage packaging
Copy-Item $jnaJar -Destination target/libs/
Copy-Item $jnaPlatformJar -Destination target/libs/
Copy-Item $gsonJar -Destination target/libs/
Copy-Item target/Kiroku-desktop.jar -Destination target/libs/

Write-Host "Packaging complete." -ForegroundColor Green

# 5. Run jpackage
Write-Host ""
Write-Host "[5/5] Building standalone Windows package ($Type)..." -ForegroundColor Yellow

if ($Type -eq "run") {
    Write-Host "[5/5] Launching Kiroku Desktop..." -ForegroundColor Yellow
    $javaPath = Join-Path $binDir "java.exe"
    Write-Host "Starting application..." -ForegroundColor Green
    & $javaPath -cp "target/classes;$classpath" io.github.zakyislm.kiroku.Main
} elseif ($Type -eq "jar") {
    Write-Host ""
    Write-Host "====================================================" -ForegroundColor Green
    Write-Host "               JAR BUILD SUCCESSFUL!                " -ForegroundColor Green
    Write-Host "====================================================" -ForegroundColor Green
    Write-Host "JAR File generated at: target\Kiroku-desktop.jar" -ForegroundColor Green
    Write-Host "====================================================" -ForegroundColor Green
} elseif ($Type -eq "msi" -or $Type -eq "exe") {
    # Check if Wix is already downloaded
    $wixDir = Join-Path $PWD "target\wix"
    $wixCandle = Join-Path $wixDir "candle.exe"
    if (-not (Test-Path $wixCandle)) {
        Write-Host "Wix Toolset binaries not found locally. Downloading Wix Toolset v3..." -ForegroundColor Cyan
        $wixUrl = "https://github.com/wixtoolset/wix3/releases/download/wix3112rtm/wix311-binaries.zip"
        $wixZip = Join-Path $PWD "target\wix311-binaries.zip"
        if (-not (Test-Path "target")) { New-Item -ItemType Directory -Path target -Force | Out-Null }
        
        Write-Host "Downloading Wix from GitHub..." -ForegroundColor Gray
        Invoke-WebRequest -Uri $wixUrl -OutFile $wixZip -UseBasicParsing
        
        Write-Host "Extracting Wix binaries..." -ForegroundColor Gray
        Expand-Archive -Path $wixZip -DestinationPath $wixDir -Force
        Write-Host "Wix Toolset set up successfully." -ForegroundColor Green
    }
    
    # Add Wix to PATH for this process session
    $env:PATH += ";$wixDir"
    
    Write-Host "Running jpackage to build Windows installer ($Type)... (this may take a few seconds)" -ForegroundColor Gray
    & $jpackagePath `
      --name Kiroku `
      --input target/libs `
      --main-jar Kiroku-desktop.jar `
      --main-class io.github.zakyislm.kiroku.Main `
      --type $Type `
      --win-dir-chooser `
      --win-shortcut `
      --win-menu `
      --win-menu-group "Kiroku" `
      --icon src/main/resources/icon-app-desktop.ico `
      --dest target/dist
      
    Write-Host ""
    Write-Host "====================================================" -ForegroundColor Green
    Write-Host "               BUILD SUCCESSFUL!                    " -ForegroundColor Green
    Write-Host "====================================================" -ForegroundColor Green
    Write-Host "Installer generated at: target\dist\Kiroku-1.0.$Type" -ForegroundColor Green
    Write-Host "====================================================" -ForegroundColor Green
} else {
    Write-Host "Running jpackage to build self-contained app image... (this may take a few seconds)" -ForegroundColor Gray
    & $jpackagePath `
      --name Kiroku `
      --input target/libs `
      --main-jar Kiroku-desktop.jar `
      --main-class io.github.zakyislm.kiroku.Main `
      --type app-image `
      --icon src/main/resources/icon-app-desktop.ico `
      --dest target/dist
      
    Write-Host ""
    Write-Host "====================================================" -ForegroundColor Green
    Write-Host "               BUILD SUCCESSFUL!                    " -ForegroundColor Green
    Write-Host "====================================================" -ForegroundColor Green
    Write-Host "Executable generated at: target\dist\Kiroku\Kiroku.exe" -ForegroundColor Green
    Write-Host "You can share the entire 'target\dist\Kiroku' folder with others." -ForegroundColor Gray
    Write-Host "====================================================" -ForegroundColor Green
}
