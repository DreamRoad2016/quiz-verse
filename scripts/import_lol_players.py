#!/usr/bin/env python3
"""
将 scripts/data/lol-players-cn-kr.json 导入 PostgreSQL 表 demo_lol_player。

依赖：
  pip install -r scripts/requirements.txt

环境变量（可选，默认连本机）：
  PGHOST      默认 127.0.0.1
  PGPORT      默认 5432
  PGDATABASE  默认 quiz_verse
  PGUSER      默认当前系统用户名
  PGPASSWORD  默认空

用法：
  cd quiz-verse
  psql -U postgres -d quiz_verse -f db/demo_lol_schema.sql   # 先建表
  python3 scripts/import_lol_players.py
"""
from __future__ import annotations

import json
import os
import sys
from pathlib import Path

try:
    import psycopg2
    from psycopg2.extras import execute_batch
except ImportError:
    print("请先安装: pip install -r scripts/requirements.txt", file=sys.stderr)
    sys.exit(1)


ROOT = Path(__file__).resolve().parents[1]
JSON_PATH = ROOT / "scripts" / "data" / "lol-players-cn-kr.json"

INSERT_SQL = """
INSERT INTO demo_lol_player (
  id, game_id, real_name, age, current_team, historical_teams, region,
  identity_regions, positions, birthplace, champions, status,
  worlds_count, championships_count, avatar_url
) VALUES (
  %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
)
"""


def load_rows():
    with open(JSON_PATH, "r", encoding="utf-8") as f:
        payload = json.load(f)
    if not payload.get("success"):
        raise SystemExit("JSON success != true")
    rows = []
    for p in payload["data"]:
        rows.append(
            (
                p["id"],
                p["gameId"],
                p.get("realName"),
                p.get("age"),
                p["currentTeam"],
                p.get("historicalTeams") or [],
                p["region"],
                p.get("identityRegions") or [],
                p.get("positions") or [],
                p["birthplace"],
                p.get("champions") or [],
                p["status"],
                p.get("worldsCount", 0),
                p.get("championshipsCount", 0),
                p.get("avatarUrl"),
            )
        )
    return rows


def main():
    if not JSON_PATH.exists():
        print(f"找不到数据文件: {JSON_PATH}", file=sys.stderr)
        sys.exit(1)

    conn = psycopg2.connect(
        host=os.environ.get("PGHOST", "127.0.0.1"),
        port=os.environ.get("PGPORT", "5432"),
        dbname=os.environ.get("PGDATABASE", "quiz_verse"),
        user=os.environ.get("PGUSER", os.environ.get("USER", "postgres")),
        password=os.environ.get("PGPASSWORD", ""),
    )
    conn.autocommit = False
    rows = load_rows()
    try:
        with conn.cursor() as cur:
            cur.execute("TRUNCATE demo_lol_player;")
            execute_batch(cur, INSERT_SQL, rows, page_size=100)
        conn.commit()
        print(f"导入完成: {len(rows)} 条 -> demo_lol_player")
    except Exception as e:
        conn.rollback()
        print(f"导入失败: {e}", file=sys.stderr)
        sys.exit(1)
    finally:
        conn.close()


if __name__ == "__main__":
    main()
