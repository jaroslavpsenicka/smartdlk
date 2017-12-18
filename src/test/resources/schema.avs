{
    "namespace": "cz.csas.smartdlk",
    "type": "record",
    "name": "Event",
    "fields": [
        {"name": "cidla", "type": "string"},
        {"name": "caseType", "type": "string"},
        {"name": "type", "type": "string"},
        {"name": "createdAt", "type": "long", "logicalType": "date"},
        {"name": "completedAt", "type": "long", "logicalType": "date"},
        {"name": "jsonData", "type": "string", "logicalType": "json"}
    ]
}