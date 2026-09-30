#!/usr/bin/env python3
"""Lint / scrub history day JSON for basic quality (subject, tags, date).

Usage:
  python scripts/history_demo_lint.py           # report only
  python scripts/history_demo_lint.py --fix    # drop bad rows & rewrite days/
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO_ROOT / "scripts"))

from history_demo_generate import (  # noqa: E402
    ALLOWED_TAGS,
    OUT_DIR,
    assign_ids,
    day_key,
    validate_event,
)

DAYS = OUT_DIR


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--fix", action="store_true", help="删除不合格条目并回写")
    args = parser.parse_args()

    total_drop = 0
    total_keep = 0
    for path in sorted(DAYS.glob("*.json")):
        m = re.fullmatch(r"(\d{2})-(\d{2})\.json", path.name)
        if not m:
            continue
        month, day = int(m.group(1)), int(m.group(2))
        events = json.loads(path.read_text(encoding="utf-8"))
        kept = []
        dropped_here = 0
        for e in events:
            err = validate_event(e, month, day)
            if err:
                print(f"{path.name}: DROP [{e.get('title')}] ({err})")
                total_drop += 1
                dropped_here += 1
            else:
                kept.append(e)
                total_keep += 1
        if args.fix and dropped_here:
            final = assign_ids(
                [{k: v for k, v in e.items() if k != "eventId"} for e in kept],
                month,
                day,
            )
            path.write_text(
                json.dumps(final, ensure_ascii=False, indent=2) + "\n",
                encoding="utf-8",
            )
            print(f"  wrote {path.name}: {len(events)} -> {len(final)}")

    print(f"\nkeep={total_keep} drop={total_drop}")
    if total_drop and not args.fix:
        print("Run with --fix to scrub files.")
        raise SystemExit(1)


if __name__ == "__main__":
    # silence unused
    _ = (ALLOWED_TAGS, day_key)
    main()
