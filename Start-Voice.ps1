param([string]$SerialPort = 'COM7', [int]$Port = 8770)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$voicePython = Join-Path $PSScriptRoot '.voice-env\Scripts\python.exe'
$voiceModel = Join-Path $PSScriptRoot '.speech_models\vosk-model-small-en-us-0.15'
if (-not (Test-Path -LiteralPath $voicePython)) {
    throw 'Voice environment missing. Create .voice-env and install requirements-voice.txt first.'
}
if (-not (Test-Path -LiteralPath (Join-Path $voiceModel 'am\final.mdl'))) {
    throw 'Voice model missing. Run .voice-env\Scripts\python.exe scripts\setup_offline_speech.py once.'
}
$credentials = Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'local_credentials.json') | ConvertFrom-Json
try {
    $existingVoice = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/local/speech/status" -Headers @{'X-ClawGrid-Token'=$credentials.api_token} -TimeoutSec 1
    if ($existingVoice.ready) {
        Write-Output "Offline voice is already running at http://127.0.0.1:$Port/voice"
        exit 0
    }
} catch { }
Write-Output "Open http://127.0.0.1:$Port/voice; microphone starts only when you click Start listening."
$bridgeArguments = @('scripts/browser_bridge.py','--serial',$SerialPort,'--port',"$Port",'--offline-speech',$voiceModel)
$cloudConfig = Join-Path $PSScriptRoot 'supabase.local.json'
if (Test-Path -LiteralPath $cloudConfig) {
    $cloudSettings = Get-Content -Raw -LiteralPath $cloudConfig | ConvertFrom-Json
    if ($cloudSettings.enabled) { $bridgeArguments += @('--supabase-config',$cloudConfig) }
}
& $voicePython @bridgeArguments
exit $LASTEXITCODE
