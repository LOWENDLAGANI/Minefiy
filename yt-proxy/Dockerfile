FROM python:3.12-slim

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    PORT=8080

WORKDIR /app

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY main.py ./

RUN useradd --create-home --uid 10001 appuser
USER appuser

EXPOSE 8080

# Cloud Run injects $PORT and terminates TLS at its own proxy, so uvicorn must
# trust X-Forwarded-* to log real client IPs. Shell form is required for ${PORT}
# expansion. One worker per container: Cloud Run scales by instances, and extra
# uvicorn workers would each keep their own response cache.
CMD exec uvicorn main:app --host 0.0.0.0 --port "${PORT}" \
    --proxy-headers --forwarded-allow-ips='*' --workers 1
