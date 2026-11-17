# Exporta docs/arquitetura-devops.svg para docs/arquitetura-devops.png com o Microsoft Edge (headless).
# Uso (na raiz do repositório): powershell -ExecutionPolicy Bypass -File docs\ferramentas\exportar-arquitetura.ps1
$ErrorActionPreference = 'Stop'
$raiz = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$svg = Join-Path $raiz 'docs\arquitetura-devops.svg'
$png = Join-Path $raiz 'docs\arquitetura-devops.png'
$edge = @(
  "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe",
  "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $edge) { throw 'Microsoft Edge não encontrado.' }

# Perfil separado: o Edge headless não interfere no navegador aberto.
$perfil = Join-Path $env:TEMP 'edge-captura-zela'
$url = 'file:///' + ($svg.Replace('\', '/'))
Remove-Item $png -ErrorAction SilentlyContinue
$argumentos = @('--headless=new', '--disable-gpu', '--hide-scrollbars', '--no-first-run',
  "--user-data-dir=$perfil", '--force-device-scale-factor=1.5', '--window-size=1600,1010',
  "--screenshot=$png", $url)
Start-Process -FilePath $edge -ArgumentList $argumentos -Wait
if (-not (Test-Path $png)) { throw 'O PNG não foi gerado.' }
Write-Output "Gerado: $png ($((Get-Item $png).Length) bytes)"
