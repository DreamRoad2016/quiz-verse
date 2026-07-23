#!/usr/bin/env python3
"""Apply curated plot/connection/role fixes onto zhenhuan entities.json."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENTITIES = ROOT / "src/main/resources/packs/zhenhuan_2011/entities.json"
SEED = ROOT / "scripts/data/zhenhuan_core_seed.json"
EP_PATCHES = [
    ROOT / "scripts/data/zhenhuan_ep01_10_patch.json",
    ROOT / "scripts/data/zhenhuan_ep11_20_patch.json",
    ROOT / "scripts/data/zhenhuan_ep21_30_patch.json",
    ROOT / "scripts/data/zhenhuan_ep31_40_patch.json",
    ROOT / "scripts/data/zhenhuan_ep41_50_patch.json",
    ROOT / "scripts/data/zhenhuan_ep51_60_patch.json",
    ROOT / "scripts/data/zhenhuan_ep61_70_patch.json",
    ROOT / "scripts/data/zhenhuan_ep71_76_patch.json",
]

# 关系网只保留核心圈，每人上限
MAX_CONNECTIONS = 10
CORE_CONNECTIONS: dict[str, list[str]] = {
    "甄嬛": [
        "甄远道", "甄母", "甄玉娆",
        "乌拉那拉·宜修", "年世兰", "安陵容", "沈眉庄", "爱新觉罗·胤禛",
        "允礼", "浣碧",
    ],
    "乌拉那拉·宜修": [
        "爱新觉罗·胤禛", "甄嬛", "安陵容", "乌雅氏", "剪秋",
        "纯元皇后", "年世兰", "曹贵人", "富察·温宜", "齐妃",
    ],
    "年世兰": [
        "爱新觉罗·胤禛", "甄嬛", "曹贵人", "年羹尧", "乔颂芝",
        "丽嫔", "乌拉那拉·宜修", "乌雅氏", "周宁海", "肃喜",
    ],
    "安陵容": [
        "甄嬛", "乌拉那拉·宜修", "沈眉庄", "爱新觉罗·胤禛", "安比槐",
        "宝鹃", "年世兰",
    ],
    "沈眉庄": [
        "甄嬛", "安陵容", "爱新觉罗·胤禛", "沈自山", "温实初", "敬妃", "采月", "静和公主",
    ],
    "爱新觉罗·胤禛": [
        "甄嬛", "乌拉那拉·宜修", "年世兰", "乌雅氏", "允礼",
        "沈眉庄", "安陵容", "纯元皇后", "苏培盛", "年羹尧",
    ],
    "乌雅氏": [
        "爱新觉罗·胤禛", "乌拉那拉·宜修", "甄嬛", "苏培盛", "芳若", "年世兰",
    ],
    "曹贵人": [
        "年世兰", "爱新觉罗·胤禛", "甄嬛", "富察·温宜", "乌拉那拉·宜修",
    ],
    "允礼": [
        "甄嬛", "浣碧", "舒太妃", "爱新觉罗·胤禛", "孟静娴", "弘曕", "元澈",
    ],
    "浣碧": [
        "甄嬛", "允礼", "流朱", "孟静娴", "元澈",
    ],
    "温实初": [
        "甄嬛", "沈眉庄", "卫临",
    ],
    "卫临": [
        "温实初", "甄嬛",
    ],
    "章弥": [
        "甄嬛", "爱新觉罗·胤禛", "温实初",
    ],
    "江慎": [
        "年世兰", "江诚", "温实初",
    ],
    "江诚": [
        "江慎", "年世兰", "温实初",
    ],
    "刘畚": [
        "沈眉庄", "甄嬛", "温实初",
    ],
    "许太医": [
        "温实初", "爱新觉罗·胤禛",
    ],
    "崔槿汐": [
        "甄嬛", "小允子", "苏培盛",
    ],
    "苏培盛": [
        "爱新觉罗·胤禛", "甄嬛", "乌雅氏", "崔槿汐",
    ],
    "敬妃": [
        "爱新觉罗·胤禛", "端妃", "甄嬛", "沈眉庄", "胧月公主",
    ],
    "端妃": [
        "爱新觉罗·胤禛", "敬妃", "甄嬛",
    ],
    "齐妃": [
        "爱新觉罗·胤禛", "弘时", "甄嬛", "乌拉那拉·宜修",
    ],
    "叶澜依": [
        "爱新觉罗·胤禛", "甄嬛", "乌拉那拉·宜修", "阿绿",
    ],
    "纯元皇后": [
        "爱新觉罗·胤禛", "乌拉那拉·宜修",
    ],
    "富察·温宜": [
        "曹贵人", "乌拉那拉·宜修", "爱新觉罗·胤禛", "年世兰",
    ],
    "年羹尧": [
        "年世兰", "年斌", "年富", "年兴", "爱新觉罗·胤禛",
    ],
    "年斌": [
        "年羹尧", "年富", "年兴", "年世兰",
    ],
    "年富": [
        "年羹尧", "年斌", "年兴", "年世兰",
    ],
    "年兴": [
        "年羹尧", "年斌", "年富", "年世兰",
    ],
    "乔颂芝": [
        "年世兰",
    ],
    # —— 宫女 / 太监 / 官员（小角色必须自带收敛线索）——
    "斐雯": [
        "甄嬛", "文鸳", "温实初",
    ],
    "玢儿": [
        "甄嬛", "文鸳", "浣碧",
    ],
    "菊青": [
        "甄嬛", "安陵容",
    ],
    "品儿": [
        "甄嬛", "崔槿汐",
    ],
    "绘春": [
        "甄嬛", "姜忠敏",
    ],
    "莫言": [
        "甄嬛", "静岸", "静白",
    ],
    "宝鹃": [
        "安陵容", "宝鹊",
    ],
    "宝鹊": [
        "安陵容", "宝鹃", "乌拉那拉·宜修",
    ],
    "孙妙青": [
        "甄嬛", "乌雅氏",
    ],
    "花穗": [
        "余莺儿", "甄嬛",
    ],
    "雨儿": [
        "淳常在",
    ],
    "吉祥": [
        "端妃",
    ],
    "阿绿": [
        "叶澜依",
    ],
    "江福海": [
        "乌拉那拉·宜修", "剪秋",
    ],
    "周宁海": [
        "年世兰", "沈眉庄",
    ],
    "肃喜": [
        "年世兰", "甄嬛",
    ],
    "苟总管": [
        "甄嬛", "流朱",
    ],
    "梁多瑞": [
        "甄嬛", "苏培盛",
    ],
    "小荷子": [
        "康禄海", "丽嫔", "甄嬛",
    ],
    "小卫子": [
        "曹贵人",
    ],
    "姜忠敏": [
        "甄嬛",
    ],
    "精奇嬷嬷": [
        "年世兰", "周宁海",
    ],
    "季惟生": [
        "甄嬛", "沈眉庄", "安陵容",
    ],
    "隆科多": [
        "乌雅氏", "爱新觉罗·胤禛",
    ],
    "康常在": [
        "贞嫔", "安陵容", "甄嬛",
    ],
    "贞嫔": [
        "康常在", "安陵容", "甄嬛",
    ],
    "甄远道": [
        "甄嬛", "甄玉娆", "甄母",
    ],
    "甄母": [
        "甄远道", "甄嬛", "甄玉娆",
    ],
    "甄玉娆": [
        "甄嬛", "允禧", "甄远道",
    ],
    "文鸳": [
        "乌拉那拉·宜修", "安陵容", "甄嬛", "鄂敏", "欣嫔",
    ],
    "胧月公主": [
        "甄嬛", "敬妃", "爱新觉罗·胤禛",
    ],
    "静岸": [
        "甄嬛",
    ],
    "静白": [
        "甄嬛", "静岸",
    ],
    "舒太妃": [
        "允礼", "积云", "爱新觉罗·胤禛",
    ],
    "积云": [
        "舒太妃",
    ],
    "鄂敏": [
        "文鸳",
    ],
    "肃喜": [
        "年世兰",
    ],
    "弘历": [
        "甄嬛", "爱新觉罗·胤禛", "乌雅氏", "富察氏皇后",
    ],
    "弘时": [
        "齐妃", "爱新觉罗·胤禛", "乌拉那拉·宜修", "瑛贵人",
    ],
    "弘曕": [
        "甄嬛", "允礼", "爱新觉罗·胤禛",
    ],
    "灵犀公主": [
        "甄嬛", "允礼",
    ],
    "佩儿": [
        "甄嬛", "欣嫔",
    ],
    "采月": [
        "沈眉庄",
    ],
    "欣嫔": [
        "文鸳", "甄嬛", "佩儿",
    ],
    "小厦子": [
        "苏培盛",
    ],
    "孟静娴": [
        "允礼", "浣碧", "元澈",
    ],
    "静和公主": [
        "沈眉庄", "甄嬛",
    ],
    "瑛贵人": [
        "弘时", "甄嬛", "乌拉那拉·宜修",
    ],
    "青樱": [
        "乌拉那拉·宜修", "弘时", "爱新觉罗·胤禛", "弘历",
    ],
    "摩格": [
        "爱新觉罗·胤禛", "甄嬛", "允礼",
    ],
    "夏刈": [
        "爱新觉罗·胤禛",
    ],
    "孙竹息": [
        "乌雅氏", "爱新觉罗·胤禛",
    ],
    "富察氏皇后": [
        "弘历", "甄嬛",
    ],
    "元澈": [
        "允礼", "孟静娴", "浣碧",
    ],
    "张廷玉": [
        "爱新觉罗·胤禛", "弘历",
    ],
}

# 亲属 / 最亲密边（无向）。在 CORE_CONNECTIONS 之后强制双向写入，优先占满名额。
# 用于避免「猜父亲却对不上儿子」这类无法收敛的情况。
FAMILY_BONDS: list[tuple[str, str]] = [
    # 年家：兄妹 + 父子 + 兄弟
    ("年羹尧", "年世兰"),
    ("年羹尧", "年斌"),
    ("年羹尧", "年富"),
    ("年羹尧", "年兴"),
    ("年斌", "年富"),
    ("年斌", "年兴"),
    ("年富", "年兴"),
    ("年世兰", "年斌"),
    ("年世兰", "年富"),
    ("年世兰", "年兴"),
    # 甄家
    ("甄远道", "甄母"),
    ("甄远道", "甄嬛"),
    ("甄远道", "甄玉娆"),
    ("甄母", "甄嬛"),
    ("甄母", "甄玉娆"),
    ("甄嬛", "甄玉娆"),
    # 沈 / 安
    ("沈眉庄", "沈自山"),
    ("安陵容", "安比槐"),
    # 母子 / 父子 / 养育
    ("乌雅氏", "爱新觉罗·胤禛"),
    ("齐妃", "弘时"),
    ("敬妃", "胧月公主"),
    ("甄嬛", "胧月公主"),
    ("甄嬛", "灵犀公主"),
    ("甄嬛", "弘曕"),
    ("爱新觉罗·胤禛", "弘历"),
    ("爱新觉罗·胤禛", "弘时"),
    ("爱新觉罗·胤禛", "胧月公主"),
    ("爱新觉罗·胤禛", "灵犀公主"),
    ("舒太妃", "允礼"),
    ("允礼", "弘曕"),
    ("允礼", "元澈"),
    ("浣碧", "元澈"),
    ("孟静娴", "元澈"),
    # 夫妻 / 侧福晋
    ("爱新觉罗·胤禛", "乌拉那拉·宜修"),
    ("爱新觉罗·胤禛", "纯元皇后"),
    ("乌拉那拉·宜修", "纯元皇后"),
    ("允礼", "浣碧"),
    ("允礼", "孟静娴"),
    ("弘历", "富察氏皇后"),
    ("甄玉娆", "允禧"),
    # 师徒 / 同僚（双向）
    ("温实初", "卫临"),
    ("江诚", "江慎"),
    ("宝鹃", "宝鹊"),
    ("静白", "静岸"),
    ("康常在", "贞嫔"),
]

# 服务/从属边：只写入从属方 → 主方，不反向挤占主角关系网名额。
# 小角色绝不能为空；大角色放不下是预期。
SERVICE_LINKS: list[tuple[str, str]] = [
    ("温实初", "甄嬛"),
    ("温实初", "沈眉庄"),
    ("卫临", "甄嬛"),
    ("卫临", "温实初"),
    ("章弥", "甄嬛"),
    ("章弥", "爱新觉罗·胤禛"),
    ("江慎", "年世兰"),
    ("江诚", "年世兰"),
    ("刘畚", "沈眉庄"),
    ("刘畚", "甄嬛"),
    ("许太医", "温实初"),
    ("乔颂芝", "年世兰"),
    ("肃喜", "年世兰"),
    ("剪秋", "乌拉那拉·宜修"),
    ("江福海", "乌拉那拉·宜修"),
    ("周宁海", "年世兰"),
    ("肃喜", "年世兰"),
    ("肃喜", "甄嬛"),
    ("周宁海", "沈眉庄"),
    ("宝鹃", "安陵容"),
    ("宝鹊", "安陵容"),
    ("采月", "沈眉庄"),
    ("流朱", "甄嬛"),
    ("小允子", "甄嬛"),
    ("崔槿汐", "甄嬛"),
    ("斐雯", "甄嬛"),
    ("玢儿", "甄嬛"),
    ("菊青", "甄嬛"),
    ("品儿", "甄嬛"),
    ("绘春", "甄嬛"),
    ("莫言", "甄嬛"),
    ("花穗", "甄嬛"),
    ("雨儿", "淳常在"),
    ("吉祥", "端妃"),
    ("阿绿", "叶澜依"),
    ("苟总管", "甄嬛"),
    ("梁多瑞", "甄嬛"),
    ("小荷子", "丽嫔"),
    ("小卫子", "曹贵人"),
    ("姜忠敏", "甄嬛"),
    ("精奇嬷嬷", "年世兰"),
    ("季惟生", "甄嬛"),
    ("隆科多", "乌雅氏"),
]


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

# 重大剧情只挂「当场相关」：该场戏有行动/被针对/关键见证的人。
# 不含：仅被提及、同集路人、宫外家属旁听、事后才揭穿却未参与当场者。
# 此表为最终权威，覆盖分集补丁里的 major_plots。
CORE_MAJOR_PLOTS: dict[str, list[str]] = {
    # 选秀日与入宫分派
    "chuxuan_dianxuan": [
        "甄嬛", "安陵容", "沈眉庄", "爱新觉罗·胤禛", "乌拉那拉·宜修", "乌雅氏",
        "年世兰", "浣碧", "流朱", "温实初", "苏培盛", "甄远道", "沈自山", "安比槐",
        "敬妃", "端妃", "齐妃", "曹贵人", "淳常在", "剪秋", "孙妙青",
    ],
    # 惊鸿舞得宠、余莺儿构陷
    "jinghong_wu": [
        "甄嬛", "爱新觉罗·胤禛", "乌拉那拉·宜修", "年世兰", "余莺儿",
        "安陵容", "沈眉庄", "温实初", "苏培盛", "乌雅氏", "丽嫔", "曹贵人",
        "刘畚",
    ],
    # 欢宜香致小产（安陵容供香事发归败亡线，不挂本场）
    "huanyi_xiaochang": [
        "甄嬛", "年世兰", "爱新觉罗·胤禛", "乌拉那拉·宜修", "温实初",
        "崔槿汐", "浣碧", "曹贵人", "淳常在", "苏培盛", "乌雅氏",
        "章弥", "江慎",
    ],
    # 火烧碎玉轩、冷宫赐死
    "huafei_ci_si": [
        "年世兰", "爱新觉罗·胤禛", "乌拉那拉·宜修", "甄嬛", "肃喜",
        "苏培盛", "敬妃", "乔颂芝", "文鸳", "乌雅氏", "剪秋", "周宁海",
    ],
    # 误穿纯元故衣、失宠幽禁
    "chunyuan_jiufu": [
        "甄嬛", "爱新觉罗·胤禛", "乌拉那拉·宜修", "纯元皇后", "崔槿汐",
        "绘春", "姜忠敏", "乌雅氏", "苏培盛", "浣碧", "苟总管", "流朱",
    ],
    # 甘露寺/凌云峰修行与回宫（不含宫中同期闲角）
    "ganlu_yunfeng": [
        "甄嬛", "允礼", "浣碧", "崔槿汐", "静岸", "静白", "爱新觉罗·胤禛",
        "沈眉庄", "温实初", "舒太妃", "积云", "乌雅氏", "乌拉那拉·宜修",
        "敬妃", "胧月公主", "苏培盛", "叶澜依", "弘历", "阿晋", "莫言",
        "卫临",
    ],
    # 凝露香/欢宜香事发、鹂妃禁足；惠妃惊胎亡属本线因果
    "anling_baiju": [
        "安陵容", "甄嬛", "爱新觉罗·胤禛", "乌拉那拉·宜修", "端妃", "敬妃",
        "宝鹃", "宝鹊", "余莺儿", "沈眉庄", "静和公主", "吉祥", "苏培盛", "温实初",
        "卫临", "季惟生", "康常在", "贞嫔",
    ],
    # 滴血验亲当场
    "dixue_qinzi": [
        "甄嬛", "爱新觉罗·胤禛", "弘曕", "乌拉那拉·宜修", "温实初",
        "苏培盛", "乌雅氏", "崔槿汐", "浣碧", "卫临",
        "斐雯", "玢儿", "莫言", "文鸳", "静白", "康常在", "贞嫔",
    ],
    # 摩格逼和亲、允礼请缨
    "heqin_shitan": [
        "甄嬛", "爱新觉罗·胤禛", "允礼", "摩格", "夏刈", "张廷玉",
        "苏培盛", "浣碧", "朝瑰公主",
    ],
    # 废后困局、果亲王殉、龙驭宾天、尊太后
    "houwei_zhongju": [
        "甄嬛", "爱新觉罗·胤禛", "乌拉那拉·宜修", "剪秋", "端妃", "敬妃",
        "弘历", "叶澜依", "允礼", "浣碧", "孟静娴", "纯元皇后", "孙竹息",
        "苏培盛", "弘时", "张廷玉", "崔槿汐", "富察氏皇后", "青樱",
        "胧月公主", "弘曕", "卫临", "江福海", "梁多瑞",
    ],
    # 华妃赐死线：周宁海供罪
    # （已在 huafei_ci_si）
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
    "敦亲王福晋博尔济吉特氏",  # 保留 敦亲王福晋（十福晋）
    "采苹",  # 瑛贵人即采蘋
    "富察氏",  # 保留 富察氏皇后
    # —— 可玩性清理（空壳 / 重复 / 无明确交集）——
    "芝答应",  # 即乔颂芝（aliases 已含）
    "孙答应",  # 剧中未真正立人
    "娴妃",  # 即青樱（aliases 已含）
    "刘嬷嬷",
    "张嬷嬷",
    # 宫女：百科/剧本无法确认服侍对象，空网且同指纹 → 移出题池
    "春貌",
    "灵芝",
    "采星",
    "茯苓",
    "采蓝",
    "如意",
    "含珠",
    "景泰",
    "刘莲子",  # 作者客串秀女，戏份删减
}

# 按姓名覆盖的精修（role/titles/residences/aliases/plots/connections）
# connections 用姓名，写入时解析为 id
BY_NAME: dict[str, dict] = {
    "甄嬛": {
        "aliases": ["嬛嬛", "莞莞", "莞", "莫愁", "熹妃", "熹贵妃", "圣母皇太后"],
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
                "流朱", "淳常在", "小允子",
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
                "福子", "年世兰",
            ],
        },
    },
    "年世兰": {
        "aliases": ["华妃", "年妃", "华贵妃", "年答应", "年氏"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["妃", "贵妃", "答应", "皇贵妃"],
            "residences": ["翊坤宫", "冷宫"],
            "major_plots": ["chuxuan_dianxuan", "jinghong_wu", "huanyi_xiaochang", "huafei_ci_si"],
            "connections": [
                "爱新觉罗·胤禛", "甄嬛", "曹贵人", "年羹尧", "乔颂芝", "沈眉庄",
                "丽嫔", "夏冬春", "江福海", "周宁海",
            ],
        },
    },
    "安陵容": {
        "aliases": ["陵容", "安答应", "安常在", "安贵人", "安嫔", "鹂妃", "鸝妃"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["答应", "常在", "贵人", "嫔", "妃"],
            "residences": ["延禧宫"],
            "major_plots": [
                "chuxuan_dianxuan", "jinghong_wu", "huanyi_xiaochang",
                "chunyuan_jiufu", "ganlu_yunfeng", "anling_baiju", "dixue_qinzi",
            ],
            "connections": [
                "甄嬛", "乌拉那拉·宜修", "沈眉庄", "爱新觉罗·胤禛", "安比槐", "宝鹃", "年世兰",
            ],
        },
    },
    "沈眉庄": {
        "aliases": ["眉庄", "沈贵人", "惠妃", "惠贵妃"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["贵人", "嫔", "妃", "贵妃"],
            "residences": ["咸福宫"],
            "major_plots": [
                "chuxuan_dianxuan", "jinghong_wu", "huafei_ci_si",
                "chunyuan_jiufu", "anling_baiju", "dixue_qinzi",
            ],
            "connections": ["甄嬛", "安陵容", "爱新觉罗·胤禛", "沈自山", "温实初", "敬妃", "采月"],
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
                "chuxuan_dianxuan", "jinghong_wu", "huanyi_xiaochang", "huafei_ci_si",
                "chunyuan_jiufu", "ganlu_yunfeng", "dixue_qinzi", "heqin_shitan", "houwei_zhongju",
            ],
            "connections": [
                "甄嬛", "乌拉那拉·宜修", "年世兰", "乌雅氏", "允礼", "沈眉庄", "安陵容", "纯元皇后",
                "苏培盛", "余莺儿",
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
            "major_plots": ["chuxuan_dianxuan", "huafei_ci_si", "chunyuan_jiufu", "houwei_zhongju"],
            "connections": ["爱新觉罗·胤禛", "乌拉那拉·宜修", "甄嬛", "苏培盛", "芳若"],
        },
    },
    "曹贵人": {
        "aliases": ["曹琴默", "襄嫔"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["贵人", "嫔"],
            "residences": ["储秀宫"],
            "major_plots": ["chuxuan_dianxuan", "huafei_ci_si"],
            "connections": ["年世兰", "爱新觉罗·胤禛", "甄嬛", "富察·温宜", "丽嫔"],
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
            "major_plots": ["huafei_ci_si", "ganlu_yunfeng", "houwei_zhongju"],
            "connections": ["爱新觉罗·胤禛", "甄嬛", "乌雅氏", "崔槿汐"],
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
            "major_plots": ["ganlu_yunfeng", "dixue_qinzi", "heqin_shitan", "houwei_zhongju"],
            "connections": ["甄嬛", "浣碧", "孟静娴", "舒太妃", "爱新觉罗·胤禛", "弘曕"],
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
            "titles": ["宫女", "福晋"],
            "residences": ["碎玉轩", "甘露寺", "永寿宫", "宫外"],
            "major_plots": ["ganlu_yunfeng", "dixue_qinzi", "heqin_shitan"],
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
            "connections": ["甄嬛", "小允子", "苏培盛"],
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
        "aliases": ["菀菀", "纯元", "孝敬皇太后"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["皇后", "太后"],
            "residences": ["宫外"],
            "major_plots": ["chunyuan_jiufu", "houwei_zhongju"],
            "connections": ["爱新觉罗·胤禛", "乌拉那拉·宜修", "甄嬛"],
        },
    },
    "端妃": {
        "aliases": ["端皇贵妃"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["妃", "皇贵妃"],
            "residences": ["宫中"],
            "major_plots": ["huafei_ci_si", "anling_baiju", "dixue_qinzi", "houwei_zhongju"],
            "connections": ["爱新觉罗·胤禛", "敬妃", "甄嬛"],
        },
    },
    "敬妃": {
        "aliases": ["敬嫔", "敬贵妃"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["妃", "贵妃"],
            "residences": ["宫中"],
            "major_plots": ["huafei_ci_si", "anling_baiju", "dixue_qinzi", "houwei_zhongju"],
            "connections": ["爱新觉罗·胤禛", "端妃", "甄嬛", "沈眉庄", "胧月公主"],
        },
    },
    "叶澜依": {
        "aliases": ["澜依", "叶答应", "宁贵人", "宁嫔"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["答应", "贵人", "嫔"],
            "residences": ["宫中", "圆明园"],
            "major_plots": ["ganlu_yunfeng", "dixue_qinzi", "heqin_shitan", "houwei_zhongju"],
            "connections": ["爱新觉罗·胤禛", "甄嬛", "乌拉那拉·宜修", "阿绿"],
        },
    },
    "齐妃": {
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["妃"],
            "residences": ["长春宫"],
            "major_plots": ["chuxuan_dianxuan", "huanyi_xiaochang", "ganlu_yunfeng"],
            "connections": ["爱新觉罗·胤禛", "弘时"],
        },
    },
    "文鸳": {
        "aliases": ["祺嫔", "祺贵人"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["贵人", "嫔"],
            "residences": ["储秀宫"],
            "major_plots": ["huafei_ci_si", "chunyuan_jiufu", "ganlu_yunfeng", "anling_baiju"],
            "connections": ["乌拉那拉·宜修", "安陵容", "甄嬛", "鄂敏"],
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
        "aliases": ["冲静元师", "冲静先师"],
        "attrs": {
            "gender": "女",
            "role_type": "皇族",
            "titles": ["太妃"],
            "residences": ["宫外"],
            "major_plots": ["jinghong_wu", "ganlu_yunfeng"],
            "connections": ["允礼", "积云", "爱新觉罗·胤禛"],
        },
    },
    "孟静娴": {
        "aliases": ["孟氏", "娴福晋"],
        "attrs": {
            "gender": "女",
            "role_type": "皇族",
            "titles": ["福晋"],
            "residences": ["宫外"],
            "major_plots": ["heqin_shitan"],
            "connections": ["允礼", "浣碧"],
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
            "residences": ["宫中", "寿康宫", "永寿宫"],
            "major_plots": ["ganlu_yunfeng", "houwei_zhongju"],
            "connections": ["甄嬛", "爱新觉罗·胤禛", "乌雅氏"],
        },
    },
    "弘曕": {
        "aliases": ["六阿哥"],
        "attrs": {
            "gender": "男",
            "role_type": "皇族",
            "titles": ["皇族"],
            "residences": ["永寿宫"],
            "major_plots": ["ganlu_yunfeng", "dixue_qinzi", "heqin_shitan", "houwei_zhongju"],
            "connections": ["甄嬛", "允礼", "爱新觉罗·胤禛"],
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
            "connections": ["甄嬛", "敬妃", "爱新觉罗·胤禛"],
        },
    },
    "灵犀公主": {
        "aliases": ["灵犀"],
        "attrs": {
            "gender": "女",
            "role_type": "皇族",
            "titles": ["公主"],
            "residences": ["永寿宫"],
            "major_plots": ["ganlu_yunfeng", "dixue_qinzi"],
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
    "摩格": {
        "attrs": {
            "gender": "男",
            "role_type": "皇族",
            "titles": ["皇族"],
            "residences": ["宫外"],
            "major_plots": ["heqin_shitan", "houwei_zhongju"],
            "connections": ["爱新觉罗·胤禛", "甄嬛", "允礼"],
        },
    },
    "夏刈": {
        "attrs": {
            "gender": "男",
            "role_type": "侍卫",
            "titles": ["侍卫"],
            "residences": ["宫中"],
            "major_plots": ["huanyi_xiaochang", "heqin_shitan", "houwei_zhongju"],
            "connections": ["爱新觉罗·胤禛"],
        },
    },
    "孙竹息": {
        "aliases": ["竹息"],
        "attrs": {
            "gender": "女",
            "role_type": "宫女",
            "titles": ["宫女"],
            "residences": ["寿康宫"],
            "major_plots": ["jinghong_wu", "ganlu_yunfeng", "houwei_zhongju"],
            "connections": ["乌雅氏", "爱新觉罗·胤禛"],
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
    "瑛贵人": {
        "aliases": ["采蘋", "采苹", "瑛常在"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["常在", "贵人"],
            "residences": ["宫中"],
            "major_plots": ["anling_baiju", "houwei_zhongju"],
            "connections": ["弘时", "甄嬛", "乌拉那拉·宜修"],
        },
    },
    "静和公主": {
        "aliases": ["静和"],
        "attrs": {
            "gender": "女",
            "role_type": "皇族",
            "titles": ["公主"],
            "residences": ["咸福宫", "永寿宫"],
            "major_plots": ["anling_baiju", "dixue_qinzi"],
            "connections": ["沈眉庄", "甄嬛"],
        },
    },
    "青樱": {
        "aliases": ["娴妃"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["格格", "妃"],
            "residences": ["宫中"],
            "major_plots": ["houwei_zhongju"],
            "connections": ["乌拉那拉·宜修", "弘时", "爱新觉罗·胤禛", "弘历"],
        },
    },
    "富察氏皇后": {
        "aliases": ["富察氏"],
        "attrs": {
            "gender": "女",
            "role_type": "后宫嫔妃",
            "titles": ["皇后", "福晋"],
            "residences": ["宫中"],
            "major_plots": ["houwei_zhongju"],
            "connections": ["弘历", "甄嬛"],
        },
    },
}

# 仅修正明显错误的身份（不改剧情关系）
ROLE_FIXES = {
    "康常在": ("女", "后宫嫔妃", ["常在"], ["宫中"]),
    "贞嫔": ("女", "后宫嫔妃", ["嫔"], ["宫中"]),
    "芳贵人": ("女", "后宫嫔妃", ["贵人"], ["碎玉轩", "冷宫"]),
    "斐雯": ("女", "宫女", ["宫女"], ["永寿宫"]),
    "玢儿": ("女", "宫女", ["宫女"], ["宫中"]),
    "菊青": ("女", "宫女", ["宫女"], ["碎玉轩", "延禧宫"]),
    "品儿": ("女", "宫女", ["宫女"], ["碎玉轩"]),
    "宝鹊": ("女", "宫女", ["宫女"], ["延禧宫"]),
    "绘春": ("女", "宫女", ["宫女"], ["宫中"]),
    "阿绿": ("女", "宫女", ["宫女"], ["宫中"]),
    "季惟生": ("男", "前朝官员", ["大臣"], ["宫外"]),
    "隆科多": ("男", "前朝官员", ["大臣"], ["宫外"]),
    "卫临": ("男", "太医", ["太医"], ["宫中"]),
    "章弥": ("男", "太医", ["太医"], ["宫中"]),
    "江诚": ("男", "太医", ["太医"], ["宫中"]),
    "江慎": ("男", "太医", ["太医"], ["宫中"]),
    "刘畚": ("男", "太医", ["太医"], ["宫中"]),
    "许太医": ("男", "太医", ["太医"], ["宫中"]),
    "静白": ("女", "宫女", ["宫女"], ["甘露寺"]),
    "莫言": ("女", "宫女", ["宫女"], ["甘露寺"]),
    "孙妙青": ("女", "宫女", ["宫女"], ["宫中"]),
    "江福海": ("男", "太监", ["太监"], ["景仁宫"]),
    "周宁海": ("男", "太监", ["太监"], ["翊坤宫"]),
    "小卫子": ("男", "太监", ["太监"], ["景阳宫"]),
    "小荷子": ("男", "太监", ["太监"], ["碎玉轩"]),
    "苟总管": ("男", "太监", ["太监"], ["宫中"]),
    "梁多瑞": ("男", "太监", ["太监"], ["宫中"]),
    "姜忠敏": ("男", "太监", ["太监"], ["宫中"]),
    "精奇嬷嬷": ("女", "嬷嬷", ["宫女"], ["宫中"]),
    "祺嫔": None,
    "淳贵人": None,
    "余答应": None,
    "采苹": None,
}


def uniq(xs):
    out, seen = [], set()
    for x in xs:
        if x and x not in seen:
            seen.add(x)
            out.append(x)
    return out


def merge_person(base: dict, patch: dict) -> dict:
    """浅合并人物补丁：aliases 整表替换；attrs 内字段整表替换。"""
    out = dict(base)
    if "aliases" in patch:
        out["aliases"] = patch["aliases"]
    if "attrs" in patch:
        attrs = dict(out.get("attrs") or {})
        attrs.update(patch["attrs"])
        out["attrs"] = attrs
    return out


def load_episode_patches() -> dict:
    merged: dict = {}
    for path in EP_PATCHES:
        if not path.exists():
            continue
        data = json.loads(path.read_text(encoding="utf-8"))
        for name, patch in data.items():
            merged[name] = merge_person(merged.get(name, {}), patch)
    return merged


def main():
    ents = json.loads(ENTITIES.read_text(encoding="utf-8"))
    by_name = {e["name"]: e for e in ents}

    # drop duplicates
    ents = [e for e in ents if e["name"] not in DROP_NAMES]
    by_name = {e["name"]: e for e in ents}

    # drop role-fix Nones that are duplicate titles already covered
    for name, fix in list(ROLE_FIXES.items()):
        if fix is None and name in by_name:
            del by_name[name]
    ents = [e for e in ents if e["name"] in by_name]

    curated = dict(BY_NAME)
    for name, patch in load_episode_patches().items():
        curated[name] = merge_person(curated.get(name, {}), patch)

    # apply curated
    for name, patch in curated.items():
        e = by_name.get(name)
        if not e:
            # 已 DROP 的名字在分集补丁里可能仍出现，忽略即可
            continue
        if "aliases" in patch:
            e["aliases"] = patch["aliases"]
        attrs = patch.get("attrs") or {}
        for k in ("gender", "role_type", "titles", "residences"):
            if k in attrs:
                e["attrs"][k] = attrs[k]
        if "major_plots" in attrs:
            plots = attrs["major_plots"]
            bad = [p for p in plots if p not in PLOTS]
            if bad:
                raise SystemExit(f"unknown plots on {name}: {bad}")
            e["attrs"]["major_plots"] = plots
        if "connections" in attrs:
            conn_names = attrs["connections"]
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

    # 核心圈关系网（覆盖补丁里可能膨胀的 connections）
    for name, conn_names in CORE_CONNECTIONS.items():
        e = by_name.get(name)
        if not e:
            continue
        ids = []
        for cn in conn_names[:MAX_CONNECTIONS]:
            ce = by_name.get(cn)
            if not ce:
                print("WARN core conn missing", name, "->", cn)
                continue
            ids.append(ce["id"])
        e["attrs"]["connections"] = uniq(ids)

    # 亲属边：双向强制写入，插到最前，再截断到上限
    def ensure_family_link(src_name: str, dst_name: str) -> None:
        src = by_name.get(src_name)
        dst = by_name.get(dst_name)
        if not src or not dst:
            print("WARN family bond missing", src_name, "<->", dst_name)
            return
        dst_id = dst["id"]
        cur = [c for c in src["attrs"].get("connections", []) if c != dst_id]
        src["attrs"]["connections"] = uniq([dst_id] + cur)[:MAX_CONNECTIONS]

    for a, b in FAMILY_BONDS:
        ensure_family_link(a, b)
        ensure_family_link(b, a)

    # 服务边：只写从属方，主角名额不够是预期
    for sub, principal in SERVICE_LINKS:
        ensure_family_link(sub, principal)

    # 重大剧情权威名单（覆盖分集补丁的宽松挂载）
    for e in ents:
        e["attrs"]["major_plots"] = []
    for plot, names in CORE_MAJOR_PLOTS.items():
        if plot not in PLOTS:
            raise SystemExit(f"unknown plot in CORE_MAJOR_PLOTS: {plot}")
        for name in names:
            e = by_name.get(name)
            if not e:
                print("WARN plot cast missing", plot, "->", name)
                continue
            e["attrs"]["major_plots"] = uniq(e["attrs"].get("major_plots", []) + [plot])

    # strip obsolete plot key / dangling ids；其余角色也硬截断到上限
    ids = {x["id"] for x in ents}
    empty_conn = []
    for e in ents:
        e["attrs"]["major_plots"] = [p for p in e["attrs"].get("major_plots", []) if p in PLOTS]
        conns = [c for c in e["attrs"].get("connections", []) if c in ids]
        if len(conns) > MAX_CONNECTIONS:
            print(f"TRIM {e['name']}: {len(conns)} -> {MAX_CONNECTIONS}")
            conns = conns[:MAX_CONNECTIONS]
        e["attrs"]["connections"] = conns
        if not conns:
            empty_conn.append(e["name"])
    if empty_conn:
        print(f"NOTE empty connections ({len(empty_conn)}): {', '.join(empty_conn[:40])}")

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
