/**
 * 历史源流 · 时间轴渲染：政权泳道 / 帝王轨 / 年号轨 / 事件点 / 刻度 / 选中线。
 * 纯渲染，只设置 data-eid；点击交互由 app.js 事件委托处理。
 */
window.HistoryStream = window.HistoryStream || {};
HistoryStream.timeline = (function () {
  const EVENT_LABEL_YEARS = 140;

  function esc(s) {
    return String(s ?? '').replace(/[&<>"']/g, (c) => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
  }

  function yearLabel(y) {
    const n = Math.round(y);
    if (n < 0) return '前' + (-n);
    if (n === 0) return '公元元年';
    return String(n) + '年';
  }

  function kindLabel(kind) {
    if (kind === 'battle') return '战役';
    if (kind === 'culture') return '文化';
    return '时局';
  }

  function oneLine(s) { return String(s ?? '').replace(/\s+/g, ' ').trim(); }

  function clamp(n, min, max) { return Math.max(min, Math.min(max, n)); }

  function rangeFromCenter(S) {
    const min = S.meta ? S.meta.minYear : -2070;
    const max = S.meta ? S.meta.maxYear : 1949;
    let from = S.center - S.radius;
    let to = S.center + S.radius;
    if (from < min) { to += (min - from); from = min; }
    if (to > max) { from -= (to - max); to = max; }
    from = clamp(from, min, max);
    to = clamp(to, min, max);
    if (from > to) { const t = from; from = to; to = t; }
    return { from, to };
  }

  function pct(year, from, to) {
    if (to === from) return 0;
    return ((year - from) / (to - from)) * 100;
  }

  function barStyle(item, from, to) {
    const left = Math.max(0, pct(Math.max(item.fromYear, from), from, to));
    const right = Math.min(100, pct(Math.min(item.toYear, to) + (to > from ? 0.35 : 0), from, to));
    return { left: left + '%', width: Math.max(1.2, right - left) + '%' };
  }

  /** 贪心把重叠区间排进泳道。 */
  function packLanes(items) {
    const sorted = items.slice().sort((a, b) => a.fromYear - b.fromYear || a.toYear - b.toYear);
    const laneEnds = [];
    const placed = [];
    sorted.forEach((item) => {
      let lane = laneEnds.findIndex((end) => item.fromYear > end);
      if (lane < 0) {
        lane = laneEnds.length;
        laneEnds.push(item.toYear);
      } else {
        laneEnds[lane] = item.toYear;
      }
      placed.push({ item, lane });
    });
    return placed;
  }

  function appendPackedBars(lanesRoot, items, className, labelFn, from, to) {
    const packed = packLanes(items);
    const maxLane = packed.reduce((m, x) => Math.max(m, x.lane), 0);
    for (let i = 0; i <= maxLane; i++) {
      const lane = document.createElement('div');
      lane.className = 'lane' + (className === 'reign' ? ' reign-lane' : '');
      packed.filter((x) => x.lane === i).forEach(({ item }) => {
        const style = barStyle(item, from, to);
        const bar = document.createElement('div');
        bar.className = 'bar ' + className + (item.uncertain ? ' uncertain' : '');
        bar.dataset.eid = className + ':' + (item.id || '');
        bar.style.left = style.left;
        bar.style.width = style.width;
        const w = parseFloat(style.width);
        const approx = item.uncertain ? '约' : '';
        bar.textContent = labelFn(item, w);
        bar.dataset.tipTitle = (item.uncertain ? '约 ' : '') + labelFn(item, 100);
        bar.dataset.tipSub = approx + yearLabel(item.fromYear) + '–' + approx + yearLabel(item.toYear)
          + (item.note ? ' · ' + oneLine(item.note) : '');
        lane.appendChild(bar);
      });
      lanesRoot.appendChild(lane);
    }
  }

  function appendPolityBars(lanesRoot, items, from, to) {
    const laneMap = new Map();
    items.forEach((p) => {
      const key = String(p.lane ?? 0);
      if (!laneMap.has(key)) laneMap.set(key, []);
      laneMap.get(key).push(p);
    });
    Array.from(laneMap.keys()).sort((a, b) => Number(a) - Number(b)).forEach((key) => {
      const lane = document.createElement('div');
      lane.className = 'lane';
      laneMap.get(key).forEach((p) => {
        const style = barStyle(p, from, to);
        const bar = document.createElement('div');
        const axisClass = p.axis === 'south' || p.axis === 'north' ? p.axis : (p.tier || 'spine');
        bar.className = 'bar ' + axisClass + ' ' + (p.tier || '') + (p.uncertain ? ' uncertain' : '');
        bar.dataset.eid = 'polity:' + p.id;
        bar.style.left = style.left;
        bar.style.width = style.width;
        const w = parseFloat(style.width);
        bar.textContent = (p.uncertain && w >= 8 ? '约·' : '') + (w < 6 ? (p.shortName || p.name) : p.name);
        bar.dataset.tipTitle = (p.uncertain ? '约 ' : '') + p.name;
        bar.dataset.tipSub = yearLabel(p.fromYear) + '–' + yearLabel(p.toYear)
          + (p.capital ? ' · 都城 ' + p.capital : '')
          + (p.note ? ' · ' + oneLine(p.note) : '');
        lane.appendChild(bar);
      });
      lanesRoot.appendChild(lane);
    });
  }

  function renderEventTrack(board, S, from, to) {
    const track = document.createElement('div');
    track.className = 'track';
    const title = document.createElement('div');
    title.className = 'axis-title';
    title.textContent = '事件点轨';
    track.appendChild(title);

    const events = S.cache.events.slice()
      .filter((e) => S.kinds[e.kind] !== false)
      .sort((a, b) => a.year - b.year || (b.importance || 0) - (a.importance || 0));
    if (!events.length) {
      const empty = document.createElement('div');
      empty.className = 'empty-inline';
      empty.style.padding = '4px';
      empty.textContent = '此区间暂无事件点';
      track.appendChild(empty);
      board.appendChild(track);
      return;
    }

    const showLabels = (to - from) <= EVENT_LABEL_YEARS;
    const rowEnds = [];
    events.forEach((e) => {
      const x = pct(e.year, from, to);
      let row = 0;
      if (showLabels) {
        row = rowEnds.findIndex((endX) => x - endX > 7);
        if (row < 0) {
          row = rowEnds.length;
          rowEnds.push(x + 7);
        } else {
          rowEnds[row] = x + 7;
        }
      } else {
        row = events.indexOf(e) % 2;
      }
      const node = document.createElement('div');
      const k = e.kind === 'politics' || e.kind === 'culture' || e.kind === 'battle' ? e.kind : 'politics';
      const imp = e.importance || 3;
      node.className = 'dot ' + k + ' i' + imp + (showLabels ? '' : ' no-label');
      node.dataset.eid = 'event:' + e.eventId;
      node.style.left = x + '%';
      node.style.top = (14 + row * 16) + 'px';
      node.dataset.tipTitle = yearLabel(e.year) + ' · ' + e.title;
      node.dataset.tipSub = kindLabel(e.kind) + ' · 重要度 ' + imp
        + (e.summary ? ' · ' + oneLine(e.summary) : '');
      const tip = document.createElement('span');
      tip.style.top = '16px';
      tip.textContent = e.title;
      node.appendChild(tip);
      track.appendChild(node);
    });
    track.style.minHeight = (36 + Math.max(1, showLabels ? rowEnds.length : 2) * 16) + 'px';
    board.appendChild(track);
  }

  function niceStep(span) {
    const raw = span / 8;
    const pow = Math.pow(10, Math.floor(Math.log10(Math.max(raw, 1))));
    const n = raw / pow;
    let step;
    if (n <= 1) step = 1;
    else if (n <= 2) step = 2;
    else if (n <= 5) step = 5;
    else step = 10;
    return step * pow;
  }

  function renderTicks(container, S, from, to) {
    const ticks = document.createElement('div');
    ticks.className = 'ticks';
    const span = Math.max(1, to - from);
    const major = niceStep(span);
    const minor = major / 5;
    const start = Math.ceil(from / minor) * minor;
    for (let y = start; y <= to + 1e-9; y += minor) {
      const year = Math.round(y);
      const majorOk = Math.round(year / major) * major === year;
      const node = document.createElement('div');
      node.className = 'tick ' + (majorOk ? 'major' : 'minor');
      node.style.left = pct(year, from, to) + '%';
      if (majorOk) {
        const lab = document.createElement('span');
        lab.textContent = yearLabel(year).replace(/年$/, '');
        node.appendChild(lab);
      }
      ticks.appendChild(node);
    }
    const sel = document.createElement('div');
    sel.className = 'tick major';
    sel.style.left = pct(S.year, from, to) + '%';
    sel.style.background = 'var(--accent)';
    sel.style.height = '16px';
    const selLab = document.createElement('span');
    selLab.textContent = '本年';
    selLab.style.color = 'var(--accent)';
    sel.appendChild(selLab);
    ticks.appendChild(sel);
    const years = new Set((S.cache.events || []).map((e) => e.year));
    years.forEach((y) => {
      if (y < from || y > to || y === S.year) return;
      const heat = document.createElement('div');
      heat.className = 'heat';
      heat.style.left = pct(y, from, to) + '%';
      ticks.appendChild(heat);
    });
    container.appendChild(ticks);
  }

  function renderBoard(S) {
    const board = document.getElementById('board');
    board.innerHTML = '';
    const inner = document.createElement('div');
    inner.className = 'board-inner';
    board.appendChild(inner);
    const { from, to } = rangeFromCenter(S);
    let any = false;

    if (S.layers.polity) {
      const byBucket = { spine: [], south: [], north: [], parallel: [], fragment: [] };
      S.cache.polities.forEach((p) => {
        if (p.tier === 'fragment') byBucket.fragment.push(p);
        else if (p.axis === 'south') byBucket.south.push(p);
        else if (p.axis === 'north') byBucket.north.push(p);
        else if (p.tier === 'parallel') byBucket.parallel.push(p);
        else byBucket.spine.push(p);
      });
      [
        ['spine', '① 主线政权'],
        ['south', '② 南方'],
        ['north', '③ 北方'],
        ['parallel', '④ 重要并立'],
        ['fragment', '⑤ 割据'],
      ].forEach(([key, title]) => {
        const list = byBucket[key];
        if (!list.length) return;
        any = true;
        const block = document.createElement('div');
        block.className = 'axis-block';
        block.innerHTML = '<div class="axis-title">' + title + '</div>';
        const lanesRoot = document.createElement('div');
        lanesRoot.className = 'lanes';
        appendPolityBars(lanesRoot, list, from, to);
        block.appendChild(lanesRoot);
        inner.appendChild(block);
      });
    }

    if (S.layers.ruler && S.cache.rulers.length) {
      const visiblePolity = new Set((S.cache.polities || []).map((p) => p.id));
      const visibleRulers = S.cache.rulers.filter((r) => !r.polityId || visiblePolity.has(r.polityId));
      if (visibleRulers.length) {
        any = true;
        const block = document.createElement('div');
        block.className = 'axis-block';
        block.innerHTML = '<div class="axis-title">人物轨（帝 / 王）</div>';
        const lanesRoot = document.createElement('div');
        lanesRoot.className = 'lanes';
        appendPackedBars(lanesRoot, visibleRulers, 'ruler',
          (r, w) => (w < 5 ? r.name : (r.name + (r.personalName ? '·' + r.personalName : ''))),
          from, to);
        block.appendChild(lanesRoot);
        inner.appendChild(block);
      }
    }

    if (S.layers.reign && S.cache.reigns.length) {
      any = true;
      const block = document.createElement('div');
      block.className = 'axis-block';
      block.innerHTML = '<div class="axis-title">年号轨</div>';
      const lanesRoot = document.createElement('div');
      lanesRoot.className = 'lanes';
      appendPackedBars(lanesRoot, S.cache.reigns, 'reign', (r) => r.name, from, to);
      block.appendChild(lanesRoot);
      inner.appendChild(block);
    }

    if (S.layers.event) {
      renderEventTrack(inner, S, from, to);
      any = true;
    }

    if (S.cache.markers.length) {
      const row = document.createElement('div');
      row.className = 'track thin';
      S.cache.markers.forEach((m) => {
        const node = document.createElement('div');
        node.className = 'marker';
        node.style.left = pct(m.year, from, to) + '%';
        node.title = m.name + ' · ' + yearLabel(m.year);
        row.appendChild(node);
      });
      inner.appendChild(row);
    }

    renderTicks(inner, S, from, to);
    const scale = document.createElement('div');
    scale.className = 'scale';
    scale.innerHTML = '<span>' + yearLabel(from) + '</span><span>选中 ' + yearLabel(S.year)
      + '</span><span>' + yearLabel(to) + '</span>';
    inner.appendChild(scale);

    if (!any) {
      const empty = document.createElement('div');
      empty.className = 'empty-inline';
      empty.style.padding = '24px';
      empty.style.textAlign = 'center';
      empty.textContent = '打开上方图层，或切换时代查看';
      inner.appendChild(empty);
    }

    placeYearLine(S);
    applySelectionDim(S);
  }

  function placeYearLine(S) {
    const line = document.getElementById('yearLine');
    const shell = document.getElementById('boardShell');
    if (!line || !shell) return;
    const { from, to } = rangeFromCenter(S);
    const ratio = (to === from) ? 0.5 : (S.year - from) / (to - from);
    const x = Math.max(0, Math.min(1, ratio)) * shell.getBoundingClientRect().width;
    line.style.left = x + 'px';
    line.style.display = (S.year < from || S.year > to) ? 'none' : 'block';
    const label = line.querySelector('.year-line-label');
    if (label) label.textContent = yearLabel(S.year);
  }

  function selectionKey(S) {
    const d = S.detail;
    if (!d || !d.data) return null;
    if (d.kind === 'event') return d.data.eventId ? 'event:' + d.data.eventId : null;
    if (d.kind === 'polity') return d.data.id ? 'polity:' + d.data.id : null;
    if (d.kind === 'ruler') return d.data.id ? 'ruler:' + d.data.id : null;
    if (d.kind === 'reign') return d.data.id ? 'reign:' + d.data.id : null;
    return null;
  }

  function applySelectionDim(S) {
    const sel = selectionKey(S);
    document.querySelectorAll('.bar[data-eid], .dot[data-eid]').forEach((n) => {
      const on = !!sel && n.getAttribute('data-eid') === sel;
      n.classList.toggle('dim', !!sel && !on);
      n.classList.toggle('on', on);
    });
  }

  return { renderBoard, placeYearLine, applySelectionDim };
})();
