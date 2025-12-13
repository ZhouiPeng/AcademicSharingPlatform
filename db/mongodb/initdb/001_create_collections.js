if (!db.getCollectionNames().includes('messages')) {
  db.createCollection('messages');
}
