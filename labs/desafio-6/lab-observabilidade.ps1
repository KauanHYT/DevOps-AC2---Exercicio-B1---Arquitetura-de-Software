<#
  Mini-lab do Desafio 6 (ADR-006): as métricas no Prometheus comprovam as decisões anteriores.

  Cenários (cada um provoca uma falha e confere, NO PROMETHEUS, a métrica que deve denunciá-la):
    1. IA fora (ADR-003)              -> gamificacao_recomendacoes_total{origem="PADRAO"} sobe
    2. RabbitMQ fora (ADR-004)        -> up{job="rabbitmq"} = 0 e gamificacao_outbox_pendentes sobe e volta a 0
    3. Consumidor fora (ADR-004)      -> rabbitmq_queue_messages_ready sobe e volta a 0
    4. Durante tudo isso (ASR-09)     -> nenhuma conclusão de curso com status diferente de 200; latência p95

  Gera: docs/arquitetura/evidencias/desafio-6/lab-resultado.txt
  Depois, tire o print do Grafana (http://localhost:3000, últimos 15 minutos).

  Uso, na raiz do repositório, com o Docker Desktop aberto:
    powershell -ExecutionPolicy Bypass -File labs/desafio-6/lab-observabilidade.ps1
#>

$ErrorActionPreference = 'Continue'
$raiz = (Get-Item $PSScriptRoot).Parent.Parent.FullName
Set-Location $raiz

$api = 'http://localhost:8080'
$prometheus = 'http://localhost:9090'
$saida = [IO.Path]::Combine($raiz, 'docs', 'arquitetura', 'evidencias', 'desafio-6', 'lab-resultado.txt')
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
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            $texto = $_.ErrorDetails.Message
        } else {
            $status = 0
            $texto = $_.Exception.Message
        }
    }
    $json = $null
    if ($texto) { try { $json = $texto | ConvertFrom-Json } catch { } }
    return [pscustomobject]@{ Status = $status; Json = $json }
}

function Aguardar([string]$url, [string]$nome) {
    for ($i = 0; $i -lt 90; $i++) {
        if ((Chamar 'GET' $url).Status -eq 200) { return }
        Start-Sleep -Seconds 2
    }
    Registrar "ERRO: $nome não respondeu em 3 minutos. Veja: docker compose logs $nome"
    exit 1
}

# Valor de uma consulta PromQL (primeira série), ou $null se não houver dado
function Prometheus([string]$consulta) {
    $url = "$prometheus/api/v1/query?query=" + [uri]::EscapeDataString($consulta)
    try {
        $r = Invoke-RestMethod -Uri $url -TimeoutSec 10
        if ($r.data.result.Count -gt 0) { return [double]$r.data.result[0].value[1] }
    } catch { }
    return $null
}

function ValorOuZero($valor) {
    if ($null -eq $valor) { return 0 }
    return $valor
}

# Espera até a consulta satisfazer a condição; devolve os segundos ou -1
function EsperarMetrica([string]$consulta, [scriptblock]$condicao, [int]$segundos) {
    $cronometro = [Diagnostics.Stopwatch]::StartNew()
    while ($cronometro.Elapsed.TotalSeconds -lt $segundos) {
        $v = Prometheus $consulta
        if (& $condicao $v) { return [math]::Round($cronometro.Elapsed.TotalSeconds, 1) }
        Start-Sleep -Seconds 2
    }
    return -1
}

function NovoAluno {
    return [long](Chamar 'POST' "$api/alunos" @{ plano = 'BASICO' }).Json.id
}

function ConcluirCursos([long]$id, [int]$quantidade) {
    $ok = 0
    for ($i = 0; $i -lt $quantidade; $i++) {
        if ((Chamar 'PUT' "$api/alunos/$id/concluir-curso").Status -eq 200) { $ok++ }
    }
    return $ok
}

$FILTRO_CONCLUSAO = 'uri="/alunos/{id}/concluir-curso"'

Registrar "# Mini-lab Desafio 6 - métricas que comprovam as decisões arquiteturais"
Registrar ("# Executado em {0}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
Registrar ""

Write-Host "Subindo o ambiente (Core, banco, RabbitMQ, serviços Python, Prometheus e Grafana)..."
docker compose up -d --build postgres rabbitmq notificacao-service ia-service backend prometheus grafana
Aguardar "$api/alunos" 'backend'
Aguardar "$prometheus/-/ready" 'prometheus'
Write-Host "Esperando o Prometheus coletar os dois alvos..."
EsperarMetrica 'sum(up{job=~"core|rabbitmq"})' { param($v) $v -eq 2 } 90 | Out-Null

# ---------------------------------------------------------------- cenário 0
Registrar "## Cenário 0 - Ambiente observado"
Verificar 'Prometheus lê o Core (up{job="core"} = 1)' ((Prometheus 'up{job="core"}') -eq 1)
Verificar 'Prometheus lê o RabbitMQ (up{job="rabbitmq"} = 1)' ((Prometheus 'up{job="rabbitmq"}') -eq 1)
Registrar ""

# ---------------------------------------------------------------- cenário 1
Registrar "## Cenário 1 - IA fora do ar (ADR-003 / ASR-07)"
$aluno = NovoAluno
1..5 | ForEach-Object { Chamar 'GET' "$api/alunos/$aluno/recomendacoes" | Out-Null }
Start-Sleep -Seconds 8
$padraoAntes = ValorOuZero (Prometheus 'sum(gamificacao_recomendacoes_total{origem="PADRAO"})')
Registrar "   > docker compose stop ia-service"
docker compose stop ia-service
$respostas = 1..10 | ForEach-Object { Chamar 'GET' "$api/alunos/$aluno/recomendacoes" }
$padrao = @($respostas | Where-Object { $_.Status -eq 200 -and $_.Json.origem -eq 'PADRAO' }).Count
$conclusao = (Chamar 'PUT' "$api/alunos/$aluno/concluir-curso").Status
Registrar "   10 recomendações com a IA fora -> $padrao com HTTP 200 e origem=PADRAO | concluir-curso -> HTTP $conclusao"
$t1 = EsperarMetrica 'sum(gamificacao_recomendacoes_total{origem="PADRAO"})' { param($v) (ValorOuZero $v) -ge ($padraoAntes + 10) } 30
$padraoDepois = ValorOuZero (Prometheus 'sum(gamificacao_recomendacoes_total{origem="PADRAO"})')
Registrar "   Prometheus: gamificacao_recomendacoes_total{origem=PADRAO} $padraoAntes -> $padraoDepois"
Verificar 'as 10 recomendações responderam 200 com a trilha PADRAO' ($padrao -eq 10)
Verificar 'a queda da IA aparece no Prometheus (+10 em origem=PADRAO)' ($t1 -ge 0)
Verificar 'concluir-curso continua com 200 com a IA fora' ($conclusao -eq 200)
Registrar "   > docker compose start ia-service"
docker compose start ia-service | Out-Null
Registrar ""

# ---------------------------------------------------------------- cenário 2
Registrar "## Cenário 2 - RabbitMQ fora do ar (ADR-004 / ASR-09 e ASR-10)"
Registrar "   > docker compose stop rabbitmq"
docker compose stop rabbitmq
$tDown = EsperarMetrica 'up{job="rabbitmq"}' { param($v) $v -eq 0 } 30
$aluno2 = NovoAluno
$ok2 = ConcluirCursos $aluno2 12
Registrar "   12 conclusões com o broker fora -> $ok2 com HTTP 200"
$tPend = EsperarMetrica 'sum(gamificacao_outbox_pendentes)' { param($v) (ValorOuZero $v) -ge 1 } 30
$pendentes = ValorOuZero (Prometheus 'sum(gamificacao_outbox_pendentes)')
Registrar "   Prometheus: up{job=rabbitmq} = 0 em $tDown s | gamificacao_outbox_pendentes = $pendentes"
Verificar 'a queda do broker aparece no Prometheus (up{job="rabbitmq"} = 0)' ($tDown -ge 0)
Verificar 'as 12 conclusões responderam 200 com o broker fora' ($ok2 -eq 12)
Verificar 'o evento parado aparece no Prometheus (outbox pendente >= 1)' ($tPend -ge 0)
Registrar "   > docker compose start rabbitmq"
docker compose start rabbitmq | Out-Null
$tVolta = EsperarMetrica 'sum(gamificacao_outbox_pendentes)' { param($v) $v -eq 0 } 180
Registrar "   Prometheus: outbox voltou a 0 em $tVolta s depois de religar o broker"
Verificar 'o outbox esvazia sozinho quando o broker volta (nada se perdeu)' ($tVolta -ge 0)
Registrar ""

# ---------------------------------------------------------------- cenário 3
Registrar "## Cenário 3 - Consumidor fora do ar (ADR-004 / ASR-10)"
EsperarMetrica 'up{job="rabbitmq"}' { param($v) $v -eq 1 } 60 | Out-Null
Registrar "   > docker compose stop notificacao-service"
docker compose stop notificacao-service
$aluno3 = NovoAluno
$ok3 = ConcluirCursos $aluno3 12
$tFila = EsperarMetrica 'sum(rabbitmq_queue_messages_ready)' { param($v) (ValorOuZero $v) -ge 1 } 60
$fila = ValorOuZero (Prometheus 'sum(rabbitmq_queue_messages_ready)')
Registrar "   12 conclusões com o consumidor fora -> $ok3 com HTTP 200 | Prometheus: mensagens na fila = $fila"
Verificar 'a mensagem esperando aparece no Prometheus (fila >= 1)' ($tFila -ge 0)
Registrar "   > docker compose start notificacao-service"
docker compose start notificacao-service | Out-Null
$tFilaVolta = EsperarMetrica 'sum(rabbitmq_queue_messages_ready)' { param($v) $v -eq 0 } 120
Registrar "   Prometheus: fila voltou a 0 em $tFilaVolta s depois de religar o consumidor"
Verificar 'a fila esvazia quando o consumidor volta' ($tFilaVolta -ge 0)
Registrar ""

# ---------------------------------------------------------------- cenário 4
Registrar "## Cenário 4 - O fluxo essencial durante todas as falhas (ASR-09)"
Start-Sleep -Seconds 6
$erros = ValorOuZero (Prometheus "sum(increase(http_server_requests_seconds_count{$FILTRO_CONCLUSAO,status!=`"200`"}[15m]))")
$total = ValorOuZero (Prometheus "sum(increase(http_server_requests_seconds_count{$FILTRO_CONCLUSAO}[15m]))")
$p95 = Prometheus "histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{$FILTRO_CONCLUSAO}[15m])))"
$p95ms = if ($null -eq $p95) { -1 } else { [math]::Round($p95 * 1000) }
Registrar ("   Prometheus (últimos 15 min): ~{0:N0} conclusões de curso, ~{1:N0} com status diferente de 200, p95 = {2} ms" -f $total, $erros, $p95ms)
Verificar 'nenhuma conclusão de curso falhou enquanto IA, broker e consumidor caíam' ($total -gt 0 -and $erros -lt 0.5)
Verificar 'latência p95 da conclusão de curso abaixo de 1 s' ($p95ms -ge 0 -and $p95ms -lt 1000)

$passaram = @($verificacoes | Where-Object { $_.Passou }).Count
Registrar ""
Registrar ("## Resultado: {0} de {1} verificações passaram" -f $passaram, $verificacoes.Count)

[IO.File]::WriteAllLines($saida, $linhas, (New-Object Text.UTF8Encoding $false))
Write-Host ""
Write-Host "Evidência salva em $saida"
Write-Host "Agora tire o print do Grafana: http://localhost:3000 (intervalo: Last 15 minutes)."
