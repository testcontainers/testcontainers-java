var complexCollectionName = 'test_col_"_with_specials_!@#%^&*()';

db.createCollection(complexCollectionName);

var japaneseCollectionName = "日本語 コレクション ﾃｽﾄ";

db.createCollection(japaneseCollectionName);

db.getCollection(complexCollectionName).insertOne({
    "_id": 1,
    "key_with_quotes": "This is a \"double quoted\" string",
    "key_with_json_chars": "{ } [ ] : ,",
    "description": "特殊記号を含むコレクションへの挿入テスト"
});

print("Initialization completed: " + complexCollectionName);