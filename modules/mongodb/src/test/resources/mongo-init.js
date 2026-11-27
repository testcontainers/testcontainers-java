db = db.getSiblingDB('testdb');
db.createCollection('testcollection');
db.testcollection.insertOne({ message: 'init script ran' });
