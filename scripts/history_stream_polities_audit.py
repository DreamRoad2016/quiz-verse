#!/usr/bin/env python3
"""Focused DeepSeek audits for China polity years (split by era)."""
from __future__ import annotations

import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO_ROOT / "scripts" / "novel_pipeline"))
from lib.deepseek_client import DeepSeekClient  # noqa: E402

OUT = REPO_ROOT / "scripts" / "data" / "history_stream" / "research"

SYSTEM = """你是中国历史纪年校对员。只根据通行中国大陆通史教材与《中国历史纪年表》常见口径回答。
输出合法 JSON。对每条给出 fromYear,toYear（公元前负数）。有异说写 note，并给 recommended=true/false。
不要编造。时间轴产品截止 1949。"""


def ask(client: DeepSeekClient, name: str, user: str) -> dict:
    print(f"=== audit {name} ===", flush=True)
    data = client.chat_json(system=SYSTEM, user=user, hard=True, thinking="enabled", max_tokens=8192)
    path = OUT / f"audit_{name}.json"
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {path}", flush=True)
    return data


def main() -> None:
    client = DeepSeekClient({"model_hard": "deepseek-v4-pro"})

    ask(
        client,
        "qin_han_three_kingdoms",
        """请核对并给出下列政权标准起止年（JSON: {"items":[{id,name,fromYear,toYear,note,recommended}]}）：
qin 秦（统一后）, western_han 西汉, xin 新, eastern_han 东汉,
cao_wei 曹魏, shu_han 蜀汉, eastern_wu 孙吴, western_jin 西晋, eastern_jin 东晋。
特别说明：秦亡用 -207 还是 -206；曹魏亡 265 还是 266；孙吴起算 222 还是 229。每条给推荐值。""",
    )

    ask(
        client,
        "sixteen_kingdoms",
        """请列出传统「十六国」全部政权的推荐起止年，以及常附列的代、冉魏、西燕（不要段齐/翟魏/桓楚除非确有通行年表）。
JSON: {"canonicalSixteen":[{id,name,fromYear,toYear,note}],"adjacent":[{id,name,fromYear,toYear,note}],"warnings":[...]}
id 用英文蛇形；胡夏不要用 xia（与夏朝冲突），用 hu_xia。前赵用 former_zhao 或 han_zhao 并在 note 说明汉→赵改名。
重点核对：前凉起年、后燕终年、北凉终年（439）、前秦终年。""",
    )

    ask(
        client,
        "nanbeichao_sui_tang",
        """核对南朝宋齐梁陈、北魏/东魏/西魏/北齐/北周、隋、唐起止年。
JSON: {"items":[{id,name,fromYear,toYear,note,recommended}]}
id: liu_song, southern_qi, liang, chen, northern_wei, eastern_wei, western_wei, northern_qi, northern_zhou, sui, tang
核对西魏终年 556/557、北周起年、隋统一南方 589 是否只作事件而非政权起止。""",
    )

    ask(
        client,
        "five_dynasties_song_yuan_qing",
        """核对：五代（后梁后唐后晋后汉后周）、十国（吴、南唐、吴越、楚、闽、南汉、前蜀、后蜀、荆南/南平、北汉）起止；
辽、西夏、金、北宋、南宋；元（推荐 1271–1368）；明；后金与清（推荐拆分口径）；中华民国（1912–1949 产品截断）。
JSON: {"items":[{id,name,fromYear,toYear,tier,note}],"metaMarkers":[{id,name,year,note}],"warnings":[...]}
tier: primary|secondary。两宋、辽夏金为并立时 Song 用 northern_song/southern_song。""",
    )


if __name__ == "__main__":
    main()
