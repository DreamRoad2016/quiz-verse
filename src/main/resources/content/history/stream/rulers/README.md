# 历史源流 · 人物（帝 / 王）

- JSON 数组；`id` 建议 `wh-gaozu` 这类短 id
- 必填：`id,name,fromYear,toYear,polityId`
- 可选：`personalName,sort,note,uncertain,yearPrecision,onTimeline`
- 样例：`western_han.json`、`eastern_han.json`

约年 / 断代：`western_zhou.json` 西周诸王（`uncertain:true`, `yearPrecision:"approx"`）及共和行政条目。

- `eastern_zhou_states.json`：东周天子 + 齐晋楚秦燕赵魏韩吴越世系（早期约年；霸主及战国可考段标 exact）。秦王政挂 `state_qin` 至前222，统一后改见 `qin.json` 始皇。

分文件样例：
- `eastern_zhou_states.json`：东周天子与主要诸侯
- `qin.json`：秦三世
- `xin.json`：新朝王莽
- `three_kingdoms.json`：魏蜀吴诸帝
- `western_jin.json`、`eastern_jin.json`：两晋帝王
- `southern_dynasties.json`：南朝宋齐梁陈诸帝
- `northern_dynasties.json`：北魏至东西魏、北齐、北周主线
- `sui.json`：隋文帝、炀帝、恭帝
- `tang.json`：唐二十一帝（含武周、中宗/睿宗再位分段）
- `five_dynasties.json`：后梁唐晋汉周诸帝
- `song.json`：北宋、南宋诸帝
- `song_parallels.json`：辽、西夏、女真金诸帝
- `yuan.json`：元朝皇帝（1271起；1328–1332间略合并）
- `mongol_empire.json`：大蒙古国汗（1206–1271前）
- `ming.json`：明十七帝（含英宗前后两段）
- `qing.json`：后金努尔哈赤/皇太极与清帝（1636改国号分段）
- `sixteen_kingdoms.json`：十六国及附列代/冉魏/西燕
- `ten_kingdoms.json`：十国诸王/帝（杨吴至北汉）
