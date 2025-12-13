#!/bin/bash
set -e

# This script runs inside the official MongoDB docker-entrypoint-initdb.d
# It creates a MongoDB user from environment variables and grants
# the user the `dbOwner` role on the target database (ADMIN_DB_NAME).
#
# Environment variables expected:
# - MONGO_USER
# - MONGO_PASSWORD
# - ADMIN_DB_NAME (optional; defaults to "admin_service")

if [ -z "$MONGO_USER" ]; then
  echo "MONGO_USER not set; skipping Mongo user creation"
  exit 0
fi

TARGET_DB=${ADMIN_DB_NAME}

echo "Creating MongoDB user '$MONGO_USER' with dbOwner on '$TARGET_DB' (if not exists)"

mongo <<EOF
var adminDB = db.getSiblingDB('admin');
var username = "${MONGO_USER}";
var password = "${MONGO_PASSWORD}";
var targetDBName = "${TARGET_DB}";
if (!adminDB.getUser(username)) {
  adminDB.createUser({
    user: username,
    pwd: password,
    roles: [ { role: 'dbOwner', db: targetDBName } ]
  });
  print('Created user ' + username + ' with dbOwner on ' + targetDBName);
} else {
  print('MongoDB user ' + username + ' already exists');
}

// ensure the target database exists by creating a placeholder collection (no-op if exists)
var targetDB = db.getSiblingDB(targetDBName);
if (!targetDB.getCollectionNames().includes('_init_coll')) {
  targetDB.createCollection('_init_coll');
  print('Created placeholder collection in ' + targetDBName);
} else {
  print('Target DB ' + targetDBName + ' already has collections');
}
EOF

echo "Mongo init user script finished"
