#!/usr/bin/env python3
"""Build zhenhuan_2011/entities.json from Wikipedia dump + supplemental roster."""
import hashlib
import json
import re
from collections import Counter
from pathlib import Path

import zhconv

ROOT = Path(__file__).resolve().parents[1]
WIKI = ROOT / "scripts/data/zhenhuan_wiki_character_list.md"
OUT = ROOT / "src/main/resources/packs/zhenhuan_2011/entities.json"
# Core rich characters (kept when regenerating). Fallback: git version of entities.json.
SEED = ROOT / "scripts/data/zhenhuan_core_seed.json"
LEGACY = SEED if SEED.exists() else ROOT / "src/main/resources/packs/zhenhuan_2011/entities.json"


def simp(s: str) -> str:
    return zhconv.convert(s, "zh-cn")


def clean_name(raw: str) -> str:
    s = simp(raw.strip())
    s = re.sub(r"\s+", "", s)
    s = re.sub(r"[（(][^）)]*[）)]", "", s)
    s = s.strip("。．.、,，)）(（•· ")
    s = re.sub(r"\[\d+\]", "", s)
    if s.startswith("浣碧"):
        s = "浣碧"
    return s


def make_id(name: str) -> str:
    return "zh_" + hashlib.md5(name.encode()).hexdigest()[:10]


# 无名凑数 / 仅职务无具体人名（不可作为猜题对象）
ANON_EXACT = {
    "御书房太监", "果郡王府管家", "御前侍卫章京", "乾清门侍卫",
    "交芦馆宫女", "闲月阁宫女", "乐道堂宫女", "钟粹宫宫女", "启祥宫宫女", "长春宫宫女",
    "清凉殿太监", "清凉台侍女", "四执库太监",
}


# 维基标「仅台词」但仍值得入题的重要人物（剧情提及频繁）
KEEP_DESPITE_DIALOGUE = {
    "纯元皇后", "孙答应", "五阿哥", "朝瑰公主",
    "年斌", "年富", "年兴", "赵之垣", "沈自山", "安比槐",
}

# 维基「仅台词/未正式登场」且不入题（爵号空壳、路人点名等）
DIALOGUE_ONLY_NAMES = {
    "雍亲王侧福晋", "裕嫔", "李金桂", "博尔济吉特贵人",
    "宜太妃", "允禩", "允禟", "允祥", "允禵", "高氏",
    "廉亲王", "奕郡王", "怡亲王", "恂郡王",
}


def is_concrete_person(name: str) -> bool:
    """仅保留有名有姓或剧中可辨识的具体人物，排除甲乙凑数与匿名职务。"""
    if not name or len(name) < 2:
        return False
    if re.search(r"[甲乙丙丁戊己庚辛壬癸]", name):
        return False
    if name in ANON_EXACT:
        return False
    if name in DIALOGUE_ONLY_NAMES and name not in KEEP_DESPITE_DIALOGUE:
        return False
    # 地点/机构 + 通用职务，且无姓氏名号（如「养心殿太监」）
    if re.fullmatch(
        r".+(宫女|太监|侍女|仆人|丫鬟|秀女|侍卫|番子|尼姑|医官|使者|解差|官员|笔帖式|驯马人|管家|章京)$",
        name,
    ):
        return False
    return True


def uniq(xs):
    out, seen = [], set()
    for x in xs:
        if x and x not in seen:
            seen.add(x)
            out.append(x)
    return out


def infer(name: str, section: str = "", intro: str = ""):
    s = simp(section + " " + intro + " " + name)
    gender, role, titles, res = "女", "宫女", ["宫女"], ["宫中"]

    if any(k in name for k in [
        "皇帝", "胤禛", "阿哥", "亲王", "郡王", "贝勒", "弘", "允", "摩格",
        "年羹尧", "甄远道", "隆科多", "鄂敏", "张廷玉", "沈自山", "安比槐",
        "太医", "侍卫", "解差", "官员", "章京", "笔帖式", "番子", "使者", "医官",
        "总管", "小允", "小厦", "太监",
    ]):
        gender = "男"

    if any(k in s for k in ["太监", "小允", "小厦", "小印", "小荷", "小卫", "江福海",
                             "周宁海", "肃喜", "康禄海", "梁多瑞", "黄规全", "姜忠敏",
                             "苟总管", "徐进良"]):
        return "男", "太监", ["太监"], ["宫中"]
    if any(k in s for k in ["太医", "温实初", "卫临", "章弥", "江诚", "江慎", "刘畚", "许太医", "医官"]):
        return "男", "太医", ["太医"], ["宫中"]
    if any(k in s for k in ["侍卫", "夏刈", "血滴子", "粘竿", "番子", "章京"]):
        return "男", "侍卫", ["侍卫"], ["宫中"]
    if "嬷嬷" in name:
        return "女", "嬷嬷", ["宫女"], ["宫中"]
    if any(k in s for k in ["宫女", "陪嫁", "侍女", "婢", "姑姑", "芳若", "剪秋", "槿汐", "流朱", "浣碧", "花穗", "佩儿", "尼姑"]):
        place = "甘露寺" if "甘露" in s or "尼姑" in name else "宫中"
        return "女", "宫女", ["宫女"], [place]
    if any(k in name for k in ["甄远道", "隆科多", "鄂敏", "年羹尧", "张廷玉", "沈自山", "安比槐",
                               "笔帖式", "官员", "解差", "使者"]):
        return "男", "前朝官员", ["大臣"], ["宫外"]
    if any(k in s for k in ["皇族", "阿哥", "公主", "亲王", "郡王", "贝勒", "福晋", "太后", "皇帝", "胤", "允禧", "允礼"]):
        if gender == "女":
            if "公主" in name:
                titles = ["公主"]
            elif "太后" in name:
                titles = ["太后"]
            elif "皇后" in name:
                titles = ["皇后"]
            elif "福晋" in name:
                titles = ["亲王"]
            else:
                titles = ["皇族"]
        else:
            if "胤禛" in name or "皇帝" in name:
                titles = ["皇帝"]
            elif "亲王" in name:
                titles = ["亲王"]
            else:
                titles = ["皇族"]
        return gender if gender else "男", "皇族", titles, ["宫中"]
    if any(k in s for k in ["妃", "嫔", "贵人", "常在", "答应", "皇后", "后宫"]):
        mapped = []
        for t in ["皇贵妃", "贵妃", "妃", "嫔", "贵人", "常在", "答应", "官女子", "皇后", "太后"]:
            if t in s:
                mapped.append(t)
        return "女", "后宫嫔妃", mapped or ["答应"], ["宫中"]

    if gender == "男":
        return "男", "前朝官员", ["大臣"], ["宫外"]
    return "女", "宫女", ["宫女"], ["宫中"]


OVERRIDES = {
    "乌拉那拉·宜修": ("女", "后宫嫔妃", ["皇后"], ["景仁宫"]),
    "爱新觉罗·胤禛": ("男", "皇族", ["皇帝"], ["养心殿"]),
    "乌雅氏": ("女", "皇族", ["太后"], ["寿康宫"]),
    "年世兰": ("女", "后宫嫔妃", ["贵妃", "妃", "答应"], ["翊坤宫"]),
    "沈眉庄": ("女", "后宫嫔妃", ["贵人", "嫔", "妃"], ["咸福宫"]),
    "安陵容": ("女", "后宫嫔妃", ["答应", "常在", "贵人", "嫔"], ["延禧宫"]),
    "叶澜依": ("女", "后宫嫔妃", ["答应", "贵人", "嫔"], ["宫中"]),
    "苏培盛": ("男", "太监", ["总管太监"], ["养心殿"]),
    "温实初": ("男", "太医", ["太医"], ["宫中"]),
    "允礼": ("男", "皇族", ["亲王"], ["宫外"]),
    "浣碧": ("女", "宫女", ["宫女"], ["碎玉轩"]),
    "崔槿汐": ("女", "宫女", ["宫女"], ["碎玉轩"]),
    "流朱": ("女", "宫女", ["宫女"], ["碎玉轩"]),
    "剪秋": ("女", "宫女", ["宫女"], ["景仁宫"]),
    "曹贵人": ("女", "后宫嫔妃", ["贵人", "嫔"], ["储秀宫"]),
    "富察·温宜": ("女", "皇族", ["公主"], ["景仁宫"]),
    "纯元皇后": ("女", "后宫嫔妃", ["皇后"], ["宫外"]),
    "沈自山": ("男", "前朝官员", ["大臣"], ["宫外"]),
    "安比槐": ("男", "前朝官员", ["大臣"], ["宫外"]),
    "孙答应": ("女", "后宫嫔妃", ["答应"], ["宫中"]),
    "五阿哥": ("男", "皇族", ["皇族"], ["宫外"]),
    "朝瑰公主": ("女", "皇族", ["公主"], ["宫外"]),
    "赵之垣": ("男", "前朝官员", ["大臣"], ["宫外"]),
    "年斌": ("男", "前朝官员", ["大臣"], ["宫外"]),
    "年富": ("男", "前朝官员", ["大臣"], ["宫外"]),
    "年兴": ("男", "前朝官员", ["大臣"], ["宫外"]),
}

ALIAS_MAP = {
    "甄嬛": ["嬛嬛", "莞莞", "熹贵妃", "圣母皇太后"],
    "乌拉那拉·宜修": ["皇后", "宜修"],
    "爱新觉罗·胤禛": ["皇上", "四爷", "雍正", "胤禛"],
    "乌雅氏": ["太后", "德妃", "成璧"],
    "年世兰": ["华妃", "年妃"],
    "安陵容": ["陵容", "鸝妃"],
    "沈眉庄": ["眉庄", "惠贵妃"],
    "叶澜依": ["宁嫔"],
    "浣碧": ["玉隐", "钮祜禄·玉隐"],
    "崔槿汐": ["槿汐"],
    "允礼": ["果郡王", "果亲王"],
    "允禧": ["慎郡王", "慎贝勒"],
    "允祺": ["恒亲王"],
    "允䄉": ["敦亲王"],
    "甄玉娆": ["玉娆"],
    "弘历": ["四阿哥", "乾隆"],
    "弘时": ["三阿哥"],
    "胧月公主": ["绾绾", "胧月"],
    "曹贵人": ["曹琴默", "襄嫔"],
    "纯元皇后": ["菀菀", "纯元"],
    "沈自山": ["沈协领", "眉庄之父"],
    "安比槐": ["安县丞", "陵容之父"],
}

NAME_MAP = {
    "宜修": "乌拉那拉·宜修",
    "胤禛": "爱新觉罗·胤禛",
    "成璧": "乌雅氏",
    "曹琴默": "曹贵人",
    "温宜公主": "富察·温宜",
    # 爵号归并到具名人物，避免「慎郡王」与「允禧」重复
    "慎郡王": "允禧",
    "恒亲王": "允祺",
    "敦亲王": "允䄉",
    "果郡王": "允礼",
    "果亲王": "允礼",
}

# 维基标注「仅台词 / 未正式登场」等：不可作猜题对象
DIALOGUE_ONLY_MARKERS = (
    "仅台词", "僅台詞",
    "仅于对白", "僅於對白",
    "未正式登场", "未正式登場",
    "人物未正式登",
)


def is_dialogue_only(text: str) -> bool:
    t = simp(text or "")
    return any(m in t for m in DIALOGUE_ONLY_MARKERS)


def main():
    wiki = WIKI.read_text(encoding="utf-8")
    if LEGACY.exists():
        existing = json.loads(LEGACY.read_text(encoding="utf-8"))
    else:
        existing = []

    rows = []
    section = "其他"
    for line in wiki.splitlines():
        if line.startswith("##") or line.startswith("###"):
            section = simp(line.lstrip("#").strip())
            continue
        if not line.startswith("|"):
            continue
        cols = [c.strip() for c in line.strip("|").split("|")]
        if not cols or cols[0].startswith("---") or "演员" in simp(cols[0]) or "演員" in cols[0]:
            continue
        if len(cols) < 2:
            continue
        n = clean_name(cols[1])
        if not n or len(n) < 2 or len(n) > 16:
            continue
        if not re.search(r"[\u4e00-\u9fff]", n):
            continue
        if n in ("角色", "介绍", "备注", "电视剧结局", "不详", "参见主要角色"):
            continue
        if "居住" in n or "主要身份" in n:
            continue
        intro = simp(cols[2]) if len(cols) > 2 else ""
        row_text = simp("|".join(cols))
        n2 = NAME_MAP.get(n, n)
        n2 = simp(n2)
        if (is_dialogue_only(intro) or is_dialogue_only(row_text)) and n2 not in KEEP_DESPITE_DIALOGUE:
            continue
        rows.append((n, section, intro))

    # 仅补充有戏份的具名人物；台词点到为止的爵号空壳不进名册
    # KEEP_DESPITE_DIALOGUE 中重要提及角色仍可入题
    extra = """
纯元皇后 端妃 敬妃 齐妃 襄嫔 祺嫔 淳贵人 宁嫔 欣嫔 夏常在 富察贵人 丽嫔 瑛贵人
余答应 芝答应 芳贵人 孙答应
舒太妃 孟静娴 敦亲王福晋
甄玉娆 弘时 弘历 五阿哥 富察氏皇后 娴妃 弘曕 朝瑰公主 温宜公主 胧月公主 灵犀公主 静和公主 元澈
花穗 斐雯 玢儿 宝鹃 宝鹊 菊青 吉祥 孙竹息 春貌 芳若 剪秋 福子 佩儿 绘春 翠果 刘嬷嬷 品儿 雨儿 灵芝 采月 采星 茯苓 采蓝 如意 含珠 音袖 桑儿 阿绿 张嬷嬷 积云 景泰 精奇嬷嬷
小允子 黄规全 姜忠敏 苟总管 梁多瑞 卫临 章弥 江诚 江慎 刘畚 许太医 江福海 周宁海 肃喜 康禄海 小印子 小荷子 小卫子 阿晋 夏刈 季惟生 小厦子 徐进良
甄远道 隆科多 鄂敏 年羹尧 年斌 年富 年兴 张廷玉 赵之垣 沈自山 安比槐
甄母 静岸 静白 莫言 摩格 刘莲子 孙妙青
"""

    all_names = []
    seen = set()
    for e in existing:
        n = e["name"]
        if n not in seen and is_concrete_person(n):
            seen.add(n)
            all_names.append(("existing", n, e))

    for n, sec, intro in rows:
        n2 = NAME_MAP.get(n, n)
        n2 = simp(n2)
        if n2 in seen or not is_concrete_person(n2):
            continue
        seen.add(n2)
        all_names.append(("wiki", n2, (sec, intro)))

    for part in re.split(r"[\s\n]+", extra.strip()):
        n = simp(part.strip())
        if not n or len(n) < 2 or n in seen or not is_concrete_person(n):
            continue
        seen.add(n)
        all_names.append(("extra", n, None))

    entities = []
    used_ids = set()
    for kind, name, payload in all_names:
        if kind == "existing":
            entities.append(payload)
            used_ids.add(payload["id"])
            continue
        sec, intro = ("其他", "") if payload is None else payload
        if name in OVERRIDES:
            gender, role, titles, res = OVERRIDES[name]
        else:
            gender, role, titles, res = infer(name, sec, intro)
        eid = make_id(name)
        while eid in used_ids:
            eid = make_id(name + eid)
        used_ids.add(eid)
        entities.append({
            "id": eid,
            "name": name,
            "aliases": ALIAS_MAP.get(name, []),
            "attrs": {
                "gender": gender,
                "role_type": role,
                "titles": uniq(titles),
                "residences": uniq(res),
                "major_plots": [],
                "connections": [],
            },
        })

    OUT.write_text(json.dumps(entities, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("wrote", len(entities), "entities")
    print(Counter(e["attrs"]["role_type"] for e in entities))
    places = sorted({p for e in entities for p in e["attrs"]["residences"]})
    print("places", places)


if __name__ == "__main__":
    main()
