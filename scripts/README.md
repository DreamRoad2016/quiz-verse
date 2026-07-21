# 题包数据脚本

| 文件 | 说明 |
|------|------|
| `build_zhenhuan_roster.py` | 从维基角色表 + 补充名册生成 `packs/zhenhuan_2011/entities.json` |
| `apply_zhenhuan_enrichment.py` | 按剧本校对后的剧情/关系/身份补丁写回 entities |
| `data/zhenhuan_core_seed.json` | 主线角色精修种子（再生时保留） |
| `data/zhenhuan_wiki_character_list.md` | 维基「甄嬛传角色列表」本地快照 |

完整剧本文本在仓库根目录 `data/huanhuan/`，供本地校对重大剧情与关系网。

```bash
python3 -m venv .venv && .venv/bin/pip install zhconv
.venv/bin/python scripts/build_zhenhuan_roster.py
python3 scripts/apply_zhenhuan_enrichment.py
```
