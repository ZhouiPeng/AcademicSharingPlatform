#!/bin/bash
set -e

MYSQL_USER=${MYSQL_USER}
MYSQL_PASSWORD=${MYSQL_PASSWORD}
ACHIEVE_DB_NAME=${MYSQL_ACHIEVE_DB_NAME}
FILE_DB_NAME=${MYSQL_FILE_DB_NAME}
ADMIN_DB_NAME=${MYSQL_ADMIN_DB_NAME}
USER_DB_NAME=${MYSQL_USER_DB_NAME}
ANALYTICS_DB_NAME=${MYSQL_ANALYTICS_DB_NAME}

for i in {1..30}; do
  if mysqladmin ping -uroot >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

# Build SQL dynamically to avoid empty database names causing syntax errors
SQL=""
SQL+="CREATE USER IF NOT EXISTS '${MYSQL_USER}'@'%' IDENTIFIED BY '${MYSQL_PASSWORD}';\n"

add_db() {
  local dbname="$1"
  if [ -n "${dbname}" ]; then
    SQL+="CREATE DATABASE IF NOT EXISTS \`${dbname}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;\n"
    SQL+="GRANT ALL PRIVILEGES ON \`${dbname}\`.* TO '${MYSQL_USER}'@'%';\n"
  fi
}

add_db "${ACHIEVE_DB_NAME}"
add_db "${FILE_DB_NAME}"
add_db "${ADMIN_DB_NAME}"
add_db "${USER_DB_NAME}"
add_db "${ANALYTICS_DB_NAME}"

SQL+="FLUSH PRIVILEGES;\n"

echo -e "${SQL}" | mysql -uroot
