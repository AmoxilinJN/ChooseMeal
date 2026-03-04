# JSON Schema Contract (v1)

```json
{
  "version": 1,
  "cafeterias": [
    {"id": 1, "name": "一食堂", "sortOrder": 1, "enabled": true}
  ],
  "floors": [
    {"id": 10, "cafeteriaId": 1, "name": "1楼", "sortOrder": 1, "enabled": true}
  ],
  "meals": [
    {"id": 100, "floorId": 10, "name": "拉面", "tags": "面食", "enabled": true}
  ]
}
```

Validation rules:
- `version` must be `1`.
- IDs in each array must be unique.
- `floor.cafeteriaId` must exist in `cafeterias.id`.
- `meal.floorId` must exist in `floors.id`.
