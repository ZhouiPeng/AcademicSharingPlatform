#!/bin/bash
set -e

if [ -z "$MONGO_USER" ]; then
  echo "MONGO_USER not set; skipping Mongo user creation"
  exit 0
fi

if [ -z "$MONGO_ADMIN_DB_NAME" ]; then
  echo "MONGO_ADMIN_DB_NAME not set"
  exit 1
fi

SHELL_CMD="mongosh -u \"$MONGO_INITDB_ROOT_USERNAME\" -p \"$MONGO_INITDB_ROOT_PASSWORD\" --authenticationDatabase admin --quiet"

${SHELL_CMD} <<EOF
var username = "${MONGO_USER}";
var password = "${MONGO_PASSWORD}";
var targetDBName = "${MONGO_ADMIN_DB_NAME}";
var targetDB = db.getSiblingDB(targetDBName);

if (!targetDB.getUser(username)) {
  targetDB.createUser({
    user: username,
    pwd: password,
    roles: [ { role: 'dbOwner', db: targetDBName } ]
  });
  print('Created user ' + username + ' in ' + targetDBName);
} else {
  print('MongoDB user ' + username + ' already exists in ' + targetDBName);
}

if (!targetDB.getCollection('_init_coll').exists()) {
  targetDB.createCollection('_init_coll');
  print('Created placeholder collection in ' + targetDBName);
}
EOF

echo "Mongo init user script finished"
