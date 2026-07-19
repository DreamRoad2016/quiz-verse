# 题包（packs）

每个子目录是一个可玩的猜人物题库。

```
{packId}/
  pack.yaml      # 元信息：id、标题、最大猜测次数、demo/official
  schema.yaml    # 比对列与枚举
  entities.json  # 人物列表（attrs 仅服务端使用）
```

## 列类型（schema.columns[].type）

| type | 含义 |
|------|------|
| `identity` | 同一人物才绿（显示姓名） |
| `exact` | 单值全等 |
| `set` | 集合：全等绿 / 相交黄 / 否则灰 |
| `number` | 数值；可配 `nearThreshold`，带 ↑↓ |

`attr` 默认等于 `key`，指向 `entities[].attrs` 下的字段。  
`enumRef` 可选，用于把存值映射成展示文案（见 `schema.enums`）。

## 当前题包

- `lol_cn_kr` — Demo
- `zhenhuan_2011` — 正式（甄嬛传）

本地覆盖目录可通过环境变量 `QUIZ_PACKS_DIR` 指向外部文件夹（同结构），会覆盖同 id 的 classpath 题包。
