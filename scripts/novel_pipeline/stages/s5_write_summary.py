#!/usr/bin/env python3
"""S5: write entity summaries via DeepSeek (≤50 chars, resumable)."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from lib.config import load_book_config, resolve_path
from lib.deepseek_client import DeepSeekClient


def load_prompt() -> str:
    return (
        Path(__file__).resolve().parents[1] / "prompts" / "s5_summary_system.txt"
    ).read_text(encoding="utf-8")


def char_len(s: str) -> int:
    return len(s or "")


def is_done(entity: dict) -> bool:
    meta = entity.get("_meta") or {}
    if meta.get("s5_done"):
        return True
    s = (entity.get("summary") or "").strip()
    return bool(s) and char_len(s) <= 50


def sort_entities(entities: list[dict]) -> list[dict]:
    def key(e: dict):
        seat = (e.get("attrs") or {}).get("seat")
        ch = ((e.get("_meta") or {}).get("chapter_count") or 0)
        return (0 if seat is not None else 1, seat or 999, -ch, e.get("name") or "")

    return sorted(entities, key=key)


def clip_summary(s: str) -> str:
    s = (s or "").strip().replace("\n", "")
    if char_len(s) <= 50:
        return s
    return s[:50]


def enrich_one(client: DeepSeekClient, system: str, entity: dict) -> None:
    attrs = entity.get("attrs") or {}
    meta = entity.get("_meta") or {}
    user = {
        "id": entity["id"],
        "name": entity["name"],
        "aliases": entity.get("aliases") or [],
        "gender": attrs.get("gender"),
        "camps": attrs.get("camps"),
        "star": attrs.get("star"),
        "seat": attrs.get("seat"),
        "regions": attrs.get("regions"),
        "major_plots": attrs.get("major_plots"),
        "plot_hooks_seen": meta.get("plot_hooks_seen") or [],
    }
    data = client.chat_json(
        system=system,
        user="请为下列人物写简介 json：\n" + json.dumps(user, ensure_ascii=False, indent=2),
        hard=False,
        max_tokens=1024,
    )
    summary = clip_summary(data.get("summary") or "")
    entity["summary"] = summary
    meta = dict(meta)
    meta["s5_done"] = True
    meta["s5_confidence"] = data.get("confidence")
    entity["_meta"] = meta


def save(path: Path, entities: list[dict]) -> None:
    path.write_text(json.dumps(entities, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    ap = argparse.ArgumentParser(description="S5 write summaries")
    ap.add_argument("--book", default="shuihu")
    ap.add_argument("--id")
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--force", action="store_true")
    args = ap.parse_args()

    cfg = load_book_config(args.book)
    roster_dir = resolve_path(cfg, "roster_dir")
    draft_path = roster_dir / "entities.draft.json"
    entities = json.loads(draft_path.read_text(encoding="utf-8"))
    by_id = {e["id"]: i for i, e in enumerate(entities)}
    client = DeepSeekClient(cfg.get("deepseek") or {})
    system = load_prompt()

    if args.id:
        todo = [entities[by_id[args.id]]]
    else:
        todo = [e for e in sort_entities(entities) if args.force or not is_done(e)]
        if args.limit:
            todo = todo[: args.limit]

    print(f"pending={len(todo)} total={len(entities)}", flush=True)
    failures = []
    for n, ent in enumerate(todo, 1):
        try:
            enrich_one(client, system, ent)
            entities[by_id[ent["id"]]] = ent
            save(draft_path, entities)
            print(
                f"[{n}/{len(todo)}] {ent['name']} ({char_len(ent['summary'])}) {ent['summary']}",
                flush=True,
            )
        except Exception as e:  # noqa: BLE001
            print(f"FAIL {ent['id']}: {e}", flush=True)
            failures.append({"id": ent["id"], "error": str(e)})
            save(draft_path, entities)

    done = sum(1 for e in entities if is_done(e))
    print(f"done={done}/{len(entities)} failures={len(failures)}", flush=True)
    if failures:
        (roster_dir / "s5_failures.json").write_text(
            json.dumps(failures, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )
        sys.exit(1)


if __name__ == "__main__":
    main()
