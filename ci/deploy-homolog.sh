#!/bin/sh
# B1 / Desafio 6 (ADR-006): implanta UM componente na homologação e faz o smoke test.
# Uso: sh ci/deploy-homolog.sh <container> <imagem> <porta> <caminho-de-saude> [opções extras do docker run]
# Se o smoke test falhar, o script sai com erro e o Jenkins marca o deploy como falho.
set -eu

NOME="$1"
IMAGEM="$2"
PORTA="$3"
SAUDE="$4"
shift 4
REDE="${REDE_HOMOLOG:-b1-ci}"

docker rm -f "$NOME" >/dev/null 2>&1 || true
docker run -d --name "$NOME" --network "$REDE" "$@" "$IMAGEM" >/dev/null
echo "$NOME: implantado com $IMAGEM; esperando http://$NOME:$PORTA$SAUDE"

tentativa=0
while [ "$tentativa" -lt 60 ]; do
    if curl -fsS "http://$NOME:$PORTA$SAUDE" >/dev/null 2>&1; then
        echo "$NOME: smoke test OK depois de $((tentativa * 2)) s"
        exit 0
    fi
    tentativa=$((tentativa + 1))
    sleep 2
done

echo "$NOME: NÃO respondeu em 120 s. Últimas linhas do log:"
docker logs --tail 40 "$NOME" || true
exit 1