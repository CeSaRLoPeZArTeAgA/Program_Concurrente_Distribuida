FROM eclipse-temurin:21-jdk-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends mariadb-client curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY Worker.java /app/Worker.java
RUN javac Worker.java
EXPOSE 8003
CMD ["java", "Worker"]
