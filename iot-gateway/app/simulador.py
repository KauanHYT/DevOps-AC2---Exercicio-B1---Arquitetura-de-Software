"""Simulador de dispositivo de laboratório (D5): publica eventos no broker MQTT.

Representa o aparelho, não o gateway; fica neste pacote só para usar a mesma imagem.

  python -m app.simulador --aluno 7 --presenca --inicio --conclusoes 1
  python -m app.simulador --aluno 7 --conclusoes 1 --repetir 3     # retransmissão (mesmo id 3 vezes)
  python -m app.simulador --aluno 7 --conclusoes 10 --offline      # guarda sem conexão e envia depois
"""
import argparse
import json
import os
import sys
import time
import uuid

import paho.mqtt.client as mqtt


def montar_eventos(aluno: int, presenca: bool, inicio: bool, conclusoes: int, agora: int) -> list[dict]:
    """Eventos no formato compacto do dispositivo, cada um com id próprio e o horário em que aconteceu."""
    tipos = (["P"] if presenca else []) + (["I"] if inicio else []) + ["C"] * conclusoes
    return [{"id": uuid.uuid4().hex, "a": aluno, "t": t, "ts": agora + i} for i, t in enumerate(tipos)]


def payload(evento: dict) -> bytes:
    return json.dumps(evento, separators=(",", ":")).encode("utf-8")


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description="Simulador de dispositivo IoT de laboratório")
    p.add_argument("--host", default=os.getenv("MQTT_HOST", "localhost"))
    p.add_argument("--porta", type=int, default=int(os.getenv("MQTT_PORTA", "1883")))
    p.add_argument("--laboratorio", default="lab-01")
    p.add_argument("--dispositivo", default="leitor-07")
    p.add_argument("--aluno", type=int, required=True)
    p.add_argument("--presenca", action="store_true")
    p.add_argument("--inicio", action="store_true")
    p.add_argument("--conclusoes", type=int, default=0)
    p.add_argument("--repetir", type=int, default=1, help="envia cada mensagem N vezes, com o mesmo id")
    p.add_argument("--offline", action="store_true", help="gera os eventos sem conexão e envia ao reconectar")
    args = p.parse_args(argv)

    topico = f"labs/{args.laboratorio}/dispositivos/{args.dispositivo}/eventos"
    eventos = montar_eventos(args.aluno, args.presenca, args.inicio, args.conclusoes, int(time.time()))
    if args.offline:
        print(f"[{args.dispositivo}] sem conexão: {len(eventos)} evento(s) guardado(s) na memória do dispositivo")
        time.sleep(2)
        print(f"[{args.dispositivo}] conexão de volta: enviando o que estava guardado")

    cliente = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=f"sim-{args.dispositivo}-{uuid.uuid4().hex[:6]}")
    try:
        cliente.connect(args.host, args.porta, keepalive=30)
    except OSError as e:
        print(f"[{args.dispositivo}] broker {args.host}:{args.porta} indisponível: {e}", file=sys.stderr)
        return 1
    cliente.loop_start()

    enviados, total_bytes = 0, 0
    for evento in eventos:
        corpo = payload(evento)
        for vez in range(args.repetir):
            info = cliente.publish(topico, corpo, qos=1)
            info.wait_for_publish(timeout=10)
            enviados += 1
            total_bytes += len(corpo)
            marca = " (reenvio do mesmo id)" if vez > 0 else ""
            print(f"[{args.dispositivo}] publicado {evento['id'][:8]} t={evento['t']} aluno={evento['a']} "
                  f"{len(corpo)} bytes{marca}")

    cliente.loop_stop()
    cliente.disconnect()
    media = total_bytes // enviados if enviados else 0
    print(f"[{args.dispositivo}] {enviados} mensagem(ns) publicada(s) em {topico}, payload médio de {media} bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main())
