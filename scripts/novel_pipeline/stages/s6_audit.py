#!/usr/bin/env python3
"""S6: rule audit for shuihu draft entities + schema."""
from __future__ import annotations

import argparse
import json
import sys
from collections import defaultdict
from pathlib import Path

import yaml

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from lib.config import load_book_config, repo_path, resolve_path

CAMP_KEYS = {
    "liangshan", "song_court", "fangla", "wangqing", "tianhu",
    "family", "local_force", "civilian", "liao",
}
REGION_KEYS = {
    "liangshanbo", "bianjing", "yuncheng", "cangzhou", "jiangzhou", "qingfengzhai",
    "erlongshan", "taohuashan", "zhujiazhuang", "zengtoushi", "gaotangzhou", "damingfu",
    "jizhou", "dengzhou", "muzhou", "mengzhou", "yanggu", "chaijinzhuang", "shaozhou",
    "wutaishan", "qinghe", "dongjing_other",
}
PLOT_KEYS = {
    "quanda_zhenguanxi", "daoba_chuiyangliu", "wuru_baihutang", "fengxue_shanshenmiao",
    "huobing_wanglun", "zhiqiu_shengchengang", "jingyanggang_tiger", "xuejian_panjinlian",
    "yuanyanglou", "jiangzhou_jiefachang", "sandazhujiazhuang", "dashidao_gaotangzhou",
    "yingxiong_paizuoci", "zhaoan", "zheng_liao", "zheng_tianhu", "zheng_wangqing",
    "zheng_fangla", "shangliangshan", "other_key_plot",
}
STARS = {"天罡", "地煞", "非星"}


def load_stars(path: Path) -> dict[str, dict]:
    if not path.is_file():
        return {}
    return {s["id"]: s for s in json.loads(path.read_text(encoding="utf-8"))}


def audit(entities: list[dict], stars: dict[str, dict]) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []
    by_id = {e["id"]: e for e in entities}
    ids = set(by_id)

    # unique ids / names
    id_counts: dict[str, int] = defaultdict(int)
    for e in entities:
        id_counts[e["id"]] += 1
    for i, n in id_counts.items():
        if n > 1:
            errors.append(f"duplicate id {i} x{n}")

    for e in entities:
        eid = e["id"]
        name = e.get("name") or ""
        attrs = e.get("attrs") or {}
        summary = (e.get("summary") or "").strip()

        if not name:
            errors.append(f"{eid}: empty name")
        if not attrs.get("gender") in ("男", "女"):
            errors.append(f"{eid} {name}: bad gender {attrs.get('gender')!r}")
        camps = attrs.get("camps") or []
        if not camps:
            errors.append(f"{eid} {name}: empty camps")
        for c in camps:
            if c not in CAMP_KEYS:
                errors.append(f"{eid} {name}: bad camp {c}")
        star = attrs.get("star")
        if star not in STARS:
            errors.append(f"{eid} {name}: bad star {star!r}")
        seat = attrs.get("seat")
        if star in ("天罡", "地煞"):
            if seat is None:
                errors.append(f"{eid} {name}: missing seat for {star}")
            elif not (1 <= int(seat) <= 108):
                errors.append(f"{eid} {name}: seat out of range {seat}")
        else:
            if attrs.get("seat") is not None:
                errors.append(f"{eid} {name}: non-star should omit seat, got {seat}")

        # canon consistency
        if eid in stars:
            s = stars[eid]
            if star != s["star"] or int(seat or -1) != int(s["seat"]):
                errors.append(
                    f"{eid} {name}: canon mismatch star/seat "
                    f"got {star}/{seat} want {s['star']}/{s['seat']}"
                )
            if name != s["name"]:
                warnings.append(f"{eid}: name {name!r} != canon {s['name']!r}")

        for r in attrs.get("regions") or []:
            if r not in REGION_KEYS:
                errors.append(f"{eid} {name}: bad region {r}")
        for p in attrs.get("major_plots") or []:
            if p not in PLOT_KEYS:
                errors.append(f"{eid} {name}: bad plot {p}")
        for cid in attrs.get("connections") or []:
            if cid not in ids:
                errors.append(f"{eid} {name}: dangling connection {cid}")
            elif cid == eid:
                warnings.append(f"{eid} {name}: self connection")

        if summary and len(summary) > 50:
            errors.append(f"{eid} {name}: summary len {len(summary)} > 50")
        if not summary:
            warnings.append(f"{eid} {name}: empty summary")

        # playability soft checks
        if not (attrs.get("major_plots") or []) and not (attrs.get("connections") or []):
            warnings.append(f"{eid} {name}: no plots and no connections")

    # all 108 present
    missing = [sid for sid in stars if sid not in by_id]
    for sid in missing:
        errors.append(f"missing canon entity {sid}")

    return errors, warnings


def main() -> None:
    ap = argparse.ArgumentParser(description="S6 audit")
    ap.add_argument("--book", default="shuihu")
    args = ap.parse_args()
    cfg = load_book_config(args.book)
    draft = resolve_path(cfg, "roster_dir") / "entities.draft.json"
    entities = json.loads(draft.read_text(encoding="utf-8"))
    stars = load_stars(resolve_path(cfg, "canon_stars"))
    errors, warnings = audit(entities, stars)

    out_dir = resolve_path(cfg, "roster_dir")
    report = {
        "entity_count": len(entities),
        "error_count": len(errors),
        "warning_count": len(warnings),
        "errors": errors,
        "warnings": warnings[:200],
        "warnings_truncated": len(warnings) > 200,
    }
    report_path = out_dir / "s6_audit.json"
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    md = [f"# S6 audit\n", f"- entities: {len(entities)}", f"- errors: {len(errors)}", f"- warnings: {len(warnings)}\n"]
    if errors:
        md.append("## Errors\n")
        md.extend(f"- {e}" for e in errors[:100])
    if warnings:
        md.append("\n## Warnings (sample)\n")
        md.extend(f"- {w}" for w in warnings[:50])
    (out_dir / "s6_audit.md").write_text("\n".join(md) + "\n", encoding="utf-8")

    print(f"entities={len(entities)} errors={len(errors)} warnings={len(warnings)}")
    print(f"report → {report_path}")
    if errors:
        for e in errors[:20]:
            print("ERR", e)
        sys.exit(1)


if __name__ == "__main__":
    main()
