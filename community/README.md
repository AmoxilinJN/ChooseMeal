# Community Config Hub

这个目录用于维护各高校可直接导入 ChooseMeal 的食堂配置文件。

## 目录说明

- `configs/`：学校配置 JSON（与 App 导入格式完全一致）
- `index.json`：学校索引（学校名、城市、文件路径、更新时间）
- `schema/`：JSON Schema 参考

## 对普通用户

1. 打开 `index.json`，找到你的学校条目。
2. 下载对应 `file` 字段指向的 JSON。
3. 在 App 中进入 `导入导出` 页面导入该 JSON。

## 对贡献者

推荐通过 PR 提交，流程如下：

1. 在 `configs/` 新增或更新学校配置文件。
2. 在 `index.json` 增加或更新对应条目。
3. 本地执行校验：

```powershell
python scripts/validate_community_configs.py --project-root .
```

4. 提交 PR，等待 CI 自动校验通过。

## 文件命名建议

- 使用小写英文 + 连字符：`<school>-<campus>.json`
- 示例：`xidian-university-main-campus.json`

## 质量要求

- 配置应遵循 `version = 1`。
- `cafeterias.id` / `floors.id` / `meals.id` 需各自唯一。
- `floor.cafeteriaId` 与 `meal.floorId` 引用必须有效。
- 禁止提交个人隐私信息。
