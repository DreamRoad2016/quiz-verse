# 作品字段配置（本地文件）

每部影视剧一个目录：`config/works/{work_id}/`

| 文件 | 作用 |
| --- | --- |
| `fields.json` | 录入页字段：单选/多选、是否参与比对、对应 attrs 键 |
| `enums/*.json` | 下拉/多选**选项内容**（可随时增删，不必改表结构） |

## 甄嬛传

路径：`config/works/zhenhuan_2011/`

- **称呼**（`call_names` 列）：旁人怎么叫，如「嬛嬛」「熹贵妃」——**不是位分**
- **位分**（`attrs.titles`）：答应、贵人、嫔…——从 `enums/titles.json` 多选
- **重大剧情**（`attrs.major_plots`）：存 `major_plots.json` 里的 **key** 数组，如 `["dixue_qinzi","jinghong_wu"]`

录入页（待开发）应：

1. 读 `fields.json` 渲染表单  
2. 读各 `enumFile` 渲染选项  
3. 保存时写入 `work_character` 列 + `attrs` jsonb  

比对 API 读 `work_column` + 角色 `attrs`；选项展示名从本目录 json 解析。
