# 数据说明

## `lol-players-cn-kr.json`

- **来源**：公开接口 `GET https://guessassin.xyz/api/players/CN_KR`（中韩赛区选手池，约 281 条）。
- **用途**：仅在 **本地开发** 中验证「读库 → 比对 → 前端」流程；**勿用于对外商用**，避免版权与数据合规问题。
- **更新**：需要时可重新执行：

```bash
curl -sS -o lol-players-cn-kr.json "https://guessassin.xyz/api/players/CN_KR"
```
