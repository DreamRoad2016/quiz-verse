#!/usr/bin/env python3
"""S3: build entity shells from candidates.json → entities.draft.json."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from lib.config import load_book_config, resolve_path


def main() -> None:
    ap = argparse.ArgumentParser(description="S3 build entity shells")
    ap.add_argument("--book", default="shuihu")
    args = ap.parse_args()
    cfg = load_book_config(args.book)

    roster_dir = resolve_path(cfg, "roster_dir")
    cand_path = roster_dir / "candidates.json"
    if not cand_path.is_file():
        raise SystemExit(f"缺少 {cand_path}，请先跑 S2")

    payload = json.loads(cand_path.read_text(encoding="utf-8"))
    candidates = payload.get("candidates") or []
    entities = []
    for c in candidates:
        gender = c.get("gender_hint") or None
        attrs: dict = {
            "gender": gender,
            "camps": [],
            "star": "非星",
            "regions": [],
            "major_plots": [],
            "connections": [],
        }
        canon = c.get("canon")
        if canon:
            attrs["star"] = canon.get("star") or "非星"
            if canon.get("seat") is not None:
                attrs["seat"] = canon["seat"]
        # non-108: omit seat key entirely
        entity = {
            "id": c["id"],
            "name": c["name"],
            "aliases": c.get("aliases") or [],
            "summary": "",
            "attrs": attrs,
            "_meta": {
                "importance": c.get("importance"),
                "chapter_count": c.get("chapter_count"),
                "places_seen": c.get("places_seen") or [],
                "plot_hooks_seen": c.get("plot_hooks_seen") or [],
                "relations_seen": c.get("relations_seen") or [],
            },
        }
        entities.append(entity)

    entities.sort(key=lambda e: (0 if e["attrs"].get("seat") else 1, e["attrs"].get("seat") or 999, e["name"]))
    out_path = roster_dir / "entities.draft.json"
    out_path.write_text(json.dumps(entities, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    with_seat = sum(1 for e in entities if "seat" in e["attrs"])
    print(
        f"wrote {out_path} entities={len(entities)} with_seat={with_seat} non108={len(entities) - with_seat}",
        flush=True,
    )


if __name__ == "__main__":
    main()
