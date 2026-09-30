# 历史上的今天 — 按日 JSON（唯一正式数据源）
#
# 文件名：MM-DD.json（如 09-30.json）
# 内容：HistoryEvent 数组
# 字段：eventId, year, month, day, title, description, tags[], region, importance, eventType?
# tags：名人 | 事件 | 中国历史 | 世界历史 | 科技 | 文化 | 影视 | 体育
#
# 生成：python scripts/history_demo_generate.py
# 质量扫描：python scripts/history_demo_lint.py [--fix]
# 可选覆盖目录：quiz.history.extra-dir / QUIZ_HISTORY_DIR
#
# 注意：不要再维护 gen/review 草稿双份；运行时只读本目录。
