<#
  Mini-lab do Desafio 3 (ADR-003): Spring Boot consumindo o ia-service (FastAPI).

  Cenários:
    1. IA no ar       -> recomendação personalizada (origem IA) e assistente respondendo
    2. IA desligada   -> recomendação degradada (origem PADRAO), assistente 503, conclusão de curso OK
    3. IA lenta (3 s) -> recomendação estoura o timeout de 800 ms e degrada; o assistente (timeout 5 s) ainda responde

  Gera: docs/arquitetura/evidencias/desafio-3/lab-resultado.txt

  Uso, na raiz do repositório, com o Docker Desktop aberto:
    powershell -ExecutionPolicy Bypass -File labs/desafio-3/lab-ia.ps1
#>

$ErrorActionPreference = 'Continue'
$raiz = (Get-Item $PSScriptRoot).Parent.Parent.FullName
Set-Location $raiz

$api = 'http://localhost:8080'
$ia = 'http://localhost:8000'
$saida = [IO.Path]::Combine($raiz, 'docs', 'arquitetura', 'evidencias', 'desafio-3', 'lab-resultado.txt')
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

function Recomendar([long]$id) {
    $r = Chamar 'GET' "$api/alunos/$id/recomendacoes"
    $origem = if ($r.Json) { $r.Json.origem } else { '-' }
    $modelo = if ($r.Json) { $r.Json.modeloVersao } else { '-' }
    $item = if ($r.Json -and $r.Json.itens) { $r.Json.itens[0].cursoId } else { '-' }
    Registrar ("   GET  /alunos/$id/recomendacoes   -> HTTP {0} em {1,5} ms | origem={2} modelo={3} 1o item={4}" -f $r.Status, $r.Ms, $origem, $modelo, $item)
    return $r
}

function Perguntar([long]$id) {
    $r = Chamar 'POST' "$api/alunos/$id/assistente" @{ pergunta = 'Como eu ganho o voucher?' }
    $detalhe = if ($r.Status -eq 200) { 'fontes=' + ($r.Json.fontes -join ',') } elseif ($r.Json) { $r.Json.mensagem } else { $r.Texto }
    Registrar ("   POST /alunos/$id/assistente      -> HTTP {0} em {1,5} ms | {2}" -f $r.Status, $r.Ms, $detalhe)
    return $r
}

function ConcluirCurso([long]$id) {
    $r = Chamar 'PUT' "$api/alunos/$id/concluir-curso"
    $cursos = if ($r.Json) { $r.Json.cursosConcluidos } else { '-' }
    Registrar ("   PUT  /alunos/$id/concluir-curso  -> HTTP {0} em {1,5} ms | cursosConcluidos={2}" -f $r.Status, $r.Ms, $cursos)
    return $r
}

Registrar "# Mini-lab Desafio 3 - Core (Spring Boot) consumindo ia-service (FastAPI)"
Registrar ("# Executado em {0} | timeouts do Core: recomendação 800 ms, assistente 5000 ms" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
Registrar ""

Write-Host "Subindo postgres, backend e ia-service (a primeira vez demora por causa do build)..."
Remove-Item Env:IA_ATRASO_MS -ErrorAction SilentlyContinue
docker compose up -d --build postgres backend ia-service
Aguardar "$api/alunos" 'backend'
Aguardar "$ia/health" 'ia-service'

$aluno = Chamar 'POST' "$api/alunos" @{ plano = 'BASICO' }
$id = [long]$aluno.Json.id
for ($i = 0; $i -lt 5; $i++) { Chamar 'PUT' "$api/alunos/$id/concluir-curso" | Out-Null }
Registrar "Aluno de teste: id=$id, plano BASICO, 5 cursos concluídos"
Registrar ""

# ---------------------------------------------------------------- cenário 1
Registrar "## Cenário 1 - IA no ar"
$r1 = Recomendar $id
$a1 = Perguntar $id
Verificar 'recomendação vem do modelo (origem IA)' ($r1.Status -eq 200 -and $r1.Json.origem -eq 'IA')
Verificar 'assistente responde 200' ($a1.Status -eq 200)
Registrar ""

# ---------------------------------------------------------------- cenário 2
Registrar "## Cenário 2 - IA desligada (docker compose stop ia-service)"
docker compose stop ia-service
$r2 = Recomendar $id
$a2 = Perguntar $id
$c2 = ConcluirCurso $id
Verificar 'recomendação degrada para a trilha padrão (HTTP 200, origem PADRAO)' ($r2.Status -eq 200 -and $r2.Json.origem -eq 'PADRAO')
Verificar 'assistente responde 503 (indisponível, sem resposta inventada)' ($a2.Status -eq 503)
Verificar 'conclusão de curso continua funcionando (HTTP 200)' ($c2.Status -eq 200)
Verificar 'falha da IA detectada rápido (< 2000 ms)' ($r2.Ms -lt 2000)
Registrar ""

# ---------------------------------------------------------------- cenário 3
Registrar "## Cenário 3 - IA lenta (ATRASO_MS=3000, acima do timeout de 800 ms da recomendação)"
$env:IA_ATRASO_MS = '3000'
docker compose up -d ia-service
Aguardar "$ia/health" 'ia-service'
$r3 = Recomendar $id
$a3 = Perguntar $id
Verificar 'recomendação degrada (origem PADRAO) em vez de esperar os 3 s' ($r3.Status -eq 200 -and $r3.Json.origem -eq 'PADRAO')
Verificar 'tempo da recomendação limitado pelo timeout (< 2000 ms)' ($r3.Ms -lt 2000)
Verificar 'assistente, com timeout próprio de 5 s, ainda responde 200' ($a3.Status -eq 200)
Registrar ""

# ---------------------------------------------------------------- restaura
Remove-Item Env:IA_ATRASO_MS -ErrorAction SilentlyContinue
docker compose up -d ia-service | Out-Null

$passaram = @($verificacoes | Where-Object { $_.Passou }).Count
Registrar ("## Resultado: {0} de {1} verificações passaram" -f $passaram, $verificacoes.Count)

[IO.File]::WriteAllLines($saida, $linhas, (New-Object Text.UTF8Encoding $false))
Write-Host ""
Write-Host "Evidência salva em $saida"