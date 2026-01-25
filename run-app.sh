#!/bin/bash

# Загружаем переменные из .env
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
    echo "✅ .env загружен"
else
    echo "⚠️  .env файл не найден"
fi

# Проверяем обязательные переменные
if [ -z "$TELEGRAM_BOT_TOKEN" ]; then
    echo "ERROR: TELEGRAM_BOT_TOKEN не установлен!"
    exit 1
fi

# Проверка NGROK_URL (исправлена - добавили $)
if [ -z "$NGROK_URL" ]; then
    echo "WARNING: NGROK_URL не установлен, будет использован localhost"
fi

echo "Запуск с переменными:"
echo "TELEGRAM_BOT_TOKEN: ${TELEGRAM_BOT_TOKEN:0:10}..."
echo "N8N_WEBHOOK_URL: ${N8N_WEBHOOK_URL:-NOT_SET}"
echo "NGROK_URL: ${NGROK_URL:-NOT_SET}"

# Запускаем приложение с переменными окружения
TELEGRAM_BOT_TOKEN="$TELEGRAM_BOT_TOKEN" \
N8N_WEBHOOK_URL="${N8N_WEBHOOK_URL:-http://localhost:5678/webhook/test-message}" \
NGROK_URL="${NGROK_URL:-http://localhost:8080}" \
./gradlew bootRun