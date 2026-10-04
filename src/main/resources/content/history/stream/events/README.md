# 历史源流事件

- 按时代/题材分文件，如 `han.json`
- `eventId` 前缀建议 `HS-`
- `kind` 只许：`battle`（战役）/ `politics`（时局）/ `culture`（文化、文献）
- 成语用 `idioms: []` 挂在事件上，不另开轴
- 同一年同一事实只留一条（分文件不要各写一遍官渡/赤壁）
- 补数：一次只填一个时代文件；只收教材能点名的节点；战役进 `battle`，改朝/变法/和议进 `politics`，成书/科技进 `culture`。十六国/十国帝系与年号按政权逐年可查。
- 与 `content/history/days/`（历史上的今天）互不引用

已有样例：
- `pre_qin.json`：夏商周传说节点至秦灭六国（`HS-PQ-*`）
- `han.json`：两汉贯通关键节点
- `eastern_han.json`：东汉补充事件
- `qin_three_kingdoms_jin.json`：秦统一至刘宋代晋（三国、两晋主干事件）
- `nanbei_sui_tang.json`：南北朝至唐末（北魏统一、迁洛、隋统一、贞观、武周、开元、安史、黄巢、代唐等）
- `five_ten_song.json`：五代至南宋（陈桥、澶渊、变法、靖康、绍兴和议、灭金、崖山等；含辽/夏/金立国、雍熙北伐、隆兴和议、钓鱼城）
- `tang_late.json`：安史平定后至唐末（泾原、永贞、平蔡、甘露、庞勋、黄巢入京）
- `ten_kingdoms.json`：杨吴至宋灭北汉（含947入汴、高平；不重复后梁/陈桥）
