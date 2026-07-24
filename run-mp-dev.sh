#!/usr/bin/env bash
# 本地起后端（鉴权开、内存 store），供微信开发者工具联调，无需先发阿里云。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "${ROOT}"

# 可选：要测真 wx.login 再 export；不配也可，小程序会回退 guest
# export WECHAT_APP_ID=wx........
# export WECHAT_APP_SECRET=........

echo "Starting quiz-verse profile=mpdev (security on, memory match store) on :8098"
exec ./run.sh -Dspring-boot.run.profiles=mpdev "$@"
