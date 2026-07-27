"""Load book YAML config with optional *.local.yaml and ${ENV} expansion."""
from __future__ import annotations

import os
import re
from pathlib import Path
from typing import Any

import yaml

REPO_ROOT = Path(__file__).resolve().parents[3]
PIPELINE_ROOT = Path(__file__).resolve().parents[1]
CONFIG_DIR = PIPELINE_ROOT / "config"

_ENV_PATTERN = re.compile(r"\$\{([A-Za-z_][A-Za-z0-9_]*)\}")


def _expand_env(value: str) -> str:
    def repl(m: re.Match[str]) -> str:
        key = m.group(1)
        return os.environ.get(key, "")

    return _ENV_PATTERN.sub(repl, value)


def _expand_obj(obj: Any) -> Any:
    if isinstance(obj, str):
        return _expand_env(obj)
    if isinstance(obj, list):
        return [_expand_obj(x) for x in obj]
    if isinstance(obj, dict):
        return {k: _expand_obj(v) for k, v in obj.items()}
    return obj


def _deep_merge(base: dict, overlay: dict) -> dict:
    out = dict(base)
    for k, v in overlay.items():
        if k in out and isinstance(out[k], dict) and isinstance(v, dict):
            out[k] = _deep_merge(out[k], v)
        else:
            out[k] = v
    return out


def load_book_config(book: str) -> dict[str, Any]:
    base_path = CONFIG_DIR / f"{book}.yaml"
    if not base_path.is_file():
        raise FileNotFoundError(f"Missing book config: {base_path}")
    cfg = yaml.safe_load(base_path.read_text(encoding="utf-8")) or {}
    local_path = CONFIG_DIR / f"{book}.local.yaml"
    if local_path.is_file():
        local = yaml.safe_load(local_path.read_text(encoding="utf-8")) or {}
        cfg = _deep_merge(cfg, local)
    cfg = _expand_obj(cfg)
    cfg["_repo_root"] = str(REPO_ROOT)
    cfg["_book"] = book
    return cfg


def repo_path(cfg: dict[str, Any], *parts: str) -> Path:
    return Path(cfg["_repo_root"]).joinpath(*parts)


def resolve_path(cfg: dict[str, Any], key: str) -> Path:
    """Resolve paths.* entry relative to repo root unless absolute."""
    rel = cfg["paths"][key]
    p = Path(rel)
    if p.is_absolute():
        return p
    return repo_path(cfg, rel)


def corpus_dir(cfg: dict[str, Any]) -> Path:
    raw = cfg.get("corpus_dir") or ""
    p = Path(raw)
    if not raw or str(p) in (".",):
        raise SystemExit(
            "corpus_dir 未配置。请设置 NOVEL_CORPUS_ROOT，或创建 "
            f"scripts/novel_pipeline/config/{cfg['_book']}.local.yaml"
        )
    if not p.is_dir():
        raise SystemExit(f"正文目录不存在: {p}\n请检查本机小说路径或 local yaml。")
    return p


def chapter_path(cfg: dict[str, Any], chapter: int) -> Path:
    pattern = cfg.get("chapter_glob") or "chapter{num:03d}.md"
    # support both {num:03d} style and plain format
    if "{num:" in pattern:
        name = pattern.replace("{num:03d}", f"{chapter:03d}")
    else:
        name = pattern.format(num=chapter)
    return corpus_dir(cfg) / name
