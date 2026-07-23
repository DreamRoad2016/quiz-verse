#!/usr/bin/env python3
"""Audit Zhenhuan pack for Wordle-like guessability.

Game goal (reminder):
  Each guess reveals column feedback (gender / role / titles / residences /
  major_plots / connections). Players converge by eliminating lookalikes.
  Connections need NOT list everyone on protagonists, but a peripheral answer
  must not be an empty-grey void among identical peers — otherwise unguessable.

Checks:
  A. empty connections (esp. when peers share the same fingerprint)
  B. hard twins: same gender+role+titles+residences+plots, and connections
     don't separate them (no unique peer / shared-peer signal)
  C. one-way edges (A→B but not B→A) — may be OK for 从属→主子
  D. soft twins: same gender+role+titles+residences with empty plots

Writes:
  scripts/data/zhenhuan_playability_audit.md
"""
from __future__ import annotations

import json
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENTITIES = ROOT / "src/main/resources/packs/zhenhuan_2011/entities.json"
OUT = ROOT / "scripts/data/zhenhuan_playability_audit.md"

# 主角 / 高戏份：关系网名额紧，不要求互逆挂小角色
PROTAGONISTS = {
    "甄嬛", "爱新觉罗·胤禛", "乌拉那拉·宜修", "年世兰", "安陵容", "沈眉庄",
    "乌雅氏", "允礼", "叶澜依", "曹贵人",
}

# 已知合理「只从小角色指向大角色」的服务边模式（审计时降级为提示）
KNOWN_SERVICE_TARGETS = PROTAGONISTS | {
    "温实初", "苏培盛", "纯元皇后", "敬妃", "端妃", "齐妃", "富察·温宜",
    "浣碧", "崔槿汐", "孟静娴", "弘历", "弘时", "年羹尧",
}


def fingerprint(e: dict, with_plots: bool = True) -> tuple:
    a = e["attrs"]
    base = (
        a.get("gender"),
        a.get("role_type"),
        tuple(sorted(a.get("titles") or [])),
        tuple(sorted(a.get("residences") or [])),
    )
    if with_plots:
        return base + (tuple(sorted(a.get("major_plots") or [])),)
    return base


def conn_names(e: dict, by_id: dict) -> list[str]:
    out = []
    for cid in e["attrs"].get("connections") or []:
        pe = by_id.get(cid)
        out.append(pe["name"] if pe else f"?{cid}")
    return out


def main() -> None:
    ents = json.loads(ENTITIES.read_text(encoding="utf-8"))
    by_id = {e["id"]: e for e in ents}
    by_name = {e["name"]: e for e in ents}

    empty = [e for e in ents if not (e["attrs"].get("connections") or [])]

    # clusters with identical playable attrs except connections
    hard = defaultdict(list)
    soft = defaultdict(list)
    for e in ents:
        hard[fingerprint(e, True)].append(e)
        soft[fingerprint(e, False)].append(e)

    # within hard cluster, who are still inseparable by connection sets?
    inseparable = []
    for key, group in hard.items():
        if len(group) < 2:
            continue
        # two entities are inseparable if their connection-name sets are equal
        # (including both empty) OR both empty
        by_conn = defaultdict(list)
        for e in group:
            by_conn[tuple(sorted(conn_names(e, by_id)))].append(e)
        for ckey, g in by_conn.items():
            if len(g) >= 2:
                inseparable.append((key, ckey, g))

    # one-way edges
    one_way = []
    for e in ents:
        for cid in e["attrs"].get("connections") or []:
            other = by_id.get(cid)
            if not other:
                continue
            if e["id"] not in (other["attrs"].get("connections") or []):
                one_way.append((e["name"], other["name"]))

    service_ok = []
    mutual_suspect = []
    for a, b in one_way:
        if a in PROTAGONISTS and b not in PROTAGONISTS:
            # protagonist pointing to side character without reverse — unusual
            mutual_suspect.append((a, b, "主角指向配角但配角未回链"))
        elif b in KNOWN_SERVICE_TARGETS and a not in PROTAGONISTS:
            service_ok.append((a, b))
        elif a not in PROTAGONISTS and b not in PROTAGONISTS:
            mutual_suspect.append((a, b, "配角↔配角单向，可能应互逆（家人/同僚/师徒）"))
        else:
            mutual_suspect.append((a, b, "其他单向"))

    # empty + in a soft cluster of size>=2 → high severity
    empty_high = []
    empty_low = []
    soft_sizes = {fingerprint(e, False): len(soft[fingerprint(e, False)]) for e in ents}
    for e in empty:
        n = soft_sizes[fingerprint(e, False)]
        if n >= 2:
            empty_high.append((e, n))
        else:
            empty_low.append(e)

    lines: list[str] = []
    lines.append("# 甄嬛传题包 · 可玩性审计（关系网 / 区分度）\n")
    lines.append("## 游戏要达成什么\n")
    lines.append(
        "猜人是靠列反馈收敛：性别、身份、位分、住所、重大剧情、关系网。"
        "同一指纹下（例如多个「男/太医/宫中」）必须靠**剧情或关系网**分开；"
        "答案方关系网为空时，关系网列恒为灰，同指纹角色会无法收敛。\n"
    )
    lines.append(
        "约定：主角关系网可以不装下所有人；**从属→主子**可以单向；"
        "父子/夫妻/师徒/同级同僚应尽量双向；小角色自己不能空网（尤其有双胞胎同伴时）。\n"
    )

    lines.append("## A. 关系网为空（高优先：同指纹还有别人）\n")
    if not empty_high:
        lines.append("无。\n")
    else:
        lines.append("| 人物 | 指纹同伴数 | 性别/身份/位分/住所 |\n|---|---:|---|\n")
        for e, n in sorted(empty_high, key=lambda x: (-x[1], x[0]["name"])):
            a = e["attrs"]
            fp = f"{a.get('gender')}/{a.get('role_type')}/{(a.get('titles') or [])}/{(a.get('residences') or [])}"
            lines.append(f"| {e['name']} | {n} | `{fp}` |\n")

    lines.append("\n## B. 关系网为空（低优先：指纹相对独特）\n")
    if not empty_low:
        lines.append("无。\n")
    else:
        lines.append(", ".join(e["name"] for e in empty_low) + "\n")

    lines.append("\n## C. 硬不可分（性别+身份+位分+住所+剧情相同，且关系网集合也相同）\n")
    if not inseparable:
        lines.append("无。\n")
    else:
        for key, ckey, group in sorted(inseparable, key=lambda x: -len(x[2])):
            g, role, titles, res, plots = key
            names = "、".join(e["name"] for e in group)
            conn = "、".join(ckey) if ckey else "（空）"
            lines.append(
                f"- **{names}** — `{g}/{role}` 位分={list(titles)} 住所={list(res)} "
                f"剧情={list(plots) or '-'} 关系网=`{conn}`\n"
            )

    lines.append("\n## D. 软双胞胎（性别+身份+位分+住所相同，剧情可能不同）里仍空网的人\n")
    soft_empty_notes = []
    for key, group in soft.items():
        if len(group) < 2:
            continue
        empties = [e for e in group if not (e["attrs"].get("connections") or [])]
        if not empties:
            continue
        g, role, titles, res = key
        soft_empty_notes.append(
            (len(group), g, role, titles, res, empties, group)
        )
    for n, g, role, titles, res, empties, group in sorted(soft_empty_notes, key=lambda x: -x[0]):
        lines.append(
            f"\n### `{g}/{role}` 位分={list(titles)} 住所={list(res)}（共 {n} 人）\n"
        )
        for e in group:
            cn = conn_names(e, by_id)
            pl = e["attrs"].get("major_plots") or []
            mark = "⚠空网" if e in empties else "ok"
            lines.append(
                f"- [{mark}] **{e['name']}** 剧情={pl or '-'} 关系网={cn or '[]'}\n"
            )

    lines.append("\n## E. 单向边（需你拍板）\n")
    lines.append(
        f"统计：单向 {len(one_way)} 条；其中像「从属→主子」的 {len(service_ok)} 条；"
        f"建议人工看的 {len(mutual_suspect)} 条。\n"
    )
    lines.append("\n### 可疑单向（请回答：应互逆 / 保持单向 / 删边）\n")
    # de-noise: only show non-service-looking, cap list
    show = mutual_suspect[:80]
    if not show:
        lines.append("无。\n")
    else:
        lines.append("| 从 | 到 | 原因提示 | 你的决定 |\n|---|---|---|---|\n")
        for a, b, why in show:
            lines.append(f"| {a} | {b} | {why} |  |\n")
        if len(mutual_suspect) > 80:
            lines.append(f"\n…其余 {len(mutual_suspect) - 80} 条见脚本完整输出。\n")

    lines.append("\n## F. 请你重点回答的问题（不确定项）\n")
    lines.append(
        "下面这些会直接决定下一轮怎么改数据。你可以用「人名：应挂谁」回复。\n"
    )

    # Build concrete questions from worst clusters
    q = 1
    # 宫女大团
    maid_key = ("女", "宫女", ("宫女",), ("宫中",), ())
    if maid_key in hard and len(hard[maid_key]) >= 2:
        lines.append(
            f"\n### Q{q}. 一堆宫中宫女几乎无法区分\n"
            f"人物：{'、'.join(e['name'] for e in hard[maid_key])}\n"
            "问题：哪些可以保留在题池？各自主要服侍谁？"
            "（例：斐雯→？  不重要的是否应从 entities 删除/不进答案池？）\n"
        )
        q += 1

    # 嬷嬷
    lines.append(
        f"\n### Q{q}. 刘嬷嬷 / 张嬷嬷 / 精奇嬷嬷\n"
        "目前全空网。各自属于谁身边？要不要进答案池？\n"
    )
    q += 1

    lines.append(
        f"\n### Q{q}. 太监空网：苟总管、梁多瑞、小荷子、小卫子\n"
        "各自跟谁？梁多瑞是否应挂甄嬛/甘露寺线？\n"
    )
    q += 1

    lines.append(
        f"\n### Q{q}. 甘露寺 莫言\n"
        "与静岸/静白同指纹，莫言空网。莫言应挂谁（静白/甄嬛/…）？\n"
    )
    q += 1

    lines.append(
        f"\n### Q{q}. 答应：孙答应、芝答应\n"
        "空网。是否值得保留？若保留各挂谁？\n"
    )
    q += 1

    lines.append(
        f"\n### Q{q}. 延禧宫 宝鹊\n"
        "空网（注意还有宝鹃）。宝鹊是谁身边的？是否与宝鹃重复？\n"
    )
    q += 1

    lines.append(
        f"\n### Q{q}. 江福海 vs 周宁海\n"
        "同为翊坤宫太监且都只挂年世兰——猜到这一层仍分不开。"
        "有没有额外关系/剧情能拆开？没有的话是否合并/只留一个进答案池？\n"
    )
    q += 1

    lines.append(
        "\n---\n生成命令：`python3 scripts/audit_zhenhuan_playability.py`\n"
    )

    OUT.write_text("".join(lines), encoding="utf-8")
    print(f"wrote {OUT}")
    print(f"empty_high={len(empty_high)} empty_low={len(empty_low)} "
          f"inseparable_groups={len(inseparable)} one_way={len(one_way)} "
          f"suspect={len(mutual_suspect)}")


if __name__ == "__main__":
    main()
