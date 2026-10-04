# 历史源流 · 谱系（无名年）

- JSON 数组；**不含** `fromYear` / `toYear`，不上时间轴，仅供夏商等「有名无可靠绝对年」的世系展示。
- 必填：`id`, `name`, `polityId`, `sort`
- 可选：`personalName`, `note`
- 与 `rulers/` 区分：`rulers` 有纪年且默认 `onTimeline`；谱系条目由 API `/lineages?polityId=` 按政权筛选。

样例：`xia.json`（十七王）、`shang.json`（《史记·殷本纪》主线三十王）。
