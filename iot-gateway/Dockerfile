FROM python:3.12-slim

ENV PYTHONUNBUFFERED=1

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app app

# o mesmo container roda o gateway (padrão) ou o simulador de dispositivo:
#   docker compose run --rm iot-simulador --aluno 7 --conclusoes 1
CMD ["python", "-m", "app.main"]
