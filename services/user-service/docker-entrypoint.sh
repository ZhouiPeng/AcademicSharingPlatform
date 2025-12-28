#!/bin/bash
set -e

echo "Waiting for MySQL to be ready at ${MYSQL_HOST}:${MYSQL_PORT}..."

# Wait for MySQL to be accessible
COUNTER=0
MAX_ATTEMPTS=60  # 60 * 2 seconds = 120 seconds
while [ $COUNTER -lt $MAX_ATTEMPTS ]; do
    if nc -z -w 2 ${MYSQL_HOST} ${MYSQL_PORT} >/dev/null 2>&1; then
        echo "MySQL is ready!"
        break
    fi
    COUNTER=$((COUNTER + 1))
    echo "MySQL is unavailable - sleeping... (attempt $COUNTER/$MAX_ATTEMPTS)"
    sleep 2
done

if [ $COUNTER -eq $MAX_ATTEMPTS ]; then
    echo "ERROR: MySQL failed to become ready after 120 seconds at ${MYSQL_HOST}:${MYSQL_PORT}!"
    exit 1
fi

echo "Starting application..."
exec java -jar app.jar --spring.profiles.active=prod
