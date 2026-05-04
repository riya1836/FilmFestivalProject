param(
    [switch]$SkipCompile,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$AppArgs
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

function Get-ConnectorJar {
    $candidateFiles = New-Object System.Collections.Generic.List[string]

    if ($env:MYSQL_CONNECTOR_JAR -and (Test-Path $env:MYSQL_CONNECTOR_JAR)) {
        $candidateFiles.Add($env:MYSQL_CONNECTOR_JAR)
    }

    $searchPatterns = @(
        (Join-Path $projectRoot "lib\mysql-connector-j-*.jar"),
        (Join-Path $projectRoot "lib\mysql-connector-java-*.jar"),
        (Join-Path $env:USERPROFILE "Downloads\mysql-connector-j-*.jar"),
        (Join-Path $env:USERPROFILE "Downloads\mysql-connector-java-*.jar")
    )

    foreach ($pattern in $searchPatterns) {
        $matches = Get-ChildItem -Path $pattern -File -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending
        foreach ($match in $matches) {
            $candidateFiles.Add($match.FullName)
        }
    }

    if ($candidateFiles.Count -eq 0) {
        return $null
    }

    return $candidateFiles[0]
}

function Ensure-DbProperties {
    $dbProperties = Join-Path $projectRoot "db.properties"
    if (-not (Test-Path $dbProperties)) {
        Copy-Item (Join-Path $projectRoot "db.properties.example") $dbProperties
        Write-Host "Created db.properties from db.properties.example." -ForegroundColor Yellow
        Write-Host "Update db.user and db.password in db.properties, then run this script again." -ForegroundColor Yellow
        return $false
    }

    $content = Get-Content $dbProperties -Raw
    if ($content -match "your_mysql_username" -or $content -match "your_mysql_password") {
        Write-Host "db.properties still contains placeholder values." -ForegroundColor Yellow
        Write-Host "Update db.user and db.password in db.properties, then run this script again." -ForegroundColor Yellow
        return $false
    }

    return $true
}

if (-not (Ensure-DbProperties)) {
    exit 1
}

$connectorJar = Get-ConnectorJar
if (-not $connectorJar) {
    Write-Host "MySQL JDBC driver not found." -ForegroundColor Red
    Write-Host "Put a mysql-connector JAR in .\lib, place it in Downloads, or set MYSQL_CONNECTOR_JAR." -ForegroundColor Yellow
    exit 1
}

Write-Host "Using JDBC driver: $connectorJar" -ForegroundColor Cyan

if (-not $SkipCompile) {
    $sourceFiles = Get-ChildItem -Path (Join-Path $projectRoot "src") -Recurse -Filter *.java |
        Select-Object -ExpandProperty FullName

    if (-not $sourceFiles) {
        Write-Host "No Java source files were found under src." -ForegroundColor Red
        exit 1
    }

    if (-not (Test-Path (Join-Path $projectRoot "out"))) {
        New-Item -ItemType Directory -Path (Join-Path $projectRoot "out") | Out-Null
    }

    Write-Host "Compiling project..." -ForegroundColor Cyan
    & javac -d (Join-Path $projectRoot "out") $sourceFiles
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

Write-Host "Starting application..." -ForegroundColor Cyan
& java -cp "$projectRoot\out;$connectorJar" app.Main @AppArgs
exit $LASTEXITCODE
