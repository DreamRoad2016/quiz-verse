#!/usr/bin/env python3
"""Apply curated plot/connection/role fixes onto zhenhuan entities.json."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENTITIES = ROOT / "src/main/resources/packs/zhenhuan_2011/entities.json"
SEED = ROOT / "scripts/data/zhenhuan_core_seed.json"

# 与 schema.yaml enums.major_plots 对齐（全剧约 10 个）
PLOTS = {
    "chuxuan_dianxuan",
    "jinghong_wu",
    "huanyi_xiaochang",
    "huafei_ci_si",
    "chunyuan_jiufu",
    "ganlu_yunfeng",
    "anling_baiju",
    "dixue_qinzi",
    "heqin_shitan",
    "houwei_zhongju",
}

# 重复条目：保留前者，删除后者（后者常为爵号/位分空壳）
DROP_NAMES = {
    "温宜公主",  # 保留 富察·温宜
    "宁嫔",  # 叶澜依即宁嫔
    "襄嫔",  # 曹贵人后期位分，已在 aliases
    "夏常在",  # 夏冬春
    "敬嫔",  # 敬妃
    "祺嫔",  # 文鸳
    "淳贵人",  # 淳常在
    "余答应",  # 余莺儿
}

# 按姓名覆盖的精修（role/titles/residences/aliases/plots/connections）
# connections 用姓名，写入时解析为 id
BY_NAME: dict[str, dict] = {
    "甄嬛": {
        "aliases": ["嬛嬛", "莞莞", "莫愁", "熹妃", "熹贵妃", "圣母皇太后"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["常在", "贵人", "嫔", "妃", "贵妃", "皇贵妃", "太后"],
            "residences": ["碎玉轩", "甘露寺", "永寿宫", "寿康宫"],
            "major_plots": [
                "chuxuan_dianxuan", "jinghong_wu", "huanyi_xiaochang", "huafei_ci_si",
                "chunyuan_jiufu", "ganlu_yunfeng", "anling_baiju", "dixue_qinzi",
                "heqin_shitan", "houwei_zhongju",
            ],
            "connections": [
                "乌拉那拉·宜修", "年世兰", "安陵容", "沈眉庄", "爱新觉罗·胤禛",
                "允礼", "浣碧", "温实初", "叶澜依", "乌雅氏", "崔槿汐", "纯元皇后",
            ],
        },
    },
    "乌拉那拉·宜修": {
        "aliases": ["皇后", "宜修"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["皇后"],
            "residences": ["景仁宫"],
            "major_plots": [
                "chuxuan_dianxuan", "jinghong_wu", "huafei_ci_si", "chunyuan_jiufu",
                "anling_baiju", "dixue_qinzi", "houwei_zhongju",
            ],
            "connections": [
                "爱新觉罗·胤禛", "甄嬛", "安陵容", "乌雅氏", "剪秋", "富察·温宜", "纯元皇后",
            ],
        },
    },
    "年世兰": {
        "aliases": ["华妃", "年妃", "华贵妃"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["妃", "贵妃", "答应"],
            "residences": ["翊坤宫", "冷宫"],
            "major_plots": ["jinghong_wu", "huanyi_xiaochang", "huafei_ci_si"],
            "connections": ["爱新觉罗·胤禛", "甄嬛", "曹贵人", "年羹尧", "乔颂芝", "沈眉庄"],
        },
    },
    "安陵容": {
        "aliases": ["陵容", "安嫔", "鸝妃"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["答应", "常在", "贵人", "嫔", "妃"],
            "residences": ["延禧宫", "咸福宫"],
            "major_plots": [
                "chuxuan_dianxuan", "jinghong_wu", "huanyi_xiaochang",
                "chunyuan_jiufu", "anling_baiju",
            ],
            "connections": ["甄嬛", "乌拉那拉·宜修", "沈眉庄", "爱新觉罗·胤禛", "安比槐", "宝鹃"],
        },
    },
    "沈眉庄": {
        "aliases": ["眉庄", "惠妃", "惠贵妃"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["贵人", "嫔", "妃", "贵妃"],
            "residences": ["碎玉轩", "咸福宫"],
            "major_plots": [
                "chuxuan_dianxuan", "jinghong_wu", "huafei_ci_si",
                "chunyuan_jiufu", "anling_baiju", "dixue_qinzi",
            ],
            "connections": ["甄嬛", "安陵容", "爱新觉罗·胤禛", "沈自山", "温实初"],
        },
    },
    "叶澜依": {
        "aliases": ["澜依", "宁嫔", "宁贵人"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["答应", "常在", "贵人", "嫔"],
            "residences": ["宫中"],
            "major_plots": ["dixue_qinzi", "heqin_shitan", "houwei_zhongju"],
            "connections": ["爱新觉罗·胤禛", "甄嬛", "乌拉那拉·宜修"],
        },
    },
    "爱新觉罗·胤禛": {
        "aliases": ["皇上", "四爷", "雍正", "胤禛"],
        "attrs": {
            "gender": "男",
            "role_type": "皇族",
            "titles": ["皇帝"],
            "residences": ["养心殿", "乾清宫"],
            "major_plots": [
                "jinghong_wu", "huanyi_xiaochang", "huafei_ci_si", "chunyuan_jiufu",
                "ganlu_yunfeng", "dixue_qinzi", "heqin_shitan", "houwei_zhongju",
            ],
            "connections": [
                "甄嬛", "乌拉那拉·宜修", "年世兰", "乌雅氏", "允礼", "沈眉庄", "安陵容", "纯元皇后",
            ],
        },
    },
    "乌雅氏": {
        "aliases": ["太后", "德妃", "成璧"],
        "attrs": {
            "gender": "女",
            "role_type": "皇族",
            "titles": ["太后"],
            "residences": ["寿康宫"],
            "major_plots": ["huafei_ci_si", "chunyuan_jiufu", "houwei_zhongju"],
            "connections": ["爱新觉罗·胤禛", "乌拉那拉·宜修", "甄嬛", "苏培盛"],
        },
    },
    "曹贵人": {
        "aliases": ["曹琴默", "襄嫔"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["贵人", "嫔"],
            "residences": ["储秀宫"],
            "major_plots": ["huafei_ci_si"],
            "connections": ["年世兰", "爱新觉罗·胤禛", "甄嬛", "富察·温宜"],
        },
    },
    "富察·温宜": {
        "aliases": ["温宜公主", "温宜"],
        "attrs": {
            "gender": "女",
            "role_type": "皇族",
            "titles": ["公主"],
            "residences": ["景仁宫"],
            "major_plots": ["huafei_ci_si"],
            "connections": ["曹贵人", "乌拉那拉·宜修", "爱新觉罗·胤禛"],
        },
    },
    "苏培盛": {
        "aliases": ["苏公公"],
        "attrs": {
            "gender": "男",
            "role_type": "太监",
            "titles": ["总管太监"],
            "residences": ["养心殿"],
            "major_plots": ["huafei_ci_si", "houwei_zhongju"],
            "connections": ["爱新觉罗·胤禛", "甄嬛", "乌雅氏"],
        },
    },
    "剪秋": {
        "aliases": [],
        "attrs": {
            "gender": "女",
            "role_type": "宫女",
            "titles": ["宫女"],
            "residences": ["景仁宫"],
            "major_plots": ["dixue_qinzi", "anling_baiju", "houwei_zhongju"],
            "connections": ["乌拉那拉·宜修"],
        },
    },
    "允礼": {
        "aliases": ["果郡王", "果亲王", "十七爷"],
        "attrs": {
            "gender": "男",
            "role_type": "皇族",
            "titles": ["亲王"],
            "residences": ["宫外"],
            "major_plots": ["ganlu_yunfeng", "heqin_shitan", "houwei_zhongju"],
            "connections": ["甄嬛", "浣碧", "孟静娴", "舒太妃", "爱新觉罗·胤禛", "弘曕", "元澈"],
        },
    },
    "温实初": {
        "aliases": ["温太医"],
        "attrs": {
            "gender": "男",
            "role_type": "太医",
            "titles": ["太医"],
            "residences": ["宫中"],
            "major_plots": ["huanyi_xiaochang", "ganlu_yunfeng", "dixue_qinzi"],
            "connections": ["甄嬛", "沈眉庄"],
        },
    },
    "浣碧": {
        "aliases": ["玉隐", "钮祜禄·玉隐"],
        "attrs": {
            "gender": "女",
            "role_type": "宫女",
            "titles": ["宫女"],
            "residences": ["碎玉轩", "宫外"],
            "major_plots": ["ganlu_yunfeng", "heqin_shitan"],
            "connections": ["甄嬛", "允礼", "流朱", "孟静娴"],
        },
    },
    "崔槿汐": {
        "aliases": ["槿汐"],
        "attrs": {
            "gender": "女",
            "role_type": "宫女",
            "titles": ["宫女"],
            "residences": ["碎玉轩", "永寿宫"],
            "major_plots": ["chunyuan_jiufu", "ganlu_yunfeng", "dixue_qinzi", "houwei_zhongju"],
            "connections": ["甄嬛"],
        },
    },
    "流朱": {
        "aliases": [],
        "attrs": {
            "gender": "女",
            "role_type": "宫女",
            "titles": ["宫女"],
            "residences": ["碎玉轩"],
            "major_plots": ["chuxuan_dianxuan", "chunyuan_jiufu"],
            "connections": ["甄嬛", "浣碧"],
        },
    },
    "纯元皇后": {
        "aliases": ["菀菀", "纯元"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["皇后"],
            "residences": ["宫外"],
            "major_plots": ["chunyuan_jiufu", "houwei_zhongju"],
            "connections": ["爱新觉罗·胤禛", "乌拉那拉·宜修", "甄嬛"],
        },
    },
    "端妃": {
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["妃", "皇贵妃"],
            "residences": ["宫中"],
            "major_plots": ["huafei_ci_si", "dixue_qinzi", "houwei_zhongju"],
            "connections": ["爱新觉罗·胤禛", "敬妃", "甄嬛"],
        },
    },
    "敬妃": {
        "aliases": ["敬贵妃"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["妃", "贵妃"],
            "residences": ["宫中"],
            "major_plots": ["huafei_ci_si", "dixue_qinzi", "houwei_zhongju"],
            "connections": ["爱新觉罗·胤禛", "端妃", "甄嬛"],
        },
    },
    "齐妃": {
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["妃"],
            "residences": ["宫中"],
            "major_plots": ["anling_baiju"],
            "connections": ["爱新觉罗·胤禛", "弘时"],
        },
    },
    "文鸳": {
        "aliases": ["祺嫔", "祺贵人"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["贵人", "嫔"],
            "residences": ["宫中"],
            "major_plots": ["huafei_ci_si", "anling_baiju"],
            "connections": ["乌拉那拉·宜修", "安陵容", "甄嬛"],
        },
    },
    "淳常在": {
        "aliases": ["淳贵人"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["常在", "贵人"],
            "residences": ["宫中"],
            "major_plots": ["chuxuan_dianxuan"],
            "connections": ["甄嬛"],
        },
    },
    "欣嫔": {
        "aliases": ["欣贵人", "欣常在"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["常在", "贵人", "嫔"],
            "residences": ["宫中"],
            "major_plots": ["anling_baiju"],
            "connections": ["甄嬛", "爱新觉罗·胤禛"],
        },
    },
    "夏冬春": {
        "aliases": ["夏常在"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["常在"],
            "residences": ["宫中"],
            "major_plots": ["chuxuan_dianxuan"],
            "connections": ["年世兰", "安陵容"],
        },
    },
    "富察贵人": {
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["贵人"],
            "residences": ["宫中"],
            "major_plots": ["huanyi_xiaochang"],
            "connections": ["年世兰", "甄嬛"],
        },
    },
    "丽嫔": {
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["嫔"],
            "residences": ["翊坤宫", "冷宫"],
            "major_plots": ["jinghong_wu"],
            "connections": ["年世兰"],
        },
    },
    "余莺儿": {
        "aliases": ["余答应", "余氏"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["答应"],
            "residences": ["冷宫"],
            "major_plots": ["jinghong_wu"],
            "connections": ["年世兰", "安陵容", "甄嬛"],
        },
    },
    "乔颂芝": {
        "aliases": ["颂芝"],
        "attrs": {
            "gender": "女",
            "role_type": "宫女",
            "titles": ["宫女"],
            "residences": ["翊坤宫"],
            "major_plots": ["huafei_ci_si"],
            "connections": ["年世兰"],
        },
    },
    "舒太妃": {
        "aliases": ["冲静元师"],
        "attrs": {
            "gender": "女",
            "role_type": "皇族",
            "titles": ["太妃"],
            "residences": ["宫外"],
            "major_plots": ["ganlu_yunfeng"],
            "connections": ["允礼", "积云"],
        },
    },
    "孟静娴": {
        "aliases": ["孟氏"],
        "attrs": {
            "gender": "女",
            "role_type": "皇族",
            "titles": ["福晋"],
            "residences": ["宫外"],
            "major_plots": ["heqin_shitan"],
            "connections": ["允礼", "浣碧", "元澈"],
        },
    },
    "甄玉娆": {
        "aliases": ["玉娆"],
        "attrs": {
            "gender": "女",
            "role_type": "皇族",
            "titles": ["福晋"],
            "residences": ["宫外"],
            "major_plots": ["dixue_qinzi", "heqin_shitan"],
            "connections": ["甄嬛", "允禧", "甄远道"],
        },
    },
    "允禧": {
        "aliases": ["慎郡王", "慎贝勒"],
        "attrs": {
            "gender": "男",
            "role_type": "皇族",
            "titles": ["亲王"],
            "residences": ["宫外"],
            "major_plots": ["dixue_qinzi", "heqin_shitan"],
            "connections": ["甄玉娆", "爱新觉罗·胤禛", "元澈"],
        },
    },
    "弘时": {
        "aliases": ["三阿哥"],
        "attrs": {
            "gender": "男",
            "role_type": "皇族",
            "titles": ["皇族"],
            "residences": ["宫中"],
            "major_plots": ["houwei_zhongju"],
            "connections": ["齐妃", "爱新觉罗·胤禛"],
        },
    },
    "弘历": {
        "aliases": ["四阿哥", "乾隆", "宝亲王"],
        "attrs": {
            "gender": "男",
            "role_type": "皇族",
            "titles": ["皇帝", "皇族"],
            "residences": ["宫中"],
            "major_plots": ["houwei_zhongju"],
            "connections": ["爱新觉罗·胤禛", "甄嬛"],
        },
    },
    "弘曕": {
        "aliases": ["六阿哥"],
        "attrs": {
            "gender": "男",
            "role_type": "皇族",
            "titles": ["皇族"],
            "residences": ["永寿宫"],
            "major_plots": ["dixue_qinzi", "heqin_shitan", "houwei_zhongju"],
            "connections": ["甄嬛", "允礼"],
        },
    },
    "胧月公主": {
        "aliases": ["绾绾", "胧月"],
        "attrs": {
            "gender": "女",
            "role_type": "皇族",
            "titles": ["公主"],
            "residences": ["永寿宫"],
            "major_plots": ["chunyuan_jiufu", "ganlu_yunfeng"],
            "connections": ["甄嬛", "爱新觉罗·胤禛"],
        },
    },
    "灵犀公主": {
        "aliases": ["灵犀"],
        "attrs": {
            "gender": "女",
            "role_type": "皇族",
            "titles": ["公主"],
            "residences": ["永寿宫"],
            "major_plots": ["dixue_qinzi"],
            "connections": ["甄嬛", "允礼"],
        },
    },
    "元澈": {
        "attrs": {
            "gender": "男",
            "role_type": "皇族",
            "titles": ["皇族"],
            "residences": ["宫外"],
            "major_plots": ["heqin_shitan"],
            "connections": ["允礼", "孟静娴", "允禧", "甄玉娆"],
        },
    },
    "宝鹃": {
        "attrs": {
            "gender": "女",
            "role_type": "宫女",
            "titles": ["宫女"],
            "residences": ["延禧宫"],
            "major_plots": ["anling_baiju"],
            "connections": ["安陵容"],
        },
    },
    "芳若": {
        "attrs": {
            "gender": "女",
            "role_type": "宫女",
            "titles": ["宫女"],
            "residences": ["寿康宫"],
            "major_plots": ["houwei_zhongju"],
            "connections": ["乌雅氏"],
        },
    },
    "福子": {
        "attrs": {
            "gender": "女",
            "role_type": "宫女",
            "titles": ["宫女"],
            "residences": ["翊坤宫"],
            "major_plots": ["chuxuan_dianxuan"],
            "connections": ["年世兰", "乌拉那拉·宜修"],
        },
    },
    "积云": {
        "attrs": {
            "gender": "女",
            "role_type": "宫女",
            "titles": ["宫女"],
            "residences": ["宫外"],
            "major_plots": ["ganlu_yunfeng"],
            "connections": ["舒太妃"],
        },
    },
    "小允子": {
        "attrs": {
            "gender": "男",
            "role_type": "太监",
            "titles": ["太监"],
            "residences": ["碎玉轩"],
            "major_plots": ["jinghong_wu"],
            "connections": ["甄嬛", "崔槿汐"],
        },
    },
    "江福海": {
        "attrs": {
            "gender": "男",
            "role_type": "太监",
            "titles": ["太监"],
            "residences": ["翊坤宫"],
            "major_plots": ["huafei_ci_si"],
            "connections": ["年世兰"],
        },
    },
    "周宁海": {
        "attrs": {
            "gender": "男",
            "role_type": "太监",
            "titles": ["太监"],
            "residences": ["翊坤宫"],
            "major_plots": ["jinghong_wu", "huafei_ci_si"],
            "connections": ["年世兰"],
        },
    },
    "夏刈": {
        "attrs": {
            "gender": "男",
            "role_type": "侍卫",
            "titles": ["侍卫"],
            "residences": ["宫中"],
            "major_plots": ["heqin_shitan"],
            "connections": ["爱新觉罗·胤禛"],
        },
    },
    "甄远道": {
        "attrs": {
            "gender": "男",
            "role_type": "前朝官员",
            "titles": ["大臣"],
            "residences": ["宫外"],
            "major_plots": ["chunyuan_jiufu", "ganlu_yunfeng"],
            "connections": ["甄嬛", "甄玉娆", "甄母"],
        },
    },
    "甄母": {
        "attrs": {
            "gender": "女",
            "role_type": "前朝官员",
            "titles": ["大臣"],
            "residences": ["宫外"],
            "major_plots": [],
            "connections": ["甄远道", "甄嬛", "甄玉娆"],
        },
    },
    "年羹尧": {
        "attrs": {
            "gender": "男",
            "role_type": "前朝官员",
            "titles": ["大臣"],
            "residences": ["宫外"],
            "major_plots": ["huafei_ci_si"],
            "connections": ["年世兰", "爱新觉罗·胤禛"],
        },
    },
    "沈自山": {
        "aliases": ["沈协领", "眉庄之父"],
        "attrs": {
            "gender": "男",
            "role_type": "前朝官员",
            "titles": ["大臣"],
            "residences": ["宫外"],
            "major_plots": [],
            "connections": ["沈眉庄"],
        },
    },
    "安比槐": {
        "aliases": ["安县丞", "陵容之父"],
        "attrs": {
            "gender": "男",
            "role_type": "前朝官员",
            "titles": ["大臣"],
            "residences": ["宫外"],
            "major_plots": ["anling_baiju"],
            "connections": ["安陵容"],
        },
    },
    "静岸": {
        "attrs": {
            "gender": "女",
            "role_type": "宫女",
            "titles": ["宫女"],
            "residences": ["甘露寺"],
            "major_plots": ["ganlu_yunfeng"],
            "connections": ["甄嬛"],
        },
    },
    "摩格": {
        "attrs": {
            "gender": "男",
            "role_type": "皇族",
            "titles": ["皇族"],
            "residences": ["宫外"],
            "major_plots": ["heqin_shitan"],
            "connections": ["爱新觉罗·胤禛", "甄嬛"],
        },
    },
    "朝瑰公主": {
        "attrs": {
            "gender": "女",
            "role_type": "皇族",
            "titles": ["公主"],
            "residences": ["宫外"],
            "major_plots": ["heqin_shitan"],
            "connections": ["爱新觉罗·胤禛"],
        },
    },
    "五阿哥": {
        "attrs": {
            "gender": "男",
            "role_type": "皇族",
            "titles": ["皇族"],
            "residences": ["宫外"],
            "major_plots": [],
            "connections": ["爱新觉罗·胤禛"],
        },
    },
}

# 仅修正明显错误的身份（不改剧情关系）
ROLE_FIXES = {
    "采苹": ("女", "宫女", ["宫女"], ["宫中"]),
    "康常在": ("女", "后宫嫔妃", ["常在"], ["宫中"]),
    "贞嫔": ("女", "后宫嫔妃", ["嫔"], ["宫中"]),
    "芳贵人": ("女", "后宫嫔妃", ["贵人"], ["碎玉轩"]),
    "孙答应": ("女", "后宫嫔妃", ["答应"], ["宫中"]),
    "斐雯": ("女", "宫女", ["宫女"], ["宫中"]),
    "玢儿": ("女", "宫女", ["宫女"], ["宫中"]),
    "宝鹊": ("女", "宫女", ["宫女"], ["延禧宫"]),
    "绘春": ("女", "宫女", ["宫女"], ["宫中"]),
    "阿绿": ("女", "宫女", ["宫女"], ["宫中"]),
    "卫临": ("男", "太医", ["太医"], ["宫中"]),
    "章弥": ("男", "太医", ["太医"], ["宫中"]),
    "江诚": ("男", "太医", ["太医"], ["宫中"]),
    "江慎": ("男", "太医", ["太医"], ["宫中"]),
    "刘畚": ("男", "太医", ["太医"], ["宫中"]),
    "许太医": ("男", "太医", ["太医"], ["宫中"]),
    "静白": ("女", "宫女", ["宫女"], ["甘露寺"]),
    "莫言": ("女", "宫女", ["宫女"], ["甘露寺"]),
    "孙妙青": ("女", "宫女", ["宫女"], ["宫中"]),
    "静和公主": ("女", "皇族", ["公主"], ["宫中"]),
    "祺嫔": None,  # dropped via 文鸳 alias if duplicate entity exists
    "淳贵人": None,
    "余答应": None,
}


def uniq(xs):
    out, seen = [], set()
    for x in xs:
        if x and x not in seen:
            seen.add(x)
            out.append(x)
    return out


def main():
    ents = json.loads(ENTITIES.read_text(encoding="utf-8"))
    by_name = {e["name"]: e for e in ents}

    # drop duplicates
    ents = [e for e in ents if e["name"] not in DROP_NAMES]
    by_name = {e["name"]: e for e in ents}

    # drop role-fix Nones that are duplicate titles already covered
    for name, fix in list(ROLE_FIXES.items()):
        if fix is None and name in by_name:
            # only drop if alias already on another character
            del by_name[name]
    ents = [e for e in ents if e["name"] in by_name]

    # apply curated
    for name, patch in BY_NAME.items():
        e = by_name.get(name)
        if not e:
            print("WARN missing", name)
            continue
        if "aliases" in patch:
            e["aliases"] = patch["aliases"]
        attrs = patch["attrs"]
        for k in ("gender", "role_type", "titles", "residences"):
            if k in attrs:
                e["attrs"][k] = attrs[k]
        plots = attrs.get("major_plots", e["attrs"].get("major_plots", []))
        bad = [p for p in plots if p not in PLOTS]
        if bad:
            raise SystemExit(f"unknown plots on {name}: {bad}")
        e["attrs"]["major_plots"] = plots
        # resolve connection names -> ids
        conn_names = attrs.get("connections", [])
        ids = []
        for cn in conn_names:
            ce = by_name.get(cn)
            if not ce:
                print("WARN conn missing", name, "->", cn)
                continue
            ids.append(ce["id"])
        e["attrs"]["connections"] = uniq(ids)

    # role fixes
    for name, fix in ROLE_FIXES.items():
        if fix is None:
            continue
        e = by_name.get(name)
        if not e:
            continue
        gender, role, titles, res = fix
        e["attrs"]["gender"] = gender
        e["attrs"]["role_type"] = role
        e["attrs"]["titles"] = titles
        e["attrs"]["residences"] = res

    # strip obsolete plot key / dangling ids
    for e in ents:
        e["attrs"]["major_plots"] = [p for p in e["attrs"].get("major_plots", []) if p in PLOTS]
        ids = {x["id"] for x in ents}
        e["attrs"]["connections"] = [c for c in e["attrs"].get("connections", []) if c in ids]

    ENTITIES.write_text(json.dumps(ents, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    # refresh seed = curated core subset
    seed_names = [
        "甄嬛", "乌拉那拉·宜修", "年世兰", "安陵容", "沈眉庄", "叶澜依",
        "爱新觉罗·胤禛", "乌雅氏", "曹贵人", "富察·温宜", "苏培盛", "剪秋",
    ]
    seed = [by_name[n] for n in seed_names if n in by_name]
    SEED.write_text(json.dumps(seed, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    filled_p = sum(1 for e in ents if e["attrs"].get("major_plots"))
    filled_c = sum(1 for e in ents if e["attrs"].get("connections"))
    print(f"wrote {len(ents)} entities; with plots={filled_p} connections={filled_c}")


if __name__ == "__main__":
    main()
