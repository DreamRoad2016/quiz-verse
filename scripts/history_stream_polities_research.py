#!/usr/bin/env python3
"""Multi-round DeepSeek research for China polity timeline (History Stream).

Rounds:
  1) Mainline dynasties to 1949
  2) Sixteen Kingdoms + Northern/Southern Dynasties structure
  3) Parallel regimes (Three Kingdoms, Five Dynasties, Song-Liao-Jin-Xia-Yuan, etc.)
  4) Cross-check / reconcile conflicts
  5) Pro model final audit

Usage:
  python scripts/history_stream_polities_research.py
  python scripts/history_stream_polities_research.py --from-round 3

Requires DEEPSEEK_API_KEY in env or repo-root .env.
Writes JSON under scripts/data/history_stream/research/ (gitignored optional).
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO_ROOT / "scripts" / "novel_pipeline"))

from lib.deepseek_client import DeepSeekClient  # noqa: E402

OUT_DIR = REPO_ROOT / "scripts" / "data" / "history_stream" / "research"

SYSTEM = """你是中国古代史与近现代史资料顾问。
任务：为「历史时间轴产品」提供政权/朝代起止年数据。
口径要求：
1. 以通行中国大陆中学/大学通史教材与《中国历史纪年表》常见说法为主，不纠缠冷僻争议。
2. 起止年用公元年：公元前用负数（前221年 = -221）。结束年取该政权灭亡/改朝那年。
3. 时间轴截止到中华人民共和国成立：1949（含）。不要列 1949 之后政权。
4. 输出必须是合法 JSON 对象；年份用整数。
5. 对有争议的起止，在 note 字段写清采用哪一种口径，并列出常见异说。
6. 不要编造；不确定就标 uncertain=true 并说明。
"""


def save(name: str, data: dict[str, Any]) -> Path:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    path = OUT_DIR / f"{name}.json"
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {path}", flush=True)
    return path


def load_if_exists(name: str) -> dict[str, Any] | None:
    path = OUT_DIR / f"{name}.json"
    if not path.is_file():
        return None
    return json.loads(path.read_text(encoding="utf-8"))


def round1_mainline(client: DeepSeekClient) -> dict[str, Any]:
    user = """请列出中国史「主线/正统序列」上用于时间轴展示的主要政权（从夏商争议期可简写，重点自西周或至少自秦起），
截止 1949 年中华人民共和国成立。

要求字段 polities[] 每项：
- id (英文蛇形，如 qin, western_han)
- name (中文标准名)
- fromYear, toYear
- tier: "primary"（主线王朝/统一或公认正统序列）
- capital (都城，可空)
- note (口径说明)
- uncertain (boolean)

另给 eras[]：适合时间轴顶部 Tab 的时代分段（先秦、秦汉、魏晋南北朝、隋唐、五代十国、宋辽夏金元、明清、晚清民国至新中国成立），
每项 id,label,fromYear,toYear,sort。

只返回 JSON：{"eras":[...],"polities":[...],"assumptions":[...]}
"""
    return client.chat_json(system=SYSTEM, user=user, hard=True, thinking="enabled", max_tokens=8192)


def round2_chaotic(client: DeepSeekClient) -> dict[str, Any]:
    user = """专攻乱世结构设计，用于时间轴「多泳道」：

A. 五胡十六国（约 304–439）：请列出通常所说「十六国」各自起止年；并说明哪些是传统十六国名单内、哪些常被附带提到（如冉魏、西燕、代等）应用 tier=secondary/tertiary 如何分层。
B. 南北朝：南朝宋齐梁陈；北朝北魏及分裂后的东魏/西魏、北齐/北周 —— 全部起止年。
C. 东晋与十六国并行时，东晋应如何与十六国同屏展示（parentGroup / lane 建议）。

请给出结构建议：
- groups[]: {id, name, fromYear, toYear, description, laneHint}
- polities[]: {id, name, fromYear, toYear, groupId, tier, lane, sort, note, uncertain}
  tier 取值 primary|secondary|tertiary
  lane 用整数，同屏并立时不同 lane 不重叠语义冲突即可

只返回 JSON。务必年份准确；有争议写 note。
"""
    return client.chat_json(system=SYSTEM, user=user, hard=True, thinking="enabled", max_tokens=12288)


def round3_parallel(client: DeepSeekClient) -> dict[str, Any]:
    user = """请校核并列出以下「并立政权」用于中国史时间轴（截止1949）：

1. 三国：魏、蜀、吴（含起止；曹魏与西晋衔接）
2. 五代十国：五代（梁唐晋汉周）+ 十国主要政权起止
3. 辽、西夏、金 与 北宋/南宋 并行
4. 蒙元：蒙古汗国/大蒙古国与元朝国号起止口径（说明采用哪一年作为「元」起算：1271？1279？）
5. 明、清；清的起算：1616后金？1636清？1644入关？请给 timeline 产品推荐口径并说明
6. 中华民国（1912–1949 产品截断）、中华人民共和国成立 1949 作为终点标记（可做成零长度或单点 polity/meta，不要延伸到1949后）

返回 JSON：
{"polities":[...同前字段...],"metaMarkers":[{"id","name","year","note"}],"assumptions":[...],"disputed":[...]}
"""
    return client.chat_json(system=SYSTEM, user=user, hard=True, thinking="enabled", max_tokens=12288)


def round4_reconcile(client: DeepSeekClient, r1: dict, r2: dict, r3: dict) -> dict[str, Any]:
    user = f"""下面是三轮研究结果（JSON）。请合并为一份「可落库」的中国史政权表，用于产品 History Stream。

合并规则：
1. 去重：同一政权只保留一条；id 统一英文蛇形。
2. 乱世必须保留 groupId / lane / tier，便于泳道渲染。
3. 时间轴截止 1949；中华人民共和国成立作为终点 meta，不要把 PRC 画成延伸到今天的长条（本产品截断）。
4. 指出三轮之间年份冲突，并给出最终采用值与理由。
5. eras 给出最终 Tab 分段。

输入 round1:
{json.dumps(r1, ensure_ascii=False)[:50000]}

输入 round2:
{json.dumps(r2, ensure_ascii=False)[:50000]}

输入 round3:
{json.dumps(r3, ensure_ascii=False)[:50000]}

返回 JSON：
{{
  "eras":[...],
  "groups":[...],
  "polities":[...],
  "metaMarkers":[...],
  "conflictsResolved":[...],
  "openQuestions":[...]
}}
"""
    return client.chat_json(system=SYSTEM, user=user, hard=True, thinking="enabled", max_tokens=16384)


def round5_audit(client: DeepSeekClient, merged: dict) -> dict[str, Any]:
    user = f"""请作为第二审，专门挑错。检查以下合并稿中每个 fromYear/toYear 是否与通行纪年一致。
重点检查：
- 秦统一 -221；秦亡 -207 还是 -206？
- 西汉建立 -202；新朝 9–23；东汉 25–220
- 三国起止；西晋 265–316；东晋 317–420
- 十六国典型：前赵/汉赵、前秦、后秦、北凉灭亡年（北魏统一北方 439）
- 北魏 386–534；东魏西魏；北齐北周；隋 581–618；唐 618–907
- 五代起止；北宋 960–1127；南宋 1127–1279
- 辽 907/916–1125；西夏 1038–1227；金 1115–1234
- 元 1271–1368 口径；明 1368–1644；清 1636/1644–1912
- 中华民国 1912；截断 1949

对每一处错误给出 correction；全对也要列出你核对过的关键节点 checklist。

合并稿：
{json.dumps(merged, ensure_ascii=False)[:60000]}

返回 JSON：
{{"corrections":[{{"id","field","old","new","reason"}}],"checklist":[...],"approvedPolitiesHint":"简述还能否直接采用","severityIssues":[...]}}
"""
    return client.chat_json(system=SYSTEM, user=user, hard=True, thinking="enabled", max_tokens=12288)


def apply_corrections(merged: dict[str, Any], audit: dict[str, Any]) -> dict[str, Any]:
    """Apply audit corrections onto polities/eras/metaMarkers by id."""
    by_id: dict[str, dict[str, Any]] = {}
    for key in ("polities", "eras", "groups", "metaMarkers"):
        for item in merged.get(key) or []:
            if isinstance(item, dict) and item.get("id"):
                by_id[item["id"]] = item

    for corr in audit.get("corrections") or []:
        if not isinstance(corr, dict):
            continue
        pid = corr.get("id")
        field = corr.get("field")
        if not pid or not field or pid not in by_id:
            continue
        if "new" in corr:
            by_id[pid][field] = corr["new"]
            note = by_id[pid].get("note") or ""
            reason = corr.get("reason") or ""
            suffix = f"[audit] {field}: {corr.get('old')}→{corr.get('new')}. {reason}".strip()
            by_id[pid]["note"] = (note + " " + suffix).strip() if note else suffix

    merged["audit"] = {
        "corrections": audit.get("corrections") or [],
        "checklist": audit.get("checklist") or [],
        "remainingIssues": audit.get("remainingIssues") or [],
        "approvedPolitiesHint": audit.get("approvedPolitiesHint"),
    }
    return merged


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--from-round", type=int, default=1, help="Skip earlier rounds if JSON exists")
    args = parser.parse_args()

    client = DeepSeekClient({"model": "deepseek-v4-flash", "model_hard": "deepseek-v4-pro"})

    r1 = load_if_exists("round1_mainline") if args.from_round > 1 else None
    r2 = load_if_exists("round2_chaotic") if args.from_round > 2 else None
    r3 = load_if_exists("round3_parallel") if args.from_round > 3 else None
    r4 = load_if_exists("round4_merged") if args.from_round > 4 else None

    if r1 is None:
        print("=== Round 1: mainline ===", flush=True)
        r1 = round1_mainline(client)
        save("round1_mainline", r1)
    else:
        print("reuse round1", flush=True)

    if r2 is None:
        print("=== Round 2: chaotic periods ===", flush=True)
        r2 = round2_chaotic(client)
        save("round2_chaotic", r2)
    else:
        print("reuse round2", flush=True)

    if r3 is None:
        print("=== Round 3: parallel regimes ===", flush=True)
        r3 = round3_parallel(client)
        save("round3_parallel", r3)
    else:
        print("reuse round3", flush=True)

    if r4 is None:
        print("=== Round 4: reconcile ===", flush=True)
        r4 = round4_reconcile(client, r1, r2, r3)
        save("round4_merged", r4)
    else:
        print("reuse round4", flush=True)

    print("=== Round 5: pro audit ===", flush=True)
    r5 = round5_audit(client, r4)
    save("round5_audit", r5)

    final = apply_corrections(json.loads(json.dumps(r4)), r5)
    save("final_merged", final)
    print("done", flush=True)


if __name__ == "__main__":
    main()
