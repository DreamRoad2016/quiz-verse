#!/usr/bin/env python3
"""S1: extract character mentions from one or all chapters via DeepSeek."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

# allow `python scripts/novel_pipeline/stages/s1_....py`
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from lib.config import chapter_path, load_book_config, resolve_path
from lib.deepseek_client import DeepSeekClient
from lib.inclusion import demote_non_personal


def load_prompt() -> str:
    p = Path(__file__).resolve().parents[1] / "prompts" / "s1_extract_system.txt"
    return p.read_text(encoding="utf-8")


def build_user(chapter: int, src_name: str, text: str, *, compact: bool) -> str:
    extra = ""
    if compact:
        extra = (
            "\n【紧凑模式】只输出 major/supporting；不要输出 extra；"
            "evidence 最多20字；aliases_seen/relations/plot_hooks 尽量短。"
            "人物多时宁可少写字段也要保证 JSON 完整。\n"
        )
    return (
        f"请抽取第 {chapter} 回人物提及，输出 json。{extra}\n"
        f"章节文件名: {src_name}\n\n"
        f"正文:\n{text}"
    )


def extract_one(cfg: dict, client: DeepSeekClient, chapter: int, force: bool) -> Path:
    out_dir = resolve_path(cfg, "chapter_extract")
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / f"{chapter:03d}.json"
    if out_path.is_file() and not force:
        print(f"skip existing {out_path}", flush=True)
        return out_path

    src = chapter_path(cfg, chapter)
    if not src.is_file():
        raise SystemExit(f"章节文件不存在: {src}")

    text = src.read_text(encoding="utf-8")
    max_chars = int(cfg.get("s1_max_chars") or 60000)
    if len(text) > max_chars:
        text = text[:max_chars] + "\n\n[正文截断]"

    system = load_prompt()
    # long chapters: start compact to avoid truncated JSON / hung large responses
    start_compact = len(text) >= int(cfg.get("s1_compact_chars") or 28000)
    try:
        data = client.chat_json(
            system=system,
            user=build_user(chapter, src.name, text, compact=start_compact),
            hard=False,
            max_tokens=24576 if start_compact else None,
        )
    except RuntimeError:
        print(f"retry chapter {chapter} in compact mode", flush=True)
        data = client.chat_json(
            system=system,
            user=build_user(chapter, src.name, text, compact=True),
            hard=False,
            max_tokens=24576,
        )

    data["chapter"] = chapter
    data.setdefault("source_file", src.name)
    mentions = [demote_non_personal(m) for m in (data.get("mentions") or [])]
    data["mentions"] = mentions
    kept = sum(1 for m in mentions if m.get("importance") in ("major", "supporting"))
    out_path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {out_path} mentions={len(mentions)} roster_worthy={kept}", flush=True)
    return out_path


def main() -> None:
    ap = argparse.ArgumentParser(description="S1 chapter extract")
    ap.add_argument("--book", default="shuihu")
    ap.add_argument("--chapter", type=int, help="single chapter number")
    ap.add_argument("--all", action="store_true", help="all chapters in config range")
    ap.add_argument("--force", action="store_true")
    args = ap.parse_args()

    if not args.chapter and not args.all:
        ap.error("需要 --chapter N 或 --all")

    cfg = load_book_config(args.book)
    client = DeepSeekClient(cfg.get("deepseek") or {})
    failures: list[tuple[int, str]] = []

    if args.all:
        lo = int(cfg.get("chapter_min") or 1)
        hi = int(cfg.get("chapter_max") or 120)
        for n in range(lo, hi + 1):
            try:
                extract_one(cfg, client, n, args.force)
            except Exception as e:  # noqa: BLE001 — batch must continue
                msg = f"{type(e).__name__}: {e}"
                print(f"FAIL chapter {n}: {msg}", flush=True)
                failures.append((n, msg))
        if failures:
            fail_path = resolve_path(cfg, "chapter_extract").parent / "s1_failures.json"
            fail_path.write_text(
                json.dumps([{"chapter": c, "error": m} for c, m in failures], ensure_ascii=False, indent=2)
                + "\n",
                encoding="utf-8",
            )
            print(f"done with {len(failures)} failures → {fail_path}", flush=True)
            sys.exit(1)
        print("done all chapters ok", flush=True)
    else:
        extract_one(cfg, client, args.chapter, args.force)


if __name__ == "__main__":
    main()
