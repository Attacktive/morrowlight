$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$properties = Get-Content (Join-Path $root 'gradle/wrapper/gradle-wrapper.properties')
$version = [regex]::Match(($properties -match '^distributionUrl=')[0], 'gradle-([0-9.]+)-bin.zip').Groups[1].Value
$expected = (($properties -match '^distributionSha256Sum=')[0] -split '=', 2)[1]
if (!$version -or !$expected) { throw 'Missing pinned Gradle version or checksum.' }
$homeDir = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $HOME '.gradle' }

$cache = Join-Path $homeDir "morrowlight/$version"
$bin = Join-Path $cache "gradle-$version/bin/gradle.bat"
if (!(Test-Path $bin)) {
	$temp = Join-Path $cache ([guid]::NewGuid().ToString())
	New-Item -ItemType Directory -Force $temp | Out-Null
	try {
		$zip = Join-Path $temp 'gradle.zip'
		Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$version-bin.zip" -OutFile $zip
		if ((Get-FileHash $zip -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected) { throw 'Gradle checksum mismatch; refusing to execute.' }
		Expand-Archive $zip -DestinationPath $temp
		if (!(Test-Path $bin)) { Move-Item (Join-Path $temp "gradle-$version") $cache }
	} finally { Remove-Item -Recurse -Force $temp -ErrorAction SilentlyContinue }
}

& $bin -p $root @args
exit $LASTEXITCODE
