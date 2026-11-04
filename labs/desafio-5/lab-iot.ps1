<#
  Mini-lab do Desafio 5 (ADR-005): dispositivos -> MQTT -> iot-gateway -> Core.

  Cenários:
    1. Fluxo normal             -> presença, início e conclusão chegam ao Core; payload pequeno
    2. Retransmissão (QoS 1)    -> a mesma conclusão enviada 3 vezes conta 1 curso
    3. Dispositivo offline      -> 10 conclusões guardadas no aparelho chegam ao reconectar; vira PREMIUM e o D4 notifica
    4. Gateway fora do ar       -> o broker guarda as mensagens (sessão persistente) e entrega quando o gateway volta
    5. Core fora do ar          -> o gateway segura a mensagem e tenta de novo até o Core voltar

  Gera: docs/arquitetura/evidencias/desafio-5/lab-resultado.txt

  Uso, na raiz do repositório, com o Docker Desktop aberto:
    powershell -ExecutionPolicy Bypass -File labs/desafio-5/lab-iot.ps1
#>

$ErrorActionPreference = 'Continue'
# a saída do docker (simulador e logs do gateway) vem em UTF-8
try { [Console]::OutputEncoding = New-Object Text.UTF8Encoding $false } catch { }
$raiz = (Get-Item $PSScriptRoot).Parent.Parent.FullName
Set-Location $raiz

$api = 'http://localhost:8080'
$saida = [IO.Path]::Combine($raiz, 'docs', 'arquitetura', 'evidencias', 'desafio-5', 'lab-resultado.txt')
New-Item -ItemType Directory -Force -Path (Split-Path $saida) | Out-Null

$linhas = New-Object System.Collections.Generic.List[string]
$verificacoes = New-Object System.Collections.Generic.List[object]

function Registrar([string]$texto) {
    Write-Host $texto
    $linhas.Add($texto)
}

function Verificar([string]$descricao, [bool]$passou) {
    $verificacoes.Add([pscustomobject]@{ Descricao = $descricao; Passou = $passou })
    if ($passou) { Registrar "   [OK]     $descricao" } else { Registrar "   [FALHOU] $descricao" }
}

function Chamar([string]$metodo, [string]$url, $corpo = $null) {
    $params = @{ Method = $metodo; Uri = $url; UseBasicParsing = $true; TimeoutSec = 30 }
    if ($null -ne $corpo) {
        $params.Body = [Text.Encoding]::UTF8.GetBytes(($corpo | ConvertTo-Json -Compress))
        $params.ContentType = 'application/json; charset=utf-8'
    }
    try {
        $resposta = Invoke-WebRequest @params
        $status = [int]$resposta.StatusCode
        $texto = [Text.Encoding]::UTF8.GetString($resposta.RawContentStream.ToArray())
    } catch {
        $status = 0
        $texto = $null
        if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode }
    }
    $json = $null
    if ($texto) { try { $json = $texto | ConvertFrom-Json } catch { } }
    return [pscustomobject]@{ Status = $status; Json = $json }
}

function Aguardar([string]$url, [string]$nome, [int]$segundos = 180) {
    $cronometro = [Diagnostics.Stopwatch]::StartNew()
    while ($cronometro.Elapsed.TotalSeconds -lt $segundos) {
        if ((Chamar 'GET' $url).Status -eq 200) { return }
        Start-Sleep -Seconds 2
    }
    Registrar "ERRO: $nome não respondeu. Veja: docker compose logs $nome"
    exit 1
}

function NovoAluno {
    return [long](Chamar 'POST' "$api/alunos" @{ plano = 'BASICO' }).Json.id
}

function Aluno([long]$id) {
    return (Chamar 'GET' "$api/alunos/$id").Json
}

function QuantosEventos([long]$id) {
    $r = Chamar 'GET' "$api/iot/eventos?alunoId=$id"
    if ($r.Status -ne 200 -or $null -eq $r.Json) { return 0 }
    return @($r.Json).Count
}

# Espera até a condição ficar verdadeira; devolve os segundos ou -1
function Esperar([scriptblock]$condicao, [int]$segundos) {
    $cronometro = [Diagnostics.Stopwatch]::StartNew()
    while ($cronometro.Elapsed.TotalSeconds -lt $segundos) {
        if (& $condicao) { return [math]::Round($cronometro.Elapsed.TotalSeconds, 1) }
        Start-Sleep -Seconds 1
    }
    return -1
}

# Roda o simulador de dispositivo e devolve a saída dele
function Simular([string[]]$argumentos) {
    $texto = docker compose run --rm -T iot-simulador @argumentos 2>$null
    foreach ($l in @($texto)) { if ($l -match '^\[') { Registrar "   $l" } }
    return @($texto)
}

function LogDoGatewayDesde([string]$desde) {
    return @(docker compose logs --no-color --since $desde iot-gateway 2>$null)
}

function AgoraUtc { return (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ') }

Registrar "# Mini-lab Desafio 5 - eventos IoT: MQTT -> iot-gateway -> Core"
Registrar ("# Executado em {0}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
Registrar ""

Write-Host "Subindo Core, banco, RabbitMQ, notificação, Mosquitto e iot-gateway (a primeira vez demora)..."
docker compose up -d --build postgres rabbitmq notificacao-service backend mosquitto iot-gateway
docker compose --progress plain build iot-simulador 2>&1 | Out-Null
Aguardar "$api/alunos" 'backend'
$conectado = Esperar { (LogDoGatewayDesde '10m' | Where-Object { $_ -match 'conectado ao broker' }).Count -gt 0 } 90
if ($conectado -lt 0) { Registrar "AVISO: o iot-gateway não conectou ao Mosquitto (docker compose logs iot-gateway)" }

# ---------------------------------------------------------------- cenário 1
Registrar "## Cenário 1 - Fluxo normal: presença, início e conclusão"
$a = NovoAluno
$saida1 = Simular @('--aluno', "$a", '--presenca', '--inicio', '--conclusoes', '1')
$bytes = -1
foreach ($l in $saida1) { if ($l -match 'de (\d+) bytes$') { $bytes = [int]$Matches[1] } }
$t1 = Esperar { (QuantosEventos $a) -ge 3 } 30
$al = Aluno $a
Registrar ("   Core: {0} eventos registrados para o aluno {1} em {2} s | cursosConcluidos={3}" -f (QuantosEventos $a), $a, $t1, $al.cursosConcluidos)
Verificar 'os 3 eventos (presença, início, conclusão) chegaram ao Core pelo MQTT' ($t1 -ge 0)
Verificar 'só a conclusão somou curso (cursosConcluidos = 1)' ($al.cursosConcluidos -eq 1)
Verificar "payload do dispositivo pequeno ($bytes bytes por evento, abaixo de 100)" ($bytes -gt 0 -and $bytes -lt 100)
Registrar ""

# ---------------------------------------------------------------- cenário 2
Registrar "## Cenário 2 - Retransmissão: a mesma conclusão enviada 3 vezes (QoS 1 = pelo menos uma vez)"
$desde = AgoraUtc
Simular @('--aluno', "$a", '--conclusoes', '1', '--repetir', '3') | Out-Null
Esperar { (QuantosEventos $a) -ge 4 } 30 | Out-Null
Start-Sleep -Seconds 3
$al = Aluno $a
$duplicados = @(LogDoGatewayDesde $desde | Where-Object { $_ -match '-> DUPLICADO' }).Count
Registrar ("   Core: cursosConcluidos={0} | gateway: {1} entrega(s) respondida(s) como DUPLICADO" -f $al.cursosConcluidos, $duplicados)
Verificar '3 envios do mesmo evento somaram 1 curso (cursosConcluidos = 2)' ($al.cursosConcluidos -eq 2)
Verificar 'as 2 repetições foram reconhecidas como DUPLICADO pelo Core' ($duplicados -ge 2)
Registrar ""

# ---------------------------------------------------------------- cenário 3
Registrar "## Cenário 3 - Dispositivo offline: 10 conclusões guardadas no aparelho e enviadas ao reconectar"
Simular @('--aluno', "$a", '--conclusoes', '10', '--offline') | Out-Null
$t3 = Esperar { (Aluno $a).cursosConcluidos -ge 12 } 60
$al = Aluno $a
Registrar ("   Core: cursosConcluidos={0} plano={1} voucher={2} em {3} s" -f $al.cursosConcluidos, $al.plano, $al.possuiVoucher, $t3)
Verificar 'as 10 conclusões chegaram e o aluno virou PREMIUM com 12 cursos' ($al.cursosConcluidos -eq 12 -and $al.plano -eq 'PREMIUM')
$t3n = Esperar { (Aluno $a).notificadoMelhoriaPlano } 90
Registrar "   D4: notificação da promoção entregue $t3n s depois (outbox -> RabbitMQ -> notificacao-service)"
Verificar 'a promoção vinda do laboratório seguiu o fluxo do D4 (notificado=true)' ($t3n -ge 0)
Registrar ""

# ---------------------------------------------------------------- cenário 4
Registrar "## Cenário 4 - iot-gateway fora do ar: o broker guarda as mensagens"
$b = NovoAluno
Registrar "   > docker compose stop iot-gateway"
docker compose stop iot-gateway
Simular @('--aluno', "$b", '--conclusoes', '3') | Out-Null
Start-Sleep -Seconds 5
$antes = (Aluno $b).cursosConcluidos
Registrar "   Core, com o gateway fora: cursosConcluidos=$antes"
Registrar "   > docker compose start iot-gateway"
docker compose start iot-gateway | Out-Null
$t4 = Esperar { (Aluno $b).cursosConcluidos -ge 3 } 60
Registrar ("   Core, depois de religar: cursosConcluidos={0} em {1} s" -f (Aluno $b).cursosConcluidos, $t4)
Verificar 'com o gateway fora, nada chegou ao Core (e nada se perdeu)' ($antes -eq 0)
Verificar 'as 3 mensagens guardadas no broker chegaram quando o gateway voltou' ($t4 -ge 0 -and (Aluno $b).cursosConcluidos -eq 3)
Registrar ""

# ---------------------------------------------------------------- cenário 5
Registrar "## Cenário 5 - Core fora do ar: o gateway segura a mensagem e tenta de novo"
Registrar "   > docker compose stop backend"
docker compose stop backend
$desde = AgoraUtc
Simular @('--aluno', "$b", '--conclusoes', '1') | Out-Null
Start-Sleep -Seconds 5
$tentativas = @(LogDoGatewayDesde $desde | Where-Object { $_ -match 'nova tentativa' }).Count
Registrar "   gateway, com o Core fora: $tentativas aviso(s) de nova tentativa no log"
Registrar "   > docker compose start backend"
docker compose start backend | Out-Null
Aguardar "$api/alunos" 'backend'
$t5 = Esperar { (Aluno $b).cursosConcluidos -ge 4 } 90
Registrar ("   Core, depois de voltar: cursosConcluidos={0} em {1} s" -f (Aluno $b).cursosConcluidos, $t5)
Verificar 'o evento enviado com o Core fora foi entregue quando ele voltou' ($tentativas -ge 1 -and $t5 -ge 0 -and (Aluno $b).cursosConcluidos -eq 4)

$passaram = @($verificacoes | Where-Object { $_.Passou }).Count
Registrar ""
Registrar ("## Resultado: {0} de {1} verificações passaram" -f $passaram, $verificacoes.Count)

[IO.File]::WriteAllLines($saida, $linhas, (New-Object Text.UTF8Encoding $false))
Write-Host ""
Write-Host "Evidência salva em $saida"
