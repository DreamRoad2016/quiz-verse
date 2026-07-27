"""Inclusion rules: personal name required for roster entities."""
from __future__ import annotations

import re

# Exact role/group labels that are NOT personal names (S1 post-filter + S2).
ROLE_ONLY_SURFACES = frozenset(
    {
        "酒家",
        "店主人",
        "主人家",
        "酒保",
        "店小二",
        "知县",
        "知县相公",
        "县尉",
        "府尹",
        "太尉",
        "猎户",
        "乡夫",
        "上户",
        "庄客",
        "军士",
        "军汉",
        "小卒",
        "士兵",
        "梢公",
        "艄公",
        "水手",
        "道人",
        "和尚",
        "行者",  # alone — 「武行者」走别名校验
        "头陀",
        "客官",
        "那汉",
        "那汉子",
        "妇人",
        "一个妇人",
        "大虫",  # 虎，非人
    }
)

# surface 匹配这些模式则视为非专名（群体/纯职）
_ROLE_PATTERNS = [
    re.compile(r"^[一二两三四四五六七八九十百]+个?(猎户|庄客|军汉|军士|乡夫|小卒)"),
    re.compile(r"^众(人|军|庄客|猎户)"),
    re.compile(r"^(几个|数个|许多).+"),
]


def is_personal_name(surface: str) -> bool:
    """True if surface looks like a roster-worthy personal name."""
    s = (surface or "").strip()
    if not s:
        return False
    if s in ROLE_ONLY_SURFACES:
        return False
    for pat in _ROLE_PATTERNS:
        if pat.match(s):
            return False
    # 单字职衔
    if s in {"官", "吏", "兵", "僧", "尼", "盗", "贼"}:
        return False
    return True


def demote_non_personal(mention: dict) -> dict:
    """If model marked role-only as major/supporting, force extra."""
    out = dict(mention)
    surface = out.get("surface") or ""
    if not is_personal_name(surface):
        out["importance"] = "extra"
        out["inclusion_note"] = "非个人姓名（身份/群体称谓），不进名册"
    return out
