#!/bin/bash
set -e

if [ -z "$MONGO_USER" ]; then
  echo "MONGO_USER not set; skipping Mongo user creation"
  exit 0
fi

TARGET_DB=${MONGO_ADMIN_DB_NAME}

echo "Creating MongoDB user '$MONGO_USER' with dbOwner on '$TARGET_DB' (if not exists)"

if command -v mongosh >/dev/null 2>&1; then
  SHELL_CMD="mongosh --quiet"
elif command -v mongo >/dev/null 2>&1; then
  SHELL_CMD="mongo"
else
  echo "Neither mongosh nor mongo client found in image; cannot create user" >&2
  exit 1
fi

${SHELL_CMD} <<EOF
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
var targetDB = db.getSiblingDB(targetDBName);
if (!targetDB.getCollectionNames().includes('_init_coll')) {
  targetDB.createCollection('_init_coll');
  print('Created placeholder collection in ' + targetDBName);
} else {
  print('Target DB ' + targetDBName + ' already has collections');
}
EOF

echo "Mongo init user script finished"
