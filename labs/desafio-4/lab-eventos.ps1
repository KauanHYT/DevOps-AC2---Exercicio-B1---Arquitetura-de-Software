<#
  Mini-lab do Desafio 4 (ADR-004): interação síncrona x assíncrona na promoção para PREMIUM.

  Cenários:
    1. Assíncrono (decisão) com o notificacao-service DESLIGADO -> conclusão 200, mensagem espera na fila
    2. Assíncrono com o RabbitMQ DESLIGADO                     -> conclusão 200, evento espera no outbox
    3. Síncrono (alternativa A) com o notificacao-service DESLIGADO -> 12º curso 503 e conclusão desfeita

  Gera: docs/arquitetura/evidencias/desafio-4/lab-resultado.txt

  Uso, na raiz do repositório, com o Docker Desktop aberto:
    powershell -ExecutionPolicy Bypass -File labs/desafio-4/lab-eventos.ps1
#>

$ErrorActionPreference = 'Continue'
$raiz = (Get-Item $PSScriptRoot).Parent.Parent.FullName
Set-Location $raiz

$api = 'http://localhost:8080'
$notif = 'http://localhost:8001'
$saida = [IO.Path]::Combine($raiz, 'docs', 'arquitetura', 'evidencias', 'desafio-4', 'lab-resultado.txt')
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
    $cronometro = [Diagnostics.Stopwatch]::StartNew()
    try {
        $resposta = Invoke-WebRequest @params
        $status = [int]$resposta.StatusCode
        $texto = [Text.Encoding]::UTF8.GetString($resposta.RawContentStream.ToArray())
    } catch {
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            $texto = $_.ErrorDetails.Message
            if (-not $texto) {
                # no Windows PowerShell 5.1 o ErrorDetails às vezes vem vazio: lê o corpo direto da resposta
                try {
                    $leitor = New-Object IO.StreamReader($_.Exception.Response.GetResponseStream(), [Text.Encoding]::UTF8)
                    $texto = $leitor.ReadToEnd()
                } catch { }
            }
        } else {
            $status = 0
            $texto = $_.Exception.Message
        }
    }
    $cronometro.Stop()
    $json = $null
    if ($texto) { try { $json = $texto | ConvertFrom-Json } catch { } }
    return [pscustomobject]@{ Status = $status; Ms = $cronometro.ElapsedMilliseconds; Json = $json; Texto = $texto }
}

function Aguardar([string]$url, [string]$nome) {
    for ($i = 0; $i -lt 90; $i++) {
        if ((Chamar 'GET' $url).Status -eq 200) { return }
        Start-Sleep -Seconds 2
    }
    Registrar "ERRO: $nome não respondeu em 3 minutos. Veja: docker compose logs $nome"
    exit 1
}

function AguardarConsumidorConectado {
    for ($i = 0; $i -lt 60; $i++) {
        $h = Chamar 'GET' "$notif/health"
        if ($h.Status -eq 200 -and $h.Json.consumidorConectado) { return }
        Start-Sleep -Seconds 2
    }
    Registrar "AVISO: o notificacao-service não conectou ao RabbitMQ em 2 minutos (docker compose logs notificacao-service)"
}

function NovoAluno {
    return [long](Chamar 'POST' "$api/alunos" @{ plano = 'BASICO' }).Json.id
}

function ConcluirCursos([long]$id, [int]$quantidade) {
    $resultados = @()
    for ($i = 0; $i -lt $quantidade; $i++) {
        $resultados += Chamar 'PUT' "$api/alunos/$id/concluir-curso"
    }
    return ,$resultados
}

function Aluno([long]$id) {
    return (Chamar 'GET' "$api/alunos/$id").Json
}

function DescreverAluno([long]$id) {
    $a = Aluno $id
    Registrar ("   aluno {0}: cursosConcluidos={1} plano={2} voucher={3} notificado={4}" -f `
        $id, $a.cursosConcluidos, $a.plano, $a.possuiVoucher, $a.notificadoMelhoriaPlano)
    return $a
}

function AguardarNotificado([long]$id, [int]$segundos) {
    $cronometro = [Diagnostics.Stopwatch]::StartNew()
    while ($cronometro.Elapsed.TotalSeconds -lt $segundos) {
        $a = Aluno $id
        if ($a -and $a.notificadoMelhoriaPlano) { return [math]::Round($cronometro.Elapsed.TotalSeconds, 1) }
        Start-Sleep -Seconds 1
    }
    return -1
}

function MensagensNaFila {
    $saidaCmd = docker compose exec -T rabbitmq rabbitmqctl list_queues name messages --quiet 2>$null
    $linha = $saidaCmd | Where-Object { $_ -match 'notificacao\.aluno-promovido' } | Select-Object -First 1
    if ($linha) { return [int](($linha.Trim() -split '\s+')[1]) }
    return -1
}

function OutboxPendentes {
    $saidaCmd = docker compose exec -T postgres psql -U postgres -d ac1final -t -A `
        -c "select count(*) from outbox_eventos where publicado_em is null" 2>$null
    $numero = $saidaCmd | Where-Object { $_ -match '^\s*\d+\s*$' } | Select-Object -First 1
    if ($numero) { return [int]$numero.Trim() }
    return -1
}

Registrar "# Mini-lab Desafio 4 - interação síncrona x assíncrona na promoção para PREMIUM"
Registrar ("# Executado em {0}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
Registrar ""

Write-Host "Subindo postgres, rabbitmq, notificacao-service e backend (a primeira vez demora por causa do build)..."
Remove-Item Env:NOTIFICACAO_MODO -ErrorAction SilentlyContinue
docker compose up -d --build postgres rabbitmq notificacao-service backend
Aguardar "$api/alunos" 'backend'
Aguardar "$notif/health" 'notificacao-service'
AguardarConsumidorConectado

# ---------------------------------------------------------------- cenário 1
Registrar "## Cenário 1 - Assíncrono (ADR-004) com o notificacao-service DESLIGADO"
docker compose stop notificacao-service
$id1 = NovoAluno
$res1 = ConcluirCursos $id1 12
$ok1 = @($res1 | Where-Object { $_.Status -eq 200 }).Count
$msAssincrono = $res1[-1].Ms
Registrar ("   12 x PUT /alunos/$id1/concluir-curso -> {0} de 12 com HTTP 200 | 12º curso (promoção) em {1} ms" -f $ok1, $msAssincrono)
$a1 = DescreverAluno $id1
Start-Sleep -Seconds 5
$fila = MensagensNaFila
Registrar "   mensagens esperando na fila notificacao.aluno-promovido: $fila"
Verificar 'as 12 conclusões responderam 200 com o consumidor fora' ($ok1 -eq 12)
Verificar 'promoção imediata: PREMIUM + voucher na própria resposta' ($a1.plano -eq 'PREMIUM' -and $a1.possuiVoucher)
Verificar 'notificação ainda pendente (notificado=false)' (-not $a1.notificadoMelhoriaPlano)
Verificar 'mensagem guardada na fila enquanto o consumidor está fora' ($fila -ge 1)
Registrar "   > docker compose start notificacao-service"
docker compose start notificacao-service
$t1 = AguardarNotificado $id1 120
Registrar "   notificação entregue $t1 s depois de religar o consumidor"
$enviadas = @((Chamar 'GET' "$notif/notificacoes").Json | Where-Object { $_.alunoId -eq $id1 }).Count
Verificar 'consumidor voltou e entregou a notificação (notificado=true)' ($t1 -ge 0)
Verificar 'exatamente 1 notificação enviada para o aluno' ($enviadas -eq 1)
Registrar ""

# ---------------------------------------------------------------- cenário 2
Registrar "## Cenário 2 - Assíncrono com o RabbitMQ DESLIGADO"
docker compose stop rabbitmq
$id2 = NovoAluno
$res2 = ConcluirCursos $id2 12
$ok2 = @($res2 | Where-Object { $_.Status -eq 200 }).Count
Registrar ("   12 x PUT /alunos/$id2/concluir-curso -> {0} de 12 com HTTP 200 | 12º curso em {1} ms" -f $ok2, $res2[-1].Ms)
Start-Sleep -Seconds 5
$pendentes = OutboxPendentes
Registrar "   eventos pendentes no outbox (tabela outbox_eventos): $pendentes"
Verificar 'as 12 conclusões responderam 200 com o broker fora' ($ok2 -eq 12)
Verificar 'evento guardado no outbox enquanto o broker está fora' ($pendentes -ge 1)
Registrar "   > docker compose start rabbitmq"
docker compose start rabbitmq
$t2 = AguardarNotificado $id2 180
Registrar "   notificação entregue $t2 s depois de religar o broker"
$pendentesDepois = OutboxPendentes
Verificar 'broker voltou: o relay publicou e o consumidor entregou (notificado=true)' ($t2 -ge 0)
Verificar 'outbox esvaziou depois da publicação' ($pendentesDepois -eq 0)
Registrar ""

# ---------------------------------------------------------------- cenário 3
Registrar "## Cenário 3 - Alternativa A: síncrono (NOTIFICACAO_MODO=sincrono) com o notificacao-service DESLIGADO"
$env:NOTIFICACAO_MODO = 'sincrono'
docker compose up -d backend
Start-Sleep -Seconds 5
Aguardar "$api/alunos" 'backend'
docker compose stop notificacao-service
$id3 = NovoAluno
ConcluirCursos $id3 11 | Out-Null
$r12 = Chamar 'PUT' "$api/alunos/$id3/concluir-curso"
$mensagem = if ($r12.Json) { $r12.Json.mensagem } else { $r12.Texto }
Registrar ("   12º PUT /alunos/$id3/concluir-curso -> HTTP {0} em {1} ms | {2}" -f $r12.Status, $r12.Ms, $mensagem)
$a3 = DescreverAluno $id3
Verificar '12º curso falha com 503: a notificação virou dependência da conclusão' ($r12.Status -eq 503)
Verificar 'conclusão desfeita: o aluno continua com 11 cursos e BASICO' ($a3.cursosConcluidos -eq 11 -and $a3.plano -eq 'BASICO')
Registrar "   > docker compose start notificacao-service"
docker compose start notificacao-service
Aguardar "$notif/health" 'notificacao-service'
# o serviço já responde, mas a JVM guarda por ~10 s a falha de DNS do nome "notificacao-service":
# tenta de novo a cada 2 s e mede quanto tempo o Core leva para voltar a concluir o curso
$recuperacao = [Diagnostics.Stopwatch]::StartNew()
$tentativas = 0
do {
    $tentativas++
    $r12b = Chamar 'PUT' "$api/alunos/$id3/concluir-curso"
    if ($r12b.Status -eq 200) { break }
    Start-Sleep -Seconds 2
} while ($recuperacao.Elapsed.TotalSeconds -lt 40)
$segundosRecuperacao = [math]::Round($recuperacao.Elapsed.TotalSeconds, 1)
Registrar ("   12º curso de novo -> HTTP {0} em {1} ms | notificado={2} | {3} tentativa(s), {4} s depois de o serviço responder ao /health" -f `
    $r12b.Status, $r12b.Ms, $r12b.Json.notificadoMelhoriaPlano, $tentativas, $segundosRecuperacao)
Verificar 'com o consumidor de volta, o 12º curso passa (12 cursos, sem contar as tentativas que falharam) e já sai notificado' `
    ($r12b.Status -eq 200 -and $r12b.Json.cursosConcluidos -eq 12 -and $r12b.Json.notificadoMelhoriaPlano)
Registrar ""
Registrar ("Latência do 12º curso (promoção): assíncrono {0} ms | síncrono com consumidor no ar {1} ms" -f $msAssincrono, $r12b.Ms)

# ---------------------------------------------------------------- restaura
Remove-Item Env:NOTIFICACAO_MODO -ErrorAction SilentlyContinue
docker compose up -d backend | Out-Null

$passaram = @($verificacoes | Where-Object { $_.Passou }).Count
Registrar ""
Registrar ("## Resultado: {0} de {1} verificações passaram" -f $passaram, $verificacoes.Count)

[IO.File]::WriteAllLines($saida, $linhas, (New-Object Text.UTF8Encoding $false))
Write-Host ""
Write-Host "Evidência salva em $saida"