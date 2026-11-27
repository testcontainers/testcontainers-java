db = db.getSiblingDB("init-script-db");
db.messages.insertOne({ message: "init script ran" });
