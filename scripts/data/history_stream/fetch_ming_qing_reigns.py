#!/usr/bin/env python3
"""Fetch Ming/Qing reign table via DeepSeek for audit."""
from __future__ import annotations

import json
import os
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]


def load_env() -> None:
    env_path = ROOT / ".env"
    if not env_path.exists():
        return
    for line in env_path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        os.environ.setdefault(k.strip(), v.strip().strip('"').strip("'"))


def main() -> None:
    load_env()
    key = os.environ.get("DEEPSEEK_API_KEY", "").strip()
    if not key:
        raise SystemExit("缺少 DEEPSEEK_API_KEY")
    model = os.environ.get("DEEPSEEK_MODEL", "deepseek-v4-flash")
    payload = {
        "model": model,
        "messages": [
            {
                "role": "system",
                "content": "你是中国历史纪年专家。只输出合法 JSON 对象。公元年；闭区间 fromYear/toYear。",
            },
            {
                "role": "user",
                "content": """输出明清全部年号 JSON：
{"ming":[{"id":"ming-jiajing","name":"嘉靖","fromYear":1522,"toYear":1566,"rulerId":"ming_shizong","polityId":"ming","sort":120}],"qing":[...]}

明 rulerId 必须用：ming_taizu,ming_jianwen,ming_chengzu,ming_renzong,ming_xuanzong,ming_yingzong,ming_jingdi,ming_yingzong_restore,ming_xianzong,ming_xiaozong,ming_wuzong,ming_shizong,ming_muzong,ming_shenzong,ming_guangzong,ming_xizong,ming_sizong
清 rulerId/polity：hj_taizu/hou_jin(天命), hj_taizong/hou_jin(天聪1627-1635), qing_taizong/qing(崇德1636-1643), qing_shizu(顺治1644-1661), qing_shengzu(康熙1662-1722), qing_shizong(雍正1723-1735), qing_gaozong(乾隆1736-1795), qing_renzong(嘉庆1796-1820), qing_xuanzong(道光1821-1850), qing_wenzong(咸丰1851-1861), qing_muzong(同治1862-1874), qing_dezong(光绪1875-1908), qing_xuantong(宣统1909-1912)

明：洪武1368-1398,建文1399-1402,永乐1403-1424,洪熙1425,宣德1426-1435,正统1436-1449,景泰1450-1457,天顺1457-1464,成化1465-1487,弘治1488-1505,正德1506-1521,嘉靖1522-1566,隆庆1567-1572,万历1573-1620,泰昌1620,天启1621-1627,崇祯1628-1644。
必须包含嘉靖。""",
            },
        ],
        "response_format": {"type": "json_object"},
        "temperature": 0.1,
        "max_tokens": 8192,
    }
    req = urllib.request.Request(
        "https://api.deepseek.com/chat/completions",
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Authorization": "Bearer " + key,
            "Content-Type": "application/json",
        },
    )
    with urllib.request.urlopen(req, timeout=180) as resp:
        data = json.load(resp)
    content = data["choices"][0]["message"]["content"]
    obj = json.loads(content)
    out = Path(__file__).resolve().parent / "research" / "ming_qing_reigns_deepseek.json"
    out.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("wrote", out)
    print("ming", len(obj.get("ming", [])), "qing", len(obj.get("qing", [])))
    for x in obj.get("ming", []):
        print(f"  {x['name']} {x['fromYear']}-{x['toYear']} {x['rulerId']}")
    print("---")
    for x in obj.get("qing", []):
        print(f"  {x['name']} {x['fromYear']}-{x['toYear']} {x['rulerId']} {x.get('polityId')}")


if __name__ == "__main__":
    main()
