# 题包数据脚本

| 文件 | 说明 |
|------|------|
| `build_zhenhuan_roster.py` | 从维基角色表 + 补充名册生成 `packs/zhenhuan_2011/entities.json` |
| `apply_zhenhuan_enrichment.py` | 按剧本校对后的剧情/关系/身份补丁写回 entities |
| `data/zhenhuan_ep01_10_patch.json` | 第 1–10 集校对补丁 |
| `data/zhenhuan_ep11_20_patch.json` | 第 11–20 集校对补丁（惊鸿舞、丽嫔失心等） |
| `data/zhenhuan_ep21_30_patch.json` | 第 21–30 集校对补丁（欢宜香小产、华贵妃等） |
| `data/zhenhuan_ep31_40_patch.json` | 第 31–40 集校对补丁（欢宜香坐实、华妃降答应等） |
| `data/zhenhuan_ep41_50_patch.json` | 第 41–50 集校对补丁（华妃赐死、误穿纯元故衣、甘露寺/凌云峰） |
| `data/zhenhuan_ep51_60_patch.json` | 第 51–60 集校对补丁（熹妃回宫、齐妃自裁、龙凤胎） |
| `data/zhenhuan_ep61_70_patch.json` | 第 61–70 集校对补丁（滴血验亲、惠妃去世、鹂妃事发、侧福晋） |
| `data/zhenhuan_ep71_76_patch.json` | 第 71–76 集校对补丁（废后、和亲试探、果亲王殉、后位终局） |
| `data/zhenhuan_core_seed.json` | 主线角色精修种子（再生时保留） |

关系网约定：核心人物用 `CORE_CONNECTIONS` 固定圈层，**每人最多 10 人**，避免无限扩张。
| `data/zhenhuan_wiki_character_list.md` | 维基「甄嬛传角色列表」本地快照 |

完整剧本文本在仓库根目录 `data/huanhuan/`，供本地校对重大剧情与关系网。

```bash
python3 -m venv .venv && .venv/bin/pip install zhconv
.venv/bin/python scripts/build_zhenhuan_roster.py
python3 scripts/apply_zhenhuan_enrichment.py
```
