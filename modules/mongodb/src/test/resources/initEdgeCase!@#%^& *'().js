var complexCollectionName = 'test_col_"_with_specials_!@#%^&*()';

db.createCollection(complexCollectionName);

var collectionWithSpecialChars = "col with spaces & symbols !@#";

db.createCollection(collectionWithSpecialChars);

db.getCollection(complexCollectionName).insertOne({
    "_id": 1,
    "key_with_quotes": "This is a \"double quoted\" string",
    "key_with_json_chars": "{ } [ ] : ,",
    "description": "Insertion test for collection with special symbols"
});

print("Initialization completed: " + complexCollectionName);