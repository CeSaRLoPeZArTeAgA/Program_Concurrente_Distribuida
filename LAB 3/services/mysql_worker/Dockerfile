FROM python:3.12-slim-bookworm
RUN apt-get update \
    && apt-get install -y --no-install-recommends default-mysql-client curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY worker.py /app/worker.py
EXPOSE 8001
CMD ["python", "-u", "worker.py"]
