# JSON Schema Contract (v1)

## App 导入导出结构

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

## 社区共享约定

- 所有可导入配置文件放在 `community/configs/`。
- 文件内容必须完全遵循上面的 App 合约（可直接导入）。
- 学校元数据通过 `community/index.json` 维护，不写入配置文件本体。
- 社区 JSON Schema 参考：
  - `community/schema/choosemeal-config-v1.schema.json`
  - `community/schema/community-index-v1.schema.json`

## 校验工具

仓库提供 CI 与本地脚本进行校验：

```powershell
python scripts/validate_community_configs.py --project-root .
```

该脚本会检查：
- 每个配置 JSON 的结构与外键引用
- `index.json` 与配置文件的一致性
- 重复 `id` / 重复学校条目 / 丢失文件
