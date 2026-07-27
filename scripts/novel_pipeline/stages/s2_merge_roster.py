#!/usr/bin/env python3
"""S2: merge chapter extracts into roster/candidates.json."""
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from lib.config import load_book_config, resolve_path
from lib.inclusion import is_personal_name

# Aliases too generic to use for identity merge / binding
GENERIC_ALIASES = frozenset(
    {
        "哥哥",
        "兄弟",
        "大哥",
        "二哥",
        "大郎",
        "二郎",
        "娘子",
        "婆子",
        "妇人",
        "那妇人",
        "那汉子",
        "好汉",
        "头领",
        "大王",
        "太尉",
        "太师",
        "枢密",
        "相公",
        "恩相",
        "官人",
        "客官",
        "保正",
        "教师",
        "教头",
        "都头",
        "押司",
        "员外",
        "太守",
        "知寨",
        "干娘",
        "老咬虫",
        "婆娘",
        "那贱人",
        "那婆娘",
        "寡人",
        "圣上",
        "上皇",
        "反贼",
        "统军",
        "总管",
        "法师",
        "国师",
        "郡主",
        "夫人",
        "老婆",
        "义士",
        "兄长",
        "叔叔",
        "嫂嫂",
        "那厮",
        "淫妇",
        "副排军",
        "王子",
        "贼首",
        "草头大王",
        "李大郎",
        "打虎武松",
    }
)

def load_stars(path: Path) -> list[dict]:
    if not path.is_file():
        return []
    return json.loads(path.read_text(encoding="utf-8"))


def build_canon_maps(stars: list[dict]) -> tuple[dict[str, dict], dict[str, dict]]:
    """Return (by_name, by_strong_alias) maps. Strong alias = nickname or listed aliases."""
    by_name: dict[str, dict] = {}
    by_alias: dict[str, dict] = {}
    for s in stars:
        by_name[s["name"]] = s
        nick = (s.get("nickname") or "").strip()
        if nick and nick not in GENERIC_ALIASES and nick not in {"行者"}:
            by_alias[nick] = s
        for a in s.get("aliases") or []:
            a = (a or "").strip()
            if a and a not in GENERIC_ALIASES:
                by_alias[a] = s
    return by_name, by_alias


def slugify(name: str) -> str:
    latin = re.sub(r"[^a-z0-9]+", "", name.lower())
    if latin and len(latin) >= 2:
        return latin
    cps = "".join(f"{ord(c):x}" for c in name[:6])
    return f"u{cps}" if cps else "unknown"


def importance_rank(imp: str) -> int:
    return {"major": 0, "supporting": 1, "extra": 2}.get(imp or "extra", 9)


def filter_aliases(name: str, aliases: list[str]) -> list[str]:
    out = []
    seen = {name}
    for a in aliases:
        a = (a or "").strip()
        if not a or a in seen or a in GENERIC_ALIASES:
            continue
        if not is_personal_name(a) and len(a) <= 2:
            continue
        seen.add(a)
        out.append(a)
    return out


class Counterish:
    def __init__(self):
        self._c: dict[str, int] = defaultdict(int)

    def add(self, k: str) -> None:
        self._c[k] += 1

    def most_common(self) -> str:
        if not self._c:
            return ""
        return max(self._c.items(), key=lambda kv: kv[1])[0]


def _dedupe_rels(rels: list[dict]) -> list[dict]:
    seen = set()
    out = []
    for r in rels:
        key = (r.get("other"), r.get("type"))
        if key in seen:
            continue
        seen.add(key)
        out.append({"other": r["other"], "type": r.get("type") or ""})
        if len(out) >= 40:
            break
    return out


def resolve_canon(surface: str, aliases: list[str], by_name: dict, by_alias: dict) -> dict | None:
    if surface in by_name:
        return by_name[surface]
    # surface is a strong alias of a star
    if surface in by_alias:
        return by_alias[surface]
    for a in aliases:
        if a in by_name:
            return by_name[a]
    for a in aliases:
        if a in by_alias:
            return by_alias[a]
    return None


def merge_candidates(extract_dir: Path, stars: list[dict], keep_imp: set[str]) -> list[dict]:
    by_name, by_alias = build_canon_maps(stars)
    # Cluster key = canon id OR exact primary surface name
    clusters: dict[str, dict] = {}

    def ensure_cluster(surface: str, aliases: list[str]) -> dict:
        star = resolve_canon(surface, aliases, by_name, by_alias)
        if star:
            key = star["id"]
            if key not in clusters:
                base_aliases = set(filter_aliases(star["name"], [star.get("nickname") or "", *(star.get("aliases") or [])]))
                clusters[key] = {
                    "id": star["id"],
                    "name": star["name"],
                    "aliases": base_aliases,
                    "star": star,
                    "importance": "supporting",
                    "chapters": set(),
                    "gender_hints": Counterish(),
                    "places": set(),
                    "plot_hooks": set(),
                    "relations": [],
                    "surfaces_seen": set(),
                }
            return clusters[key]

        # Non-canon: cluster by surface string only (do not merge via shared weak aliases)
        key = f"name:{surface}"
        if key not in clusters:
            clusters[key] = {
                "id": f"sh_{slugify(surface)}",
                "name": surface,
                "aliases": set(),
                "star": None,
                "importance": "supporting",
                "chapters": set(),
                "gender_hints": Counterish(),
                "places": set(),
                "plot_hooks": set(),
                "relations": [],
                "surfaces_seen": set(),
            }
        return clusters[key]

    for fp in sorted(extract_dir.glob("*.json")):
        data = json.loads(fp.read_text(encoding="utf-8"))
        chapter = data.get("chapter") or int(fp.stem)
        for m in data.get("mentions") or []:
            imp = m.get("importance") or "extra"
            surface = (m.get("surface") or "").strip()
            if not surface or not is_personal_name(surface):
                continue
            if imp not in keep_imp:
                continue

            raw_aliases = [a.strip() for a in (m.get("aliases_seen") or []) if a and str(a).strip()]
            aliases = filter_aliases(surface, raw_aliases)
            c = ensure_cluster(surface, aliases)

            c["chapters"].add(chapter)
            c["surfaces_seen"].add(surface)
            if surface != c["name"] and surface not in GENERIC_ALIASES:
                # only attach surface as alias if it resolves to this cluster's canon
                # or is clearly alternate of same person (for non-canon, surface==name already)
                if c.get("star"):
                    if surface in {c["name"], *(c.get("star").get("aliases") or []), c["star"].get("nickname")}:
                        c["aliases"].add(surface)
                    elif resolve_canon(surface, [], by_name, by_alias) is c.get("star"):
                        c["aliases"].add(surface)
                # non-canon: surface is the name key; extra surfaces shouldn't join this cluster
            for a in aliases:
                if a != c["name"]:
                    # only keep aliases that don't point to a *different* canon person
                    other = resolve_canon(a, [], by_name, by_alias)
                    if other and c.get("star") and other["id"] != c["star"]["id"]:
                        continue
                    if other and not c.get("star"):
                        continue
                    c["aliases"].add(a)

            if importance_rank(imp) < importance_rank(c["importance"]):
                c["importance"] = imp
            gh = (m.get("gender_hint") or "").strip()
            if gh:
                c["gender_hints"].add(gh)
            for p in m.get("places") or []:
                if p:
                    c["places"].add(str(p).strip())
            for ph in m.get("plot_hooks") or []:
                if ph:
                    c["plot_hooks"].add(str(ph).strip())
            for rel in m.get("relations") or []:
                if not isinstance(rel, dict):
                    continue
                other = (rel.get("other") or "").strip()
                if not other or other in GENERIC_ALIASES:
                    continue
                c["relations"].append(
                    {"other": other, "type": rel.get("type") or "", "chapter": chapter}
                )

    out: list[dict] = []
    for c in clusters.values():
        star = c.get("star")
        item = {
            "id": c["id"],
            "name": c["name"],
            "aliases": sorted(a for a in c["aliases"] if a and a != c["name"]),
            "importance": c["importance"],
            "chapter_count": len(c["chapters"]),
            "chapters": sorted(c["chapters"]),
            "gender_hint": c["gender_hints"].most_common(),
            "places_seen": sorted(c["places"])[:30],
            "plot_hooks_seen": sorted(c["plot_hooks"])[:40],
            "relations_seen": _dedupe_rels(c["relations"]),
            "canon": None,
        }
        if star:
            item["canon"] = {
                "seat": star["seat"],
                "star": star["star"],
                "nickname": star.get("nickname"),
            }
        out.append(item)

    have_ids = {x["id"] for x in out}
    for s in stars:
        if s["id"] in have_ids:
            continue
        aliases = filter_aliases(s["name"], [s.get("nickname") or "", *(s.get("aliases") or [])])
        out.append(
            {
                "id": s["id"],
                "name": s["name"],
                "aliases": aliases,
                "importance": "supporting",
                "chapter_count": 0,
                "chapters": [],
                "gender_hint": "",
                "places_seen": [],
                "plot_hooks_seen": [],
                "relations_seen": [],
                "canon": {"seat": s["seat"], "star": s["star"], "nickname": s.get("nickname")},
                "note": "injected_from_canon",
            }
        )

    out.sort(key=lambda x: (-(1 if x.get("canon") else 0), -(x["chapter_count"]), x["name"]))
    return out


def main() -> None:
    ap = argparse.ArgumentParser(description="S2 merge roster")
    ap.add_argument("--book", default="shuihu")
    args = ap.parse_args()
    cfg = load_book_config(args.book)
    keep = set((cfg.get("inclusion") or {}).get("keep_importance") or ["major", "supporting"])
    extract_dir = resolve_path(cfg, "chapter_extract")
    stars_path = resolve_path(cfg, "canon_stars")
    stars = load_stars(stars_path)
    if len(stars) != 108:
        print(f"warn: canon stars count={len(stars)} (expected 108)", flush=True)

    candidates = merge_candidates(extract_dir, stars, keep)
    roster_dir = resolve_path(cfg, "roster_dir")
    roster_dir.mkdir(parents=True, exist_ok=True)
    out_path = roster_dir / "candidates.json"
    payload = {
        "book": args.book,
        "pack_id": cfg.get("pack_id"),
        "candidate_count": len(candidates),
        "canon_matched": sum(1 for c in candidates if c.get("canon")),
        "candidates": candidates,
    }
    out_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(
        f"wrote {out_path} candidates={len(candidates)} "
        f"canon_matched={payload['canon_matched']}",
        flush=True,
    )


if __name__ == "__main__":
    main()
