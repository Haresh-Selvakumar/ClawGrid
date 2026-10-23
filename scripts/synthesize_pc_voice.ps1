param([string]$OutputDirectory = 'benchmarks/review/pc_voice/fixtures')
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Speech
$outputPath = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $OutputDirectory))
[System.IO.Directory]::CreateDirectory($outputPath) | Out-Null
$cases = @(
    @{file='light_on.wav';phrase='bedroom light on';expected=@{room='bedroom';device='light';action='ON'}},
    @{file='lights_off.wav';phrase='bedroom lights off';expected=@{room='bedroom';device='light';action='OFF'}},
    @{file='fan_on.wav';phrase='bedroom fan on';expected=@{room='bedroom';device='fan';action='ON'}},
    @{file='fan_off.wav';phrase='bedroom fan off';expected=@{room='bedroom';device='fan';action='OFF'}},
    @{file='negative_not.wav';phrase='do not turn on bedroom light';expected=$null},
    @{file='negative_dont.wav';phrase="don't turn bedroom light on";expected=$null},
    @{file='unrelated.wav';phrase='the weather is nice today';expected=$null}
)
$synthesizer = New-Object System.Speech.Synthesis.SpeechSynthesizer
$format = New-Object System.Speech.AudioFormat.SpeechAudioFormatInfo(16000, [System.Speech.AudioFormat.AudioBitsPerSample]::Sixteen, [System.Speech.AudioFormat.AudioChannel]::Mono)
try {
    foreach ($case in $cases) {
        $synthesizer.SetOutputToWaveFile((Join-Path $outputPath $case.file), $format)
        $synthesizer.Speak($case.phrase)
        $synthesizer.SetOutputToNull()
    }
    $cases | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $outputPath 'cases.json') -Encoding ASCII
    Write-Output 'Generated 7 SYNTHETIC fixtures; no microphone recorded or hardware controlled.'
} finally { $synthesizer.Dispose() }
