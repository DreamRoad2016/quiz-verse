#!/usr/bin/env python3
"""Optional DeepSeek audit for Sixteen Kingdoms events/rulers we just added."""
from __future__ import annotations

import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(REPO_ROOT / "scripts" / "novel_pipeline"))

from lib.deepseek_client import DeepSeekClient  # noqa: E402

OUT = REPO_ROOT / "scripts" / "data" / "history_stream" / "research" / "audit_sixteen_kingdoms_fill.json"


def main() -> None:
    events = json.loads(
        (REPO_ROOT / "src/main/resources/content/history/stream/events/sixteen_kingdoms.json").read_text()
    )
    rulers = json.loads(
        (REPO_ROOT / "src/main/resources/content/history/stream/rulers/sixteen_kingdoms.json").read_text()
    )
    client = DeepSeekClient({"model": "deepseek-v4-flash"})
    user = f"""请按中国大陆通行通史教材口径，审核下列十六国时间轴条目（公元年，闭区间）。
已有、不要重复讨论的节点：311永嘉、317东晋、383淝水、420刘宋代晋、439北魏统一北方。
政权条起讫已定为：汉赵304-329、成汉304-347、前凉317-376、后赵319-351、前燕337-370、前秦351-394、后燕384-407、后秦384-417、胡夏407-431、北魏386起。

events = {json.dumps(events, ensure_ascii=False)}
rulers = {json.dumps(rulers, ensure_ascii=False)}

只返回 JSON：
{{
  "ok": true/false,
  "corrections": [{{"id":"eventId或rulerId","field":"year或fromYear","from":旧,"to":新,"reason":"一句话"}}],
  "notes": ["口径说明，最多5条"]
}}
不要扩写成全帝系。年份差1年且教材两说可列入 notes 而不必 corrections。
"""
    data = client.chat_json(
        system="你是中国古代史纪年校对。只输出合法 JSON。不确定就 notes，不要编造。",
        user=user,
        hard=False,
        thinking="disabled",
        max_tokens=4096,
    )
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("wrote", OUT)
    print("ok", data.get("ok"))
    for c in data.get("corrections") or []:
        print("FIX", c)
    for n in data.get("notes") or []:
        print("NOTE", n)


if __name__ == "__main__":
    main()
