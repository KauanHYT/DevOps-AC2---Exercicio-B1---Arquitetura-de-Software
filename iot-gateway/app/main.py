"""Gateway IoT (ADR-005): assina o broker MQTT dos laboratórios, traduz e entrega ao Core.

  MQTT_HOST / MQTT_PORTA  broker (padrão localhost:1883)
  CORE_URL                Core (padrão http://localhost:8080)

clean_session=False + client_id fixo + QoS 1: enquanto o gateway estiver fora, o broker
guarda as mensagens dos dispositivos e entrega todas quando ele volta.
"""
import logging
import os

import paho.mqtt.client as mqtt

from .core import enviar_ao_core
from .gateway import Contadores, processar

TOPICO = "labs/+/dispositivos/+/eventos"

log = logging.getLogger("iot-gateway")


def main() -> None:
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s [%(name)s] %(message)s")
    host = os.getenv("MQTT_HOST", "localhost")
    porta = int(os.getenv("MQTT_PORTA", "1883"))
    enviar = enviar_ao_core(os.getenv("CORE_URL", "http://localhost:8080"))
    contadores = Contadores()

    cliente = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id="iot-gateway", clean_session=False)

    def ao_conectar(c, _userdata, _flags, codigo, _properties):
        if codigo.is_failure:
            log.warning("broker recusou a conexão: %s", codigo)
            return
        c.subscribe(TOPICO, qos=1)
        log.info("conectado ao broker %s:%s; assinando %s (QoS 1, sessão persistente)", host, porta, TOPICO)

    def ao_receber(_c, _userdata, mensagem):
        processar(mensagem.topic, mensagem.payload, enviar, contadores)

    def ao_desconectar(_c, _userdata, _flags, codigo, _properties):
        log.warning("desconectado do broker (%s); tentando reconectar | %s", codigo, contadores)

    def ao_falhar_conexao(_c, _userdata):
        log.warning("broker %s:%s indisponível; nova tentativa em instantes", host, porta)

    cliente.on_connect = ao_conectar
    cliente.on_connect_fail = ao_falhar_conexao
    cliente.on_message = ao_receber
    cliente.on_disconnect = ao_desconectar
    cliente.reconnect_delay_set(min_delay=1, max_delay=10)
    cliente.connect_async(host, porta, keepalive=30)
    log.info("iniciando; broker %s:%s", host, porta)
    cliente.loop_forever(retry_first_connection=True)


if __name__ == "__main__":
    main()
