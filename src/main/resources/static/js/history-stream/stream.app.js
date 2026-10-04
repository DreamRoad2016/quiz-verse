/**
 * 历史源流 · 正式页装配：状态、交互（翻年 / 点击 / 光标 / 键盘）、事件委托与启动。
 * 依赖 api.js、stream.data.js、stream.timeline.js、stream.yearpack.js。
 */
window.HistoryStream = window.HistoryStream || {};
HistoryStream.app = (function () {
  const FRAGMENT_AUTO_YEARS = 120;
  const REIGN_AUTO_YEARS = 80;

  const el = (id) => document.getElementById(id);

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

  function clamp(n, min, max) { return Math.max(min, Math.min(max, n)); }

  function rangeFromCenter() {
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

  function windowWidth() { return rangeFromCenter().to - rangeFromCenter().from; }

  function wantFragment() {
    return S.showFragment
      || windowWidth() <= FRAGMENT_AUTO_YEARS
      || S.activeEraId === 'pre_qin';
  }

  function setError(msg) {
    const box = el('err');
    if (!msg) { box.hidden = true; box.textContent = ''; return; }
    box.hidden = false;
    box.textContent = msg;
  }

  function syncReignLayer() {
    if (S.reignManual) return;
    const auto = windowWidth() <= REIGN_AUTO_YEARS;
    S.layers.reign = auto;
    el('layerReign').checked = auto;
  }

  function syncInputs() {
    const { from, to } = rangeFromCenter();
    el('centerYear').value = S.center;
    el('centerRange').value = S.center;
    document.querySelectorAll('.radius').forEach((btn) => {
      btn.classList.toggle('on', Number(btn.dataset.radius) === S.radius);
    });
    const frag = wantFragment();
    const w = windowWidth();
    el('rangeMeta').textContent =
      yearLabel(from) + ' — ' + yearLabel(to)
      + '  ·  选中 ' + yearLabel(S.year)
      + '  ·  ±' + S.radius
      + (frag ? '  ·  割据' : '')
      + (S.layers.reign ? '  ·  年号' : '')
      + '  ·  ←/→ 翻年';
  }

  function bindRangeLimits() {
    const min = S.meta.minYear;
    const max = S.meta.maxYear;
    el('centerYear').min = min;
    el('centerYear').max = max;
    el('centerRange').min = min;
    el('centerRange').max = max;
  }

  function renderEras() {
    const root = el('eras');
    root.innerHTML = '';
    S.eras.forEach((era) => {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'era' + (era.id === S.activeEraId ? ' on' : '');
      btn.textContent = era.label;
      btn.title = '跳到「' + era.label + '」起始年 ' + yearLabel(era.fromYear);
      btn.addEventListener('click', () => {
        S.activeEraId = era.id;
        if (S.radius > 100) S.radius = 100;
        setYear(era.fromYear, { pan: true, keepEra: true });
      });
      root.appendChild(btn);
    });
  }

  function ensureYearVisible(year) {
    const { from, to } = rangeFromCenter();
    if (year < from || year > to) {
      S.center = year;
      return true;
    }
    return false;
  }

  function renderYearPack() { HistoryStream.yearpack.render(S); }

  function applySelectionDim() { HistoryStream.timeline.applySelectionDim(S); }

  function placeYearLine() { HistoryStream.timeline.placeYearLine(S); }

  function closeDetail() {
    S.detail = null;
    applySelectionDim();
    renderYearPack();
  }

  function openEventDetail(e) {
    S.detail = { kind: 'event', data: e };
    renderYearPack();
    applySelectionDim();
    if (!e.description && e.eventId) {
      QuizApi.apiFetch('/api/history/stream/events/' + encodeURIComponent(e.eventId))
        .then((r) => (r.ok ? r.json() : e))
        .then((full) => {
          if (S.detail && S.detail.kind === 'event' && S.detail.data.eventId === e.eventId) {
            S.detail.data = full;
            renderYearPack();
          }
        })
        .catch(() => {});
    }
  }

  function openFigureDetail(f) {
    S.detail = { kind: 'figure', data: f };
    renderYearPack();
  }

  function openRulerDetail(r) {
    S.detail = { kind: 'ruler', data: r };
    renderYearPack();
    applySelectionDim();
  }

  function openReignDetail(r) {
    S.detail = { kind: 'reign', data: r };
    renderYearPack();
    applySelectionDim();
  }

  function openPolityDetail(p) {
    S.detail = { kind: 'polity', data: p };
    renderYearPack();
    applySelectionDim();
  }

  // —— 按 id 查找对象（优先 cache，其次当前 pack） ——
  function openPolityDetailById(id) {
    const p = (S.cache.polities || []).find((x) => x.id === id)
      || ((S.pack && S.pack.polities) || []).find((x) => x.id === id);
    if (p) openPolityDetail(p);
  }
  function openRulerDetailById(id) {
    const r = (S.cache.rulers || []).find((x) => x.id === id)
      || ((S.pack && S.pack.rulers) || []).find((x) => x.id === id);
    if (r) openRulerDetail(r);
  }
  function openReignDetailById(id) {
    const r = (S.cache.reigns || []).find((x) => x.id === id)
      || ((S.pack && S.pack.reigns) || []).find((x) => x.id === id);
    if (r) openReignDetail(r);
  }
  function openEventDetailById(id) {
    const e = (S.cache.events || []).find((x) => x.eventId === id)
      || ((S.pack && S.pack.events) || []).find((x) => x.eventId === id);
    if (e) openEventDetail(e);
  }
  function openFigureDetailById(id) {
    const f = ((S.pack && S.pack.figures) || []).find((x) => x.id === id);
    if (f) openFigureDetail(f);
  }

  async function applyPendingDetail(pending) {
    if (pending.kind === 'event') {
      const e = pending.data
        || (S.pack.events || []).find((x) => x.eventId === pending.eventId)
        || (S.cache.events || []).find((x) => x.eventId === pending.eventId);
      if (e) {
        if (!e.description) {
          try {
            const r = await QuizApi.apiFetch('/api/history/stream/events/' + encodeURIComponent(e.eventId));
            S.detail = { kind: 'event', data: r.ok ? await r.json() : e };
          } catch (_) {
            S.detail = { kind: 'event', data: e };
          }
        } else {
          S.detail = { kind: 'event', data: e };
        }
      }
    }
  }

  async function loadYearPack() {
    try {
      S.pack = await HistoryStream.data.loadYearPack(S.year);
      if (S.pendingDetail) {
        await applyPendingDetail(S.pendingDetail);
        S.pendingDetail = null;
      }
      renderYearPack();
      HistoryStream.data.prefetchNeighbors(S.year);
    } catch (e) {
      el('ypBody').innerHTML = '<div class="empty-inline">' + esc(e.message || e) + '</div>';
    }
  }

  async function refresh() {
    setError('');
    syncReignLayer();
    const tiers = wantFragment() ? 'spine,parallel,fragment' : 'spine,parallel';
    const { from, to } = rangeFromCenter();
    try {
      await HistoryStream.data.loadRange(from, to, tiers);
      syncInputs();
      HistoryStream.timeline.renderBoard(S);
      await loadYearPack();
      HistoryStream.data.writeUrl(S);
    } catch (e) {
      setError(String(e.message || e));
    }
  }

  function setYear(year, opts) {
    const o = opts || {};
    const next = clamp(Math.round(Number(year)), S.meta.minYear, S.meta.maxYear);
    const yearChanged = next !== S.year;
    S.year = next;
    if (yearChanged) S.detail = null;
    if (!o.keepEra) S.activeEraId = null;
    let windowMoved = false;
    if (o.pan) {
      S.center = next;
      windowMoved = true;
    } else {
      windowMoved = ensureYearVisible(next);
    }
    syncReignLayer();
    syncInputs();
    renderEras();
    if (windowMoved || o.refreshBoard) {
      refresh();
      return;
    }
    placeYearLine();
    if (yearChanged) applySelectionDim();
    if (yearChanged || S.pendingDetail) loadYearPack();
    HistoryStream.data.writeUrl(S);
  }

  function stepYear(delta) { setYear(S.year + delta); }

  function jumpRecorded(dir) {
    const pack = S.pack;
    const target = dir < 0 ? (pack && pack.prevRecordedYear) : (pack && pack.nextRecordedYear);
    if (target != null) setYear(target, { pan: true });
    else stepYear(dir);
  }

  function onCenterChange(val) { setYear(val, { pan: true, refreshBoard: true }); }

  // —— 交互绑定（只绑定一次，避免重复累积） ——
  function bindBoardInteraction() {
    const shell = el('boardShell');
    const board = el('board');
    const line = el('cursorLine');
    const badge = line.querySelector('.cursor-badge');

    const yearAt = (clientX) => {
      const rect = shell.getBoundingClientRect();
      const x = clientX - rect.left;
      if (x < 0 || x > rect.width) return null;
      const ratio = rect.width ? x / rect.width : 0;
      const { from, to } = rangeFromCenter();
      return Math.round(from + ratio * (to - from));
    };

    const tooltip = el('boardTooltip');
    const showTip = (node, ev) => {
      const rect = shell.getBoundingClientRect();
      tooltip.innerHTML = '';
      const strong = document.createElement('strong');
      strong.textContent = node.dataset.tipTitle || '';
      tooltip.appendChild(strong);
      if (node.dataset.tipSub) {
        const small = document.createElement('small');
        small.textContent = node.dataset.tipSub;
        tooltip.appendChild(small);
      }
      tooltip.classList.add('on');
      const tw = tooltip.offsetWidth;
      const th = tooltip.offsetHeight;
      let x = ev.clientX - rect.left + 14;
      let y = ev.clientY - rect.top + 14;
      if (x + tw > rect.width - 8) x = ev.clientX - rect.left - tw - 14;
      if (y + th > rect.height - 8) y = ev.clientY - rect.top - th - 14;
      tooltip.style.left = x + 'px';
      tooltip.style.top = y + 'px';
    };
    const hideTip = () => tooltip.classList.remove('on');

    shell.onmousemove = (ev) => {
      const rect = shell.getBoundingClientRect();
      const x = ev.clientX - rect.left;
      if (x < 0 || x > rect.width) { line.classList.remove('on'); hideTip(); return; }
      const ratio = rect.width ? x / rect.width : 0;
      const { from, to } = rangeFromCenter();
      S.hoverYear = from + ratio * (to - from);
      line.style.left = x + 'px';
      line.classList.add('on');
      line.classList.toggle('near-right', ratio > 0.82);
      badge.textContent = yearLabel(S.hoverYear);
      // 富信息 tooltip：hover 到事件点 / 政权条 / 人物条 / 年号条时显示
      const t = ev.target.closest('[data-tip-title]');
      if (t) showTip(t, ev); else hideTip();
    };
    shell.onmouseleave = () => {
      line.classList.remove('on');
      hideTip();
      S.hoverYear = null;
    };

    // 时间轴点击（bar / 事件点 / 空白）
    board.addEventListener('click', (ev) => {
      const bar = ev.target.closest('.bar[data-eid]');
      if (bar) {
        const [kind, id] = bar.getAttribute('data-eid').split(':');
        const y = yearAt(ev.clientX);
        if (y != null) setYear(y);
        if (kind === 'polity') openPolityDetailById(id);
        else if (kind === 'ruler') openRulerDetailById(id);
        else if (kind === 'reign') openReignDetailById(id);
        return;
      }
      const dot = ev.target.closest('.dot[data-eid]');
      if (dot) {
        const id = dot.getAttribute('data-eid').slice('event:'.length);
        const e = (S.cache.events || []).find((x) => x.eventId === id);
        if (e) {
          S.pendingDetail = { kind: 'event', eventId: e.eventId, data: e };
          setYear(e.year, { pan: true });
        }
        return;
      }
      const t = ev.target;
      const blank = t === board || t.classList.contains('board-inner')
        || t.classList.contains('ticks') || t.classList.contains('tick')
        || t.classList.contains('scale') || (t.tagName === 'SPAN' && t.parentElement
          && t.parentElement.classList.contains('tick'));
      if (!blank) return;
      const y = yearAt(ev.clientX);
      if (y != null) setYear(y, { pan: true });
    });

    // 年包栏点击（跳转 / 钩子 / 返回 / 下钻）
    const ypBody = el('ypBody');
    ypBody.addEventListener('click', (ev) => {
      if (ev.target.closest('#ypBack')) { closeDetail(); return; }
      const jump = ev.target.closest('[data-jump]');
      if (jump) { setYear(Number(jump.getAttribute('data-jump')), { pan: true }); return; }
      const hook = ev.target.closest('[data-kind="hook"]');
      if (hook) {
        const y = Number(hook.getAttribute('data-year'));
        if (Number.isFinite(y)) setYear(y, { pan: true });
        return;
      }
      const rulerChip = ev.target.closest('[data-ruler]');
      if (rulerChip) { openRulerDetailById(rulerChip.getAttribute('data-ruler')); return; }
      const item = ev.target.closest('[data-kind]');
      if (!item) return;
      const kind = item.getAttribute('data-kind');
      const id = item.getAttribute('data-id');
      if (kind === 'event') openEventDetailById(id);
      else if (kind === 'figure') openFigureDetailById(id);
      else if (kind === 'ruler') openRulerDetailById(id);
      else if (kind === 'polity') openPolityDetailById(id);
      else if (kind === 'reign') openReignDetailById(id);
    });
  }

  async function boot() {
    try {
      const [metaR, erasR] = await Promise.all([
        QuizApi.apiFetch('/api/history/stream/meta'),
        QuizApi.apiFetch('/api/history/stream/eras'),
      ]);
      if (!metaR.ok || !erasR.ok) throw new Error('初始化失败');
      S.meta = await metaR.json();
      S.eras = await erasR.json();
      bindRangeLimits();
      const url = HistoryStream.data.readUrl();
      const def = S.eras.find((e) => e.id === S.meta.defaultEraId) || S.eras[0];
      if (url.radius) S.radius = url.radius;
      if (url.year != null) {
        S.year = clamp(url.year, S.meta.minYear, S.meta.maxYear);
        S.center = S.year;
      } else if (def) {
        S.activeEraId = def.id;
        S.year = def.fromYear;
        S.center = def.fromYear;
        if (!url.radius) S.radius = 100;
      }
      syncReignLayer();
      syncInputs();
      renderEras();
      bindBoardInteraction();
      HistoryStream.search.init(S);

      el('centerYear').addEventListener('change', (e) => onCenterChange(e.target.value));
      el('centerRange').addEventListener('change', (e) => onCenterChange(e.target.value));
      document.querySelectorAll('.radius').forEach((btn) => {
        btn.addEventListener('click', () => {
          S.radius = Number(btn.dataset.radius);
          S.center = S.year;
          S.reignManual = false;
          syncReignLayer();
          refresh();
        });
      });
      el('showFragment').addEventListener('change', (e) => {
        S.showFragment = e.target.checked;
        syncInputs();
        refresh();
      });
      el('layerReign').addEventListener('change', (e) => {
        S.reignManual = true;
        S.layers.reign = e.target.checked;
        HistoryStream.timeline.renderBoard(S);
        syncInputs();
      });
      [['kindBattle', 'battle'], ['kindPolitics', 'politics'], ['kindCulture', 'culture']].forEach(([id, kind]) => {
        el(id).addEventListener('change', (e) => {
          S.kinds[kind] = e.target.checked;
          HistoryStream.timeline.renderBoard(S);
        });
      });
      el('ypPrev').addEventListener('click', () => stepYear(-1));
      el('ypNext').addEventListener('click', () => stepYear(1));
      window.addEventListener('resize', placeYearLine);
      document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
          if (S.detail) { closeDetail(); e.preventDefault(); }
          return;
        }
        const typing = e.target && (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA');
        if (typing) return;
        if (e.key === 'ArrowLeft') { e.preventDefault(); e.shiftKey ? jumpRecorded(-1) : stepYear(-1); }
        if (e.key === 'ArrowRight') { e.preventDefault(); e.shiftKey ? jumpRecorded(1) : stepYear(1); }
      });

      await refresh();
    } catch (e) {
      setError(String(e.message || e));
    }
  }

  // —— 状态（供 yearpack/timeline 共享），缓存复用 data 层对象 ——
  const S = {
    meta: null,
    eras: [],
    year: -221,
    center: -221,
    radius: 100,
    showFragment: false,
    activeEraId: null,
    layers: { polity: true, event: true, ruler: true, reign: false },
    reignManual: false,
    cache: HistoryStream.data.cache,
    pack: null,
    detail: null,
    pendingDetail: null,
    hoverYear: null,
    kinds: { battle: true, politics: true, culture: true },
    openRulerDetail,
    openFigureDetail,
    setYear,
  };

  boot();

  return {
    openPolityDetail,
    openRulerDetail,
    openReignDetail,
    openEventDetail,
    openFigureDetail,
    closeDetail,
    setYear,
  };
})();
