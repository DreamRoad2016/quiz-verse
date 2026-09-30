#!/usr/bin/env python3
"""Generate 「历史上的今天」demo day JSON via DeepSeek (generate + review).

Usage:
  python scripts/history_demo_generate.py              # 9-30 + Oct 1-31
  python scripts/history_demo_generate.py --only 09-30
  python scripts/history_demo_generate.py --force
  python scripts/history_demo_generate.py --review-only  # re-review existing drafts

Requires DEEPSEEK_API_KEY in env or repo-root .env.
"""
from __future__ import annotations

import argparse
import calendar
import json
import re
import sys
from datetime import date
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO_ROOT / "scripts" / "novel_pipeline"))

from lib.deepseek_client import DeepSeekClient  # noqa: E402

OUT_DIR = REPO_ROOT / "src" / "main" / "resources" / "content" / "history" / "days"

ALLOWED_TAGS = {
    "名人",
    "事件",
    "中国历史",
    "世界历史",
    "科技",
    "文化",
    "影视",
    "体育",
}

# 身份/职业词，不能当作人名
ROLE_WORDS = {
    "教育家", "学者", "名人", "人物", "作家", "科学家", "画家", "诗人", "音乐家",
    "政治家", "军事家", "演员", "男演员", "女演员", "翻译家", "考古学家", "数学家",
    "物理学家", "历史学家", "语言学家", "地理学家", "气象学家", "实业家", "艺术家",
    "天文学家", "核物理学家", "桥梁专家", "古典文学研究家", "文学家", "思想家",
    "哲学家", "发明家", "探险家", "运动员", "歌手", "导演",
}

LIFE_SUBJECT_RE = re.compile(
    r"([\u4e00-\u9fff]{2,4}|[A-Za-zÀ-ÿ][A-Za-zÀ-ÿ0-9\s·•\-]{1,40}?)"
    r"(出生|诞辰|诞生|逝世|去世|病逝)"
    r"(（[^）]*）|\([^)]*\))?$"
)

# 无主语 / 含糊称谓（不含「中国现代作家鲁迅逝世」这类已有主名）
VAGUE_TITLE_RES = [
    re.compile(r"某[位个名]?"),
    re.compile(r"一位"),
    re.compile(r"无名"),
    re.compile(
        r"(教育家|学者|名人|人物)(、|/|，|,)+(教育家|学者|名人|人物).*(逝世|出生|诞辰|诞生|去世)"
    ),
]
VAGUE_DESC_RES = [
    re.compile(r"一位.*(学者|教育家|名人|人物|作家).*(逝世|出生|诞辰)"),
    re.compile(r"某[位个].*(学者|教育家)"),
]

CONTENT_RULES = """
内容硬性规则（生成与核对都必须遵守）：
1. 日期必须可核实：只写「公历月日明确」的人物出生、逝世，或重大事件。不确定月日 → 不要写。
2. title 必须含具体人名或事件专名。出生/逝世推荐写成「张衡诞生」「巴金逝世」「傅抱石出生」；
   也可「中国画家傅抱石出生」。严禁「中国近代教育家、学者逝世」这类无具体人名的泛称。
3. 描述里必须写出全名；禁止「一位……学者逝世」而无姓名。
4. 偏中国、偏普世：中国古代/文化/科技人物与成就优先；世界侧选公认发明、探索、艺术、体育纪录等中性条目。
5. 严格避开敏感：
   - 日本相关（战争、侵略、争议领土、近代冲突等）一律不要；
   - 中国近现代政治敏感、党争、政治运动、重大政治冲突一律不要；
   - 1840 年后中国政治军事冲突非必要不写。
6. 少争议：不写高度政治化、仇恨、屠杀、暗杀等撕裂性条目。
7. 语气中性百科风；描述约 80～150 汉字，事实向。
8. tags 只能从：名人、事件、中国历史、世界历史、科技、文化、影视、体育。
9. eventType 建议：出生 / 逝世 / 事件（可空）。
10. importance 1～5 整数；region 填国家或地区简称。
""".strip()

GEN_SYSTEM = f"""你是严谨的历史资料编辑，为「历史上的今天」产出 JSON。
{CONTENT_RULES}
必须返回 JSON 对象：{{"events":[...]}}。
每条字段：year(int,公元前为负), month(int), day(int), title, description, tags(array), region, importance(int), eventType(string)。
不要编造 eventId。宁可少写也不要不确定日期或无主名条目。"""

REVIEW_SYSTEM = f"""你是史实核对编辑。输入是某日候选事件 JSON，请矫正后输出同结构 JSON 对象 {{"events":[...]}}。
{CONTENT_RULES}
核对要求：
- 逐条判断「是否确实发生在该月日」；不确定 → 删除，禁止臆造。
- 无具体人名/事件专名的条目必须删除（尤其泛称「学者/教育家逝世」）。
- 修正人名、年份、地点等明显错误；改写为中性百科描述。
- 删除违规（日本相关、国内近现代敏感、高争议）条目。
- 尽量保留约 20 条；删后不足可只补「确定日期且有主名」的替补；仍不确定则宁可更少。
- 输出完整 events 数组，不要解释文字。"""


def target_days() -> list[tuple[int, int]]:
    days = [(9, 30)]
    for d in range(1, 32):
        days.append((10, d))
    return days


def day_key(month: int, day: int) -> str:
    return f"{month:02d}-{day:02d}"


def assign_ids(events: list[dict[str, Any]], month: int, day: int) -> list[dict[str, Any]]:
    today = date.today().strftime("%Y%m%d")
    out: list[dict[str, Any]] = []
    for i, raw in enumerate(events, 1):
        e = dict(raw)
        year = int(e.get("year") or 0)
        y_part = f"BC{abs(year)}" if year < 0 else f"{year:04d}"
        e["eventId"] = f"E{today}S{y_part}{month:02d}{day:02d}D{i:04d}"
        e["month"] = month
        e["day"] = day
        out.append(e)
    return out


def extract_events(payload: Any) -> list[dict[str, Any]]:
    if isinstance(payload, list):
        return [x for x in payload if isinstance(x, dict)]
    if isinstance(payload, dict):
        events = payload.get("events")
        if isinstance(events, list):
            return [x for x in events if isinstance(x, dict)]
        for v in payload.values():
            if isinstance(v, list) and v and isinstance(v[0], dict):
                return [x for x in v if isinstance(x, dict)]
    return []


def _looks_like_life_event(title: str, event_type: str) -> bool:
    # 仅当标题本身是生卒句式时才强制做人名抽取；eventType 可能标错
    return bool(re.search(r"(出生|诞辰|诞生|逝世|去世|病逝)(（[^）]*）|\([^)]*\))?$", title))


def _life_subject(title: str) -> str | None:
    m = LIFE_SUBJECT_RE.search(title.strip())
    if not m:
        return None
    name = m.group(1).strip(" ·•-")
    if name in ROLE_WORDS:
        return None
    if name in {"中国", "近代", "现代", "当代", "著名", "伟大", "知名"}:
        return None
    return name


def validate_event(e: dict[str, Any], month: int, day: int) -> str | None:
    title = str(e.get("title") or "").strip()
    desc = str(e.get("description") or "").strip()
    event_type = str(e.get("eventType") or "").strip()
    if not title:
        return "missing title"
    if not desc:
        return "missing description"
    if len(desc) < 40:
        return "description too short"
    try:
        year = int(e["year"])
        m = int(e.get("month", month))
        d = int(e.get("day", day))
        imp = int(e.get("importance", 3))
    except (KeyError, TypeError, ValueError):
        return "bad year/month/day/importance"
    if m != month or d != day:
        return f"date mismatch {m}-{d}"
    if year == 0:
        return "year 0 invalid"
    if imp < 1 or imp > 5:
        return "importance out of range"
    tags = e.get("tags") or []
    if not isinstance(tags, list) or not tags:
        return "tags required"
    for t in tags:
        if t not in ALLOWED_TAGS:
            return f"illegal tag {t}"
    for rx in VAGUE_TITLE_RES:
        if rx.search(title):
            return "vague title / missing subject"
    for rx in VAGUE_DESC_RES:
        if rx.search(desc):
            return "vague description / missing subject"
    if _looks_like_life_event(title, event_type):
        if not _life_subject(title):
            return "life event title missing person name"
    return None


def sanitize(events: list[dict[str, Any]], month: int, day: int) -> list[dict[str, Any]]:
    cleaned: list[dict[str, Any]] = []
    for e in events:
        e = dict(e)
        e["month"] = month
        e["day"] = day
        tags = e.get("tags") or []
        if isinstance(tags, str):
            tags = [x.strip() for x in re.split(r"[,，|/]", tags) if x.strip()]
        e["tags"] = [t for t in tags if t in ALLOWED_TAGS]
        if not e["tags"]:
            e["tags"] = ["事件"]
        try:
            e["importance"] = max(1, min(5, int(e.get("importance", 3))))
            e["year"] = int(e["year"])
        except (TypeError, ValueError):
            continue
        e["title"] = str(e.get("title") or "").strip()
        e["description"] = str(e.get("description") or "").strip()
        e["region"] = str(e.get("region") or "").strip() or "中国"
        e["eventType"] = str(e.get("eventType") or "").strip()
        err = validate_event(e, month, day)
        if err:
            print(f"  drop: {e.get('title', '?')} ({err})")
            continue
        cleaned.append(e)
    return cleaned


def generate_day(client: DeepSeekClient, month: int, day: int) -> list[dict[str, Any]]:
    user = (
        f"请为公历 {month} 月 {day} 日生成约 20 条「历史上的今天」事件。"
        f"month 必须为 {month}，day 必须为 {day}。"
        "每条 title 必须含具体人名或事件专名；禁止无主语泛称。"
        "优先中国人物生卒与文化科技；世界侧选非争议普世条目。"
        '返回 {"events":[...]}。'
    )
    print(f"  [1/2] generate {month}-{day} ...", flush=True)
    payload = client.chat_json(system=GEN_SYSTEM, user=user, hard=False, max_tokens=8192)
    events = sanitize(extract_events(payload), month, day)
    print(f"  generated {len(events)} candidates", flush=True)
    return events


def review_day(
    client: DeepSeekClient, month: int, day: int, events: list[dict[str, Any]]
) -> list[dict[str, Any]]:
    slim = []
    for e in events:
        slim.append({k: e[k] for k in e if k != "eventId"})
    user = (
        f"以下是公历 {month} 月 {day} 日的候选事件 JSON，请核对矫正后返回 "
        f'{{"events":[...]}}。month={month}, day={day}。'
        "务必删掉无具体人名/专名的条目。\n\n"
        + json.dumps({"events": slim}, ensure_ascii=False)
    )
    print(f"  [2/2] review {month}-{day} ...", flush=True)
    payload = client.chat_json(system=REVIEW_SYSTEM, user=user, hard=True, max_tokens=8192)
    reviewed = sanitize(extract_events(payload), month, day)
    print(f"  reviewed {len(reviewed)} events", flush=True)
    return reviewed


def write_day(month: int, day: int, events: list[dict[str, Any]]) -> Path:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    final = assign_ids(events, month, day)
    path = OUT_DIR / f"{day_key(month, day)}.json"
    path.write_text(json.dumps(final, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return path


def parse_only(s: str) -> tuple[int, int]:
    m = re.fullmatch(r"(\d{1,2})-(\d{1,2})", s.strip())
    if not m:
        raise SystemExit(f"--only 格式应为 MM-DD，收到: {s}")
    month, day = int(m.group(1)), int(m.group(2))
    if month < 1 or month > 12 or day < 1 or day > 31:
        raise SystemExit(f"非法日期: {s}")
    if month == 2 and day > 29:
        raise SystemExit(f"非法日期: {s}")
    if month in (4, 6, 9, 11) and day > 30:
        raise SystemExit(f"非法日期: {s}")
    return month, day


def main() -> None:
    parser = argparse.ArgumentParser(description="DeepSeek 两轮生成历史上的今天 demo")
    parser.add_argument("--only", help="只处理某日，如 09-30")
    parser.add_argument("--force", action="store_true", help="覆盖已有非空日文件")
    args = parser.parse_args()

    if args.only:
        days = [parse_only(args.only)]
    else:
        days = target_days()

    client = DeepSeekClient()
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    for month, day in days:
        key = day_key(month, day)
        out_path = OUT_DIR / f"{key}.json"
        print(f"\n=== {key} ===", flush=True)

        if out_path.exists() and out_path.stat().st_size > 10 and not args.force:
            print("  skip (exists). use --force to regenerate", flush=True)
            continue

        events = generate_day(client, month, day)
        reviewed = review_day(client, month, day, events)
        if len(reviewed) < 8:
            print(f"  warn: only {len(reviewed)} events after review", flush=True)
        path = write_day(month, day, reviewed)
        print(f"  wrote {path} ({len(reviewed)} events)", flush=True)

    print("\nDone.", flush=True)


if __name__ == "__main__":
    _ = calendar
    main()
