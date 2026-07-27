#!/usr/bin/env python3
"""S4: enrich entity attrs via DeepSeek (resumable)."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import yaml

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from lib.config import load_book_config, resolve_path
from lib.deepseek_client import DeepSeekClient

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


def load_prompt() -> str:
    p = Path(__file__).resolve().parents[1] / "prompts" / "s4_enrich_system.txt"
    return p.read_text(encoding="utf-8")


def build_name_index(entities: list[dict]) -> dict[str, str]:
    idx: dict[str, str] = {}
    for e in entities:
        idx[e["name"]] = e["id"]
        for a in e.get("aliases") or []:
            if a and a not in idx:
                idx[a] = e["id"]
    return idx


def resolve_connections(names: list[str], name_index: dict[str, str], self_id: str) -> list[str]:
    out: list[str] = []
    seen = {self_id}
    for n in names or []:
        n = (n or "").strip()
        if not n:
            continue
        cid = name_index.get(n)
        if not cid or cid in seen:
            continue
        seen.add(cid)
        out.append(cid)
        if len(out) >= 8:
            break
    return out


def filter_keys(values: list, allowed: set[str]) -> list[str]:
    out = []
    for v in values or []:
        v = str(v).strip()
        if v in allowed and v not in out:
            out.append(v)
    return out


def is_done(entity: dict) -> bool:
    meta = entity.get("_meta") or {}
    if meta.get("s4_done"):
        return True
    attrs = entity.get("attrs") or {}
    # consider done if camps filled (primary enrichment signal)
    return bool(attrs.get("camps"))


def sort_entities(entities: list[dict]) -> list[dict]:
    def key(e: dict):
        seat = (e.get("attrs") or {}).get("seat")
        ch = ((e.get("_meta") or {}).get("chapter_count") or 0)
        # 108 first (by seat), then high chapter count
        return (0 if seat is not None else 1, seat or 999, -ch, e.get("name") or "")

    return sorted(entities, key=key)


def enrich_one(
    client: DeepSeekClient,
    system: str,
    entity: dict,
    name_index: dict[str, str],
) -> dict:
    meta = entity.get("_meta") or {}
    user = {
        "id": entity["id"],
        "name": entity["name"],
        "aliases": entity.get("aliases") or [],
        "star": (entity.get("attrs") or {}).get("star"),
        "seat": (entity.get("attrs") or {}).get("seat"),
        "gender_hint": (entity.get("attrs") or {}).get("gender"),
        "chapter_count": meta.get("chapter_count"),
        "places_seen": meta.get("places_seen") or [],
        "plot_hooks_seen": meta.get("plot_hooks_seen") or [],
        "relations_seen": meta.get("relations_seen") or [],
    }
    data = client.chat_json(
        system=system,
        user="请为下列人物填写属性 json：\n" + json.dumps(user, ensure_ascii=False, indent=2),
        hard=False,
    )
    attrs = dict(entity.get("attrs") or {})
    gender = data.get("gender") or attrs.get("gender")
    if gender in ("男", "女"):
        attrs["gender"] = gender
    attrs["camps"] = filter_keys(data.get("camps") or [], CAMP_KEYS)
    attrs["regions"] = filter_keys(data.get("regions") or [], REGION_KEYS)[:5]
    attrs["major_plots"] = filter_keys(data.get("major_plots") or [], PLOT_KEYS)[:4]
    attrs["connections"] = resolve_connections(
        data.get("connection_names") or [], name_index, entity["id"]
    )
    # never overwrite star/seat from model
    entity["attrs"] = attrs
    meta = dict(meta)
    meta["s4_done"] = True
    meta["s4_confidence"] = data.get("confidence") or {}
    meta["s4_notes"] = data.get("notes") or ""
    entity["_meta"] = meta
    return entity


def save(path: Path, entities: list[dict]) -> None:
    path.write_text(json.dumps(entities, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    ap = argparse.ArgumentParser(description="S4 enrich attrs")
    ap.add_argument("--book", default="shuihu")
    ap.add_argument("--id", help="single entity id")
    ap.add_argument("--limit", type=int, default=0, help="max entities this run (0=all pending)")
    ap.add_argument("--force", action="store_true", help="re-enrich even if done")
    args = ap.parse_args()

    cfg = load_book_config(args.book)
    roster_dir = resolve_path(cfg, "roster_dir")
    draft_path = roster_dir / "entities.draft.json"
    if not draft_path.is_file():
        raise SystemExit(f"缺少 {draft_path}，请先跑 S3")

    entities = json.loads(draft_path.read_text(encoding="utf-8"))
    by_id = {e["id"]: i for i, e in enumerate(entities)}
    name_index = build_name_index(entities)
    client = DeepSeekClient(cfg.get("deepseek") or {})
    system = load_prompt()

    if args.id:
        if args.id not in by_id:
            raise SystemExit(f"未知 id: {args.id}")
        todo = [entities[by_id[args.id]]]
    else:
        ordered = sort_entities(entities)
        todo = [e for e in ordered if args.force or not is_done(e)]
        if args.limit and args.limit > 0:
            todo = todo[: args.limit]

    print(f"pending={len(todo)} total={len(entities)}", flush=True)
    failures = []
    for n, ent in enumerate(todo, 1):
        try:
            enrich_one(client, system, ent, name_index)
            # write-through for resume
            entities[by_id[ent["id"]]] = ent
            if n % 1 == 0:
                save(draft_path, entities)
            camps = ent["attrs"].get("camps")
            print(
                f"[{n}/{len(todo)}] {ent['id']} {ent['name']} camps={camps} "
                f"plots={ent['attrs'].get('major_plots')}",
                flush=True,
            )
        except Exception as e:  # noqa: BLE001
            msg = f"{type(e).__name__}: {e}"
            print(f"FAIL {ent['id']}: {msg}", flush=True)
            failures.append({"id": ent["id"], "error": msg})
            save(draft_path, entities)

    save(draft_path, entities)
    done = sum(1 for e in entities if is_done(e))
    print(f"done_flag={done}/{len(entities)} failures={len(failures)}", flush=True)
    if failures:
        fail_path = roster_dir / "s4_failures.json"
        fail_path.write_text(json.dumps(failures, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"failures → {fail_path}", flush=True)
        sys.exit(1)


if __name__ == "__main__":
    main()
