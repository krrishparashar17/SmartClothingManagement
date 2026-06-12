# PowerShell Build and Run Script for Smart Clothing Sales & Inventory Management System
# Compiles and runs the application without requiring a pre-installed Maven build tool.

$ErrorActionPreference = "Stop"

# Setup directories
$libDir = Join-Path $PSScriptRoot "lib"
$targetClassesDir = Join-Path $PSScriptRoot "target\classes"
$srcDir = Join-Path $PSScriptRoot "src\main\java"
$resourcesDir = Join-Path $PSScriptRoot "src\main\resources"

if (-not (Test-Path $libDir)) {
    New-Item -ItemType Directory -Path $libDir | Out-Null
}
if (-not (Test-Path $targetClassesDir)) {
    New-Item -ItemType Directory -Path $targetClassesDir -Force | Out-Null
}

# Dependency libraries URLs
$deps = @{
    "postgresql-42.7.3.jar"      = "https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.3/postgresql-42.7.3.jar"
    "flatlaf-3.5.1.jar"          = "https://repo1.maven.org/maven2/com/formdev/flatlaf/3.5.1/flatlaf-3.5.1.jar"
    "flatlaf-extras-3.5.1.jar"   = "https://repo1.maven.org/maven2/com/formdev/flatlaf-extras/3.5.1/flatlaf-extras-3.5.1.jar"
}

# Download missing dependency JARs
Write-Host "Checking dependency libraries..." -ForegroundColor Cyan
foreach ($jar in $deps.Keys) {
    $filePath = Join-Path $libDir $jar
    if (-not (Test-Path $filePath)) {
        Write-Host "Downloading $jar..." -ForegroundColor Yellow
        Invoke-WebRequest -Uri $deps[$jar] -OutFile $filePath
    } else {
        Write-Host "$jar is already present." -ForegroundColor Green
    }
}

# Copy properties resources to classes target
if (Test-Path $resourcesDir) {
    Write-Host "Copying resources to build output..." -ForegroundColor Cyan
    Copy-Item -Path (Join-Path $resourcesDir "*") -Destination $targetClassesDir -Recurse -Force
}

# Build Classpath string
$cpList = @(
    $targetClassesDir,
    (Join-Path $libDir "postgresql-42.7.3.jar"),
    (Join-Path $libDir "flatlaf-3.5.1.jar"),
    (Join-Path $libDir "flatlaf-extras-3.5.1.jar")
)
$classpath = $cpList -join ";"

# Find all Java source files
Write-Host "Locating Java source files..." -ForegroundColor Cyan
$javaFiles = Get-ChildItem -Path $srcDir -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }

if ($javaFiles.Count -eq 0) {
    Write-Error "No Java source files found in $srcDir"
}

# Compile Java source files
Write-Host "Compiling project..." -ForegroundColor Cyan
& javac -d $targetClassesDir -cp $classpath $javaFiles
Write-Host "Compilation completed successfully!" -ForegroundColor Green

# Run application
Write-Host "Launching Smart Clothing Sales & Inventory Management System..." -ForegroundColor Cyan
& java -cp $classpath com.smartclothing.Main
