# Multi-stage build for Telegram Video Translation Service
# Supports both CPU and NVIDIA GPU

# Stage 1: Build Java application
FROM maven:3.9-eclipse-temurin-11-alpine AS java-builder

WORKDIR /build

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime image with Python and Java
# Use NVIDIA CUDA base image for GPU support (falls back to CPU if no GPU)
FROM nvidia/cuda:12.2.0-runtime-ubuntu22.04

# Install system dependencies
RUN apt-get update && apt-get install -y \
    openjdk-11-jre-headless \
    python3.11 \
    python3.11-dev \
    python3-pip \
    ffmpeg \
    wget \
    curl \
    build-essential \
    git \
    && rm -rf /var/lib/apt/lists/*

# Set Python 3.11 as default
RUN update-alternatives --install /usr/bin/python3 python3 /usr/bin/python3.11 1

# Upgrade pip and install wheel
RUN pip3 install --no-cache-dir --upgrade pip setuptools wheel

# Set working directory
WORKDIR /app

# Copy JAR from builder
COPY --from=java-builder /build/target/telegram-0.0.1-SNAPSHOT.jar /app/app.jar

# Copy Python scripts and requirements
COPY src/main/resources/python /app/python
COPY src/main/resources/application.yml /app/config/application.yml

# Install Python dependencies
RUN pip3 install --no-cache-dir -r /app/python/requirements.txt

# Create necessary directories
RUN mkdir -p /app/uploads /app/outputs /app/downloads /app/models

# Environment variables
ENV JAVA_OPTS="-Xmx2g -Xms512m" \
    SPRING_CONFIG_LOCATION=/app/config/application.yml \
    TRANSLATION_PYTHON_EXECUTABLE=/usr/bin/python3 \
    TRANSLATION_PYTHON_SCRIPTS_DIR=/app/python

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
