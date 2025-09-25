#!/bin/bash

echo "=== Chatbot Platform Build & Run ==="

# Функция для сборки модуля
build_module() {
    local module=$1
    echo "Building $module..."
    cd $module
    ./mvnw clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "Error building $module"
        exit 1
    fi
    cd ..
}

# Функция для копирования Maven Wrapper
copy_maven_wrapper() {
    local module=$1
    if [ ! -f "$module/mvnw" ]; then
        echo "Copying Maven Wrapper to $module..."
        cp mvnw $module/
        cp mvnw.cmd $module/
        cp -r .mvn $module/
    fi
}

case "$1" in
    "build")
        echo "Building all modules..."
        
        # Копируем Maven Wrapper в каждый модуль
        copy_maven_wrapper "api-gateway"
        copy_maven_wrapper "chat-service"
        copy_maven_wrapper "orchestrator"
        copy_maven_wrapper "scenario-service"
        
        # Собираем модули
        build_module "api-gateway"
        build_module "chat-service"
        build_module "orchestrator"
        build_module "scenario-service"
        
        echo "Build completed successfully!"
        ;;
        
    "run")
        echo "Starting services with Docker Compose..."
        docker-compose up -d postgres redis
        sleep 10
        
        echo "Starting API Gateway..."
        cd api-gateway && java -jar target/*-runner.jar &
        
        echo "Starting Chat Service..."
        cd ../chat-service && java -jar target/*-runner.jar &
        
        echo "Starting Orchestrator..."
        cd ../orchestrator && java -jar target/*-runner.jar &
        
        echo "Starting Scenario Service..."
        cd ../scenario-service && java -jar target/*-runner.jar &
        
        echo "Services started! Check:"
        echo "- API Gateway: http://localhost:8090/q/health"
        echo "- Chat Service: http://localhost:8091/q/health"
        echo "- Orchestrator: http://localhost:8092/q/health"
        echo "- Scenario Service: http://localhost:8093/q/health"
        ;;
        
    "docker")
        echo "Building and running with Docker Compose..."
        copy_maven_wrapper "api-gateway"
        copy_maven_wrapper "chat-service"
        docker-compose up --build
        ;;
        
    "stop")
        echo "Stopping services..."
        docker-compose down
        pkill -f "java -jar.*runner.jar"
        ;;
        
    *)
        echo "Usage: $0 {build|run|docker|stop}"
        echo "  build  - Build all modules"
        echo "  run    - Run services locally"
        echo "  docker - Build and run with Docker"
        echo "  stop   - Stop all services"
        exit 1
        ;;
esac
