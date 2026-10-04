# 历史源流 · 年号

- JSON 数组；挂到 `rulerId` + `polityId`
- 必填：`id,name,fromYear,toYear,rulerId,polityId`
- 样例：`western_han.json`（文景武等）、`eastern_han.json`（光武至建安等，非全量）

分文件：
- `western_han.json` / `eastern_han.json`：两汉按年铺满（文景前元/后元至孺子居摄；光武至延康）
- `xin.json`：始建国–地皇
- `three_kingdoms.json`：魏蜀吴按年铺满
- `jin.json`：西晋泰始–建兴，东晋建武–元熙（勿与女真金 `jurchen_jin.json` 混淆）
- `nanbei.json`：北魏至北周、宋齐梁陈（西魏废帝/恭帝、北周孝闵以帝号记年）
- `sui_tang.json`：隋唐按年铺满（高宗/武周短号并存于改元当年）
- `five_dynasties.json`：后梁–后周
- `liao.json` / `western_xia.json` / `jurchen_jin.json`：辽、西夏、金按年铺满
- `song.json`：两宋年号按年铺满（建隆–祥兴）
- `yuan.json`：至元–至正（含后至元）
- `sixteen_kingdoms.json`：十六国及附列代/冉魏/西燕诸帝，按年可查（称王未建号者以王号记年）
- `ten_kingdoms.json`：十国诸王/帝年号（奉中朝正朔者以王号或中朝年号记年）
- `ming.json`：明全部年号（洪武–崇祯，含嘉靖）
- `qing.json`：后金天命/天聪 + 清崇德–宣统
