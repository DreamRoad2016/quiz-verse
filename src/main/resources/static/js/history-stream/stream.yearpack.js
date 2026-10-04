/**
 * 历史源流 · 选中年信息包（右侧栏）渲染：三层信息架构。
 *   L1 .yp-main —— 本年大事 / 空年跳转（一眼核心）
 *   L2 .yp-l2   —— 台上政权 chips + 在位者/年号一句话
 *   L3 .yp-l3   —— <details> 折叠全部细节
 * 纯渲染，元素带 data-kind/data-id；点击由 app.js 的 ypBody 事件委托处理。
 */
window.HistoryStream = window.HistoryStream || {};
HistoryStream.yearpack = (function () {
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

  function kindLabel(kind) {
    if (kind === 'battle') return '战役';
    if (kind === 'culture') return '文化';
    return '时局';
  }

  function phaseLabel(phase) {
    if (phase === 'founding') return '始';
    if (phase === 'ending') return '终';
    return '';
  }

  /** spine 主线政权优先，用于 L2 一句话定位“谁在位”。 */
  function mainOf(pack) {
    const polities = pack.polities || [];
    const rulers = pack.rulers || [];
    const reigns = pack.reigns || [];
    const spineIds = new Set(polities.filter((p) => p.tier === 'spine').map((p) => p.id));
    const pick = (list) => list.find((x) => (spineIds.size ? spineIds.has(x.polityId) : true)) || list[0] || null;
    return {
      polity: polities.find((p) => p.tier === 'spine') || polities[0] || null,
      ruler: pick(rulers),
      reign: pick(reigns),
    };
  }

  function render(S) {
    const pack = S.pack;
    el('ypYear').textContent = pack ? pack.yearLabel : yearLabel(S.year);
    el('ypHeadline').textContent = pack ? pack.headline : '';
    el('ypPrev').disabled = !S.meta || S.year <= S.meta.minYear;
    el('ypNext').disabled = !S.meta || S.year >= S.meta.maxYear;
    const body = el('ypBody');
    if (S.detail) {
      renderPackDetail(S, body);
      return;
    }
    if (!pack) {
      body.innerHTML = '<div class="empty-inline">加载中…</div>';
      return;
    }
    body.innerHTML = renderL1(S, pack) + renderL2(S, pack) + renderL3(S, pack);
  }

  // —— L1：本年大事 / 空年跳转 ——
  function renderL1(S, pack) {
    const events = pack.events || [];
    let html = '<div class="yp-main">';
    if (!events.length) {
      html += '<div class="yp-empty-year">本年无大事记载</div>';
      // 附近事件前置：空年不跳转也能一眼看清“这附近发生了什么”（时间正序）
      const before = (pack.beforeHooks || []).slice(0, 2).reverse();
      const after = (pack.afterHooks || []).slice(0, 2);
      if (before.length || after.length) {
        html += '<div class="yp-nearby">';
        html += '<div class="yp-l2-label">附近</div>';
        const row = (h, dir) => {
          const ago = dir === 'before' ? ('前' + (-h.delta) + '年') : ('后' + h.delta + '年');
          return '<div class="yp-item" data-kind="hook" data-year="' + h.year + '"><strong>'
            + esc(h.title) + '</strong><div class="sub">' + ago + ' · ' + yearLabel(h.year) + '</div></div>';
        };
        before.forEach((h) => { html += row(h, 'before'); });
        after.forEach((h) => { html += row(h, 'after'); });
        html += '</div>';
      }
      html += '<div class="yp-cta">';
      if (pack.prevRecorded) {
        html += '<button type="button" data-jump="' + pack.prevRecorded.year + '">'
          + '跳到最近有事的一年（前）'
          + '<small>' + yearLabel(pack.prevRecorded.year) + ' · ' + esc(pack.prevRecorded.title) + '</small></button>';
      }
      if (pack.nextRecorded) {
        html += '<button type="button" data-jump="' + pack.nextRecorded.year + '">'
          + '跳到最近有事的一年（后）'
          + '<small>' + yearLabel(pack.nextRecorded.year) + ' · ' + esc(pack.nextRecorded.title) + '</small></button>';
      }
      html += '</div>';
    } else {
      html += '<div class="yp-l2-label" style="margin-bottom:6px">本年大事</div>';
      events.forEach((e) => {
        html += '<div class="yp-item" data-kind="event" data-id="' + esc(e.eventId) + '"><strong>'
          + '<span class="tag">' + kindLabel(e.kind) + '</span>' + esc(e.title) + '</strong>'
          + '<div class="sub">' + esc(e.summary || '')
          + ((e.idioms && e.idioms.length) ? ' · ' + e.idioms.map(esc).join('、') : '')
          + '</div></div>';
      });
    }
    html += '</div>';
    return html;
  }

  // —— L2：台上政权 chips + 在位者/年号一句话 ——
  function renderL2(S, pack) {
    const polities = pack.polities || [];
    const main = mainOf(pack);
    let html = '<div class="yp-l2">';
    html += '<div class="yp-l2-row"><span class="yp-l2-label">台上</span>';
    if (!polities.length) {
      html += '<span class="yp-l2-muted">本年无政权条</span>';
    } else {
      polities.forEach((p) => {
        const ph = phaseLabel(p.phase);
        html += '<span class="polity-chip" data-kind="polity" data-id="' + esc(p.id) + '">'
          + (ph ? '<span class="ph">' + ph + '</span>' : '')
          + esc(p.name) + '</span>';
      });
    }
    html += '</div>';

    const rulerLine = main.ruler
      ? esc(main.ruler.name) + (main.ruler.personalName ? ' · ' + esc(main.ruler.personalName) : '')
        + ' 在位第' + main.ruler.yearOfReign + '年'
      : '';
    const reignLine = main.reign
      ? esc(main.reign.name) + (main.reign.yearOfEra === 1 ? '元年' : main.reign.yearOfEra + '年')
      : '';
    html += '<div class="yp-l2-row">';
    if (rulerLine || reignLine) {
      html += '<span class="yp-l2-label">在位</span>'
        + '<span>' + (rulerLine ? rulerLine + (reignLine ? ' · ' + reignLine : '') : reignLine) + '</span>';
    } else {
      html += '<span class="yp-l2-muted">本年无在位者记录</span>';
    }
    html += '</div></div>';
    return html;
  }

  // —— L3：折叠全部细节 ——
  function renderL3(S, pack) {
    let html = '<div class="yp-l3">';

    // 在位者
    const rulers = pack.rulers || [];
    html += '<details' + (rulers.length ? ' open' : '') + '><summary>在位者 · ' + rulers.length + '</summary>';
    if (!rulers.length) {
      html += '<div class="empty-inline">本年无轴上人物</div>';
    } else {
      rulers.forEach((r) => {
        html += '<div class="yp-item" data-kind="ruler" data-id="' + esc(r.id) + '"><strong>'
          + esc(r.name) + (r.personalName ? ' · ' + esc(r.personalName) : '') + '</strong>'
          + '<div class="sub">在位第' + r.yearOfReign + '年' + (r.uncertain ? ' · 约年' : '') + '</div></div>';
      });
    }
    html += '</details>';

    // 年号
    const reigns = pack.reigns || [];
    if (reigns.length) {
      html += '<details><summary>年号 · ' + reigns.length + '</summary>';
      reigns.forEach((r) => {
        html += '<div class="yp-item" data-kind="reign" data-id="' + esc(r.id) + '"><strong>'
          + esc(r.name) + (r.yearOfEra === 1 ? '元年' : r.yearOfEra + '年') + '</strong>'
          + '<div class="sub">' + yearLabel(r.fromYear) + '–' + yearLabel(r.toYear) + '</div></div>';
      });
      html += '</details>';
    }

    // 本年人物
    const figures = pack.figures || [];
    if (figures.length) {
      html += '<details><summary>本年人物 · ' + figures.length + '</summary>';
      figures.forEach((f) => {
        html += '<div class="yp-item" data-kind="figure" data-id="' + esc(f.id) + '"><strong>'
          + '<span class="tag">' + esc(f.roleLabel || '') + '</span>' + esc(f.name)
          + (f.personalName ? ' · ' + esc(f.personalName) : '') + '</strong>'
          + '<div class="sub">' + (f.uncertain ? '约 ' : '') + yearLabel(f.fromYear)
          + '–' + yearLabel(f.toYear) + '</div></div>';
      });
      html += '</details>';
    }

    // 出典（成语典故）
    const allusions = [];
    (pack.events || []).forEach((e) => {
      (e.idioms || []).forEach((t) => {
        allusions.push({ text: t, eventId: e.eventId, title: e.title });
      });
    });
    if (allusions.length) {
      html += '<details><summary>出典 · ' + allusions.length + '</summary>';
      allusions.forEach((a) => {
        html += '<div class="yp-item" data-kind="event" data-id="' + esc(a.eventId) + '"><strong>'
          + esc(a.text) + '</strong><div class="sub">出于「' + esc(a.title) + '」</div></div>';
      });
      html += '</details>';
    }

    // 邻近事件钩子
    html += renderHookDetails('此前不久', pack.beforeHooks);
    html += renderHookDetails('此后不久', pack.afterHooks);

    // 谱系
    (pack.lineages || []).forEach((block) => {
      html += '<details><summary>谱系 · ' + esc(block.polityName) + '（传统世系）</summary><ul class="lineage-list">';
      (block.persons || []).forEach((x) => {
        html += '<li>' + esc(x.name)
          + (x.personalName ? '<small> · ' + esc(x.personalName) + '</small>' : '') + '</li>';
      });
      html += '</ul></details>';
    });

    // 标记
    if (pack.markers && pack.markers.length) {
      html += '<details><summary>标记 · ' + pack.markers.length + '</summary>';
      pack.markers.forEach((m) => {
        html += '<div class="yp-item"><strong>' + esc(m.name) + '</strong></div>';
      });
      html += '</details>';
    }

    html += '</div>';
    return html;
  }

  function renderHookDetails(title, hooks) {
    if (!hooks || !hooks.length) return '';
    let html = '<details><summary>' + title + ' · ' + hooks.length + '</summary>';
    hooks.forEach((h) => {
      const ago = h.delta < 0 ? ((-h.delta) + '年前') : (h.delta + '年后');
      html += '<div class="yp-item" data-kind="hook" data-year="' + h.year + '"><strong>'
        + esc(h.title) + '</strong><div class="sub">' + yearLabel(h.year) + ' · ' + ago + '</div></div>';
    });
    html += '</details>';
    return html;
  }

  // —— 详情页 ——
  function renderPackDetail(S, body) {
    const d = S.detail;
    const back = '<button type="button" class="yp-back" id="ypBack">← 回到 '
      + yearLabel(S.year) + '</button>';
    body.innerHTML = back + '<div class="yp-detail" id="ypDetailInner"><div class="empty-inline">加载中…</div></div>';
    const inner = el('ypDetailInner');
    if (d.kind === 'event') inner.innerHTML = eventDetailHtml(d.data);
    else if (d.kind === 'figure') inner.innerHTML = figureDetailHtml(d.data);
    else if (d.kind === 'ruler') fillRulerDetail(inner, d.data);
    else if (d.kind === 'polity') fillPolityDetail(inner, d.data, S);
    else if (d.kind === 'reign') inner.innerHTML = reignDetailHtml(d.data);
  }

  function eventDetailHtml(e) {
    let html = '<h2>' + esc(e.title) + '</h2>'
      + '<div class="meta">' + yearLabel(e.year)
      + (e.endYear ? '–' + yearLabel(e.endYear) : '')
      + ' · ' + kindLabel(e.kind) + '</div>'
      + '<div class="note">' + esc(e.description || e.summary || '') + '</div>';
    if (e.idioms && e.idioms.length) {
      html += '<div class="meta">出典</div><div class="chip-row">';
      e.idioms.forEach((t) => { html += '<span class="chip">' + esc(t) + '</span>'; });
      html += '</div>';
    }
    return html;
  }

  function figureDetailHtml(f) {
    return '<h2>' + esc(f.name)
      + (f.personalName ? '（' + esc(f.personalName) + '）' : '') + '</h2>'
      + '<div class="meta">' + esc(f.roleLabel || '')
      + ' · ' + (f.uncertain ? '约 ' : '') + yearLabel(f.fromYear)
      + '–' + (f.uncertain ? '约 ' : '') + yearLabel(f.toYear) + '</div>'
      + (f.note ? '<div class="note">' + esc(f.note) + '</div>' : '');
  }

  function reignDetailHtml(r) {
    return '<h2>年号 · ' + esc(r.name) + '</h2>'
      + '<div class="meta">' + yearLabel(r.fromYear) + '–' + yearLabel(r.toYear) + '</div>'
      + (r.note ? '<div class="note">' + esc(r.note) + '</div>' : '');
  }

  async function fillRulerDetail(inner, r) {
    let reigns = [];
    try {
      const nr = await QuizApi.apiFetch(
        '/api/history/stream/reigns?from=' + r.fromYear + '&to=' + r.toYear
        + '&rulerId=' + encodeURIComponent(r.id));
      if (nr.ok) reigns = await nr.json();
    } catch (_) { /* ignore */ }
    let html = '<h2>' + esc(r.name) + (r.personalName ? '（' + esc(r.personalName) + '）' : '') + '</h2>'
      + '<div class="meta">' + (r.uncertain ? '约 ' : '') + yearLabel(r.fromYear)
      + '–' + (r.uncertain ? '约 ' : '') + yearLabel(r.toYear)
      + (r.uncertain ? ' · 年代约估' : '')
      + (r.yearOfReign ? ' · 本年为在位第' + r.yearOfReign + '年' : '')
      + '</div>'
      + (r.note ? '<div class="note">' + esc(r.note) + '</div>' : '');
    if (reigns.length) {
      html += '<div class="meta">在位年号</div><div class="chip-row">';
      reigns.forEach((x) => {
        html += '<span class="chip reign">' + esc(x.name) + ' · '
          + yearLabel(x.fromYear) + '–' + yearLabel(x.toYear) + '</span>';
      });
      html += '</div>';
    } else {
      html += '<div class="empty-inline">无年号细分，或尚未录入。</div>';
    }
    inner.innerHTML = html;
  }

  async function fillPolityDetail(inner, p, S) {
    const q = 'from=' + p.fromYear + '&to=' + p.toYear + '&polityId=' + encodeURIComponent(p.id);
    let rulers = [];
    let reigns = [];
    let lineages = [];
    try {
      const [rr, nr, lr] = await Promise.all([
        QuizApi.apiFetch('/api/history/stream/rulers?' + q),
        QuizApi.apiFetch('/api/history/stream/reigns?' + q),
        QuizApi.apiFetch('/api/history/stream/lineages?polityId=' + encodeURIComponent(p.id)),
      ]);
      if (rr.ok) rulers = await rr.json();
      if (nr.ok) reigns = await nr.json();
      if (lr.ok) lineages = await lr.json();
    } catch (_) { /* ignore */ }

    let html = '<h2>' + esc(p.name) + '</h2>'
      + '<div class="meta">' + (p.uncertain ? '约 ' : '') + yearLabel(p.fromYear)
      + '–' + (p.uncertain ? '约 ' : '') + yearLabel(p.toYear)
      + (p.capital ? ' · 都城 ' + esc(p.capital) : '')
      + (p.uncertain ? ' · 年代约估' : '') + '</div>'
      + (p.note ? '<div class="note">' + esc(p.note) + '</div>' : '');
    if (lineages.length) {
      html += '<div class="meta">传统世系</div><ul class="lineage-list">';
      lineages.forEach((x) => {
        html += '<li>' + esc(x.name)
          + (x.personalName ? '<small> · ' + esc(x.personalName) + '</small>' : '') + '</li>';
      });
      html += '</ul>';
    }
    if (rulers.length) {
      html += '<div class="meta">王 / 执政</div><div class="chip-row">';
      rulers.forEach((r) => {
        html += '<span class="chip" data-ruler="' + esc(r.id) + '">' + esc(r.name)
          + (r.personalName ? '·' + esc(r.personalName) : '') + '</span>';
      });
      html += '</div>';
    }
    if (reigns.length) {
      html += '<div class="meta">年号</div><div class="chip-row">';
      reigns.slice(0, 24).forEach((r) => {
        html += '<span class="chip reign">' + esc(r.name) + '</span>';
      });
      html += '</div>';
    }
    inner.innerHTML = html;
    // 政权详情里的“王 / 执政”chip 可下钻到该人物详情（用本次 fetch 的局部 rulers）
    inner.querySelectorAll('[data-ruler]').forEach((node) => {
      node.addEventListener('click', () => {
        const r = rulers.find((x) => x.id === node.getAttribute('data-ruler'));
        if (r) S.openRulerDetail(r);
      });
    });
  }

  return { render };
})();
