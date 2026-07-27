#!/usr/bin/env python3
"""S9: publish entities.draft.json → packs/{packId}/entities.json."""
from __future__ import annotations

import argparse
import json
import shutil
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from lib.config import load_book_config, resolve_path


def clean_entity(e: dict) -> dict:
    attrs = dict(e.get("attrs") or {})
    # omit null seat for non-108
    if attrs.get("seat") is None:
        attrs.pop("seat", None)
    out = {
        "id": e["id"],
        "name": e["name"],
        "aliases": e.get("aliases") or [],
        "summary": (e.get("summary") or "").strip(),
        "attrs": attrs,
    }
    return out


def main() -> None:
    ap = argparse.ArgumentParser(description="S9 publish pack")
    ap.add_argument("--book", default="shuihu")
    args = ap.parse_args()
    cfg = load_book_config(args.book)

    draft = resolve_path(cfg, "roster_dir") / "entities.draft.json"
    if not draft.is_file():
        raise SystemExit(f"缺少 {draft}")

    pack_dir = resolve_path(cfg, "pack_dir")
    pack_dir.mkdir(parents=True, exist_ok=True)
    for req in ("pack.yaml", "schema.yaml"):
        if not (pack_dir / req).is_file():
            raise SystemExit(f"缺少 {pack_dir / req}，请先完成 S0")

    raw = json.loads(draft.read_text(encoding="utf-8"))
    entities = [clean_entity(e) for e in raw]
    entities.sort(
        key=lambda e: (
            0 if e["attrs"].get("seat") is not None else 1,
            e["attrs"].get("seat") or 999,
            e["name"],
        )
    )

    out = pack_dir / "entities.json"
    # backup previous
    if out.is_file():
        shutil.copy2(out, out.with_suffix(".json.bak"))
    out.write_text(json.dumps(entities, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    world_id = cfg.get("world_id") or args.book
    pack_id = cfg.get("pack_id")
    print(f"wrote {out} entities={len(entities)}")
    print("next:")
    print(f"  1) worlds.js 中 {world_id} → packId={pack_id}, ready=true")
    print("  2) 重启 Spring Boot 加载题包")
    print(f"  3) 百科: /collection.html?world={world_id}")
    print(f"  4) 猜局(隐藏): /vault/q/2026/zh-collection/play.html?pack={pack_id}")


if __name__ == "__main__":
    main()
