"""DeepSeek OpenAI-compatible client (V4 models only).

Official models (2026-07): deepseek-v4-flash, deepseek-v4-pro.
Do NOT use retired ids: deepseek-chat, deepseek-reasoner.
Docs: https://api-docs.deepseek.com/
"""
from __future__ import annotations

import json
import os
import time
from pathlib import Path
from typing import Any

from dotenv import load_dotenv
from openai import OpenAI

RETIRED_MODELS = {"deepseek-chat", "deepseek-reasoner"}
REPO_ROOT = Path(__file__).resolve().parents[3]


def _load_env() -> None:
    """Load repo-root .env if present (gitignored). Does not override existing env."""
    load_dotenv(REPO_ROOT / ".env", override=False)


class DeepSeekClient:
    def __init__(self, cfg_deepseek: dict[str, Any] | None = None):
        _load_env()
        cfg = cfg_deepseek or {}
        api_key = os.environ.get("DEEPSEEK_API_KEY", "").strip()
        if not api_key:
            raise SystemExit(
                "缺少 DEEPSEEK_API_KEY。\n"
                "请任选其一（不要把 key 贴到聊天）：\n"
                "  1) 项目根目录创建 .env，写入 DEEPSEEK_API_KEY=...\n"
                "  2) export DEEPSEEK_API_KEY=... 后在同一终端运行脚本"
            )
        base_url = cfg.get("base_url") or "https://api.deepseek.com"
        self.model = os.environ.get("DEEPSEEK_MODEL") or cfg.get("model") or "deepseek-v4-flash"
        self.model_hard = cfg.get("model_hard") or "deepseek-v4-pro"
        self._assert_model(self.model)
        self._assert_model(self.model_hard)
        self.thinking = (cfg.get("thinking") or "disabled").lower()
        self.reasoning_effort = cfg.get("reasoning_effort") or "high"
        self.temperature = float(cfg.get("temperature", 0.2))
        self.max_tokens = int(cfg.get("max_tokens", 16384))
        self.pause = float(cfg.get("request_pause_sec", 0.4))
        self.json_retries = int(cfg.get("json_retries", 3))
        timeout = float(cfg.get("timeout_sec", 180))
        self._client = OpenAI(api_key=api_key, base_url=base_url, timeout=timeout)

    @staticmethod
    def _assert_model(model: str) -> None:
        if model in RETIRED_MODELS:
            raise SystemExit(
                f"模型 {model} 已下线。请改用 deepseek-v4-flash 或 deepseek-v4-pro。"
            )

    def chat_json(
        self,
        *,
        system: str,
        user: str,
        hard: bool = False,
        thinking: str | None = None,
        max_tokens: int | None = None,
    ) -> dict[str, Any]:
        """Chat completion forcing JSON object response. Retries on bad JSON."""
        model = self.model_hard if hard else self.model
        think = (thinking or self.thinking).lower()
        thinking_payload = {"type": "enabled" if think == "enabled" else "disabled"}
        tokens = max_tokens if max_tokens is not None else self.max_tokens
        last_err: Exception | None = None
        content = ""

        for attempt in range(1, self.json_retries + 1):
            kwargs: dict[str, Any] = {
                "model": model,
                "messages": [
                    {"role": "system", "content": system},
                    {"role": "user", "content": user},
                ],
                "response_format": {"type": "json_object"},
                "max_tokens": tokens,
                "extra_body": {
                    "thinking": thinking_payload,
                },
            }
            if think != "enabled":
                kwargs["temperature"] = self.temperature
            else:
                kwargs["extra_body"]["reasoning_effort"] = self.reasoning_effort

            try:
                resp = self._client.chat.completions.create(**kwargs)
            except Exception as e:  # noqa: BLE001 — retry transient API/timeouts
                last_err = e
                print(
                    f"warn: API error attempt {attempt}/{self.json_retries}: {type(e).__name__}: {e}",
                    flush=True,
                )
                time.sleep(min(2 * attempt, 8))
                continue

            content = resp.choices[0].message.content or ""
            if self.pause > 0:
                time.sleep(self.pause)
            try:
                return json.loads(content)
            except json.JSONDecodeError as e:
                last_err = e
                print(
                    f"warn: JSON parse failed attempt {attempt}/{self.json_retries} "
                    f"(len={len(content)})",
                    flush=True,
                )
                tokens = min(tokens + 4096, 32768)

        raise RuntimeError(
            f"DeepSeek 调用失败（重试 {self.json_retries} 次）: {last_err}"
        ) from last_err
