/**
 * 历史源流 · 查人：顶部搜索框，按 姓名/字/庙号 匹配统治者与重要人物，
 * 选中后定位到人物活跃年代并打开详情（他所处时代 · 谁在位 · 发生了什么）。
 * 依赖 api.js；由 app.js 在状态 S 就绪后调用 init(S)。
 */
window.HistoryStream = window.HistoryStream || {};
HistoryStream.search = (function () {
  let timer = null;
  let lastQ = '';

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

  function init(S) {
    const input = document.getElementById('personSearch');
    const box = document.getElementById('personResults');
    if (!input || !box) return;

    function query(q) {
      return QuizApi.apiFetch('/api/history/stream/search?q=' + encodeURIComponent(q) + '&limit=12')
        .then((r) => (r.ok ? r.json() : []))
        .catch(() => []);
    }

    function render(items) {
      box.innerHTML = '';
      if (!items || !items.length) {
        box.hidden = true;
        return;
      }
      items.forEach((it) => {
        const node = document.createElement('div');
        node.className = 'person-hit';
        node.innerHTML =
          '<div class="ph-top"><strong>' + esc(it.name)
          + (it.personalName ? '（' + esc(it.personalName) + '）' : '') + '</strong>'
          + '<span class="ph-tag">' + esc(it.roleLabel || '') + '</span></div>'
          + '<div class="ph-sub">' + esc(it.polityName || '')
          + ' · ' + (it.uncertain ? '约 ' : '') + yearLabel(it.fromYear)
          + '–' + (it.uncertain ? '约 ' : '') + yearLabel(it.toYear) + '</div>';
        node.addEventListener('mousedown', (ev) => { ev.preventDefault(); pick(it); });
        box.appendChild(node);
      });
      box.hidden = false;
    }

    function pick(it) {
      box.hidden = true;
      lastQ = '';
      input.value = it.personalName || it.name;
      input.blur();
      // 先切到该人活跃起点年（时间轴 + 右侧栏同步），再打开详情下钻
      S.setYear(it.fromYear, { pan: true });
      if (it.kind === 'ruler') S.openRulerDetail(it);
      else S.openFigureDetail(it);
    }

    input.addEventListener('input', () => {
      const q = input.value.trim();
      clearTimeout(timer);
      if (!q) { box.hidden = true; lastQ = ''; return; }
      if (q === lastQ) return;
      timer = setTimeout(() => {
        lastQ = q;
        query(q).then(render);
      }, 160);
    });
    input.addEventListener('focus', () => {
      const q = input.value.trim();
      if (q && box.children.length) box.hidden = false;
    });
    input.addEventListener('blur', () => {
      setTimeout(() => { box.hidden = true; }, 180);
    });
    input.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') { box.hidden = true; input.blur(); }
    });
  }

  return { init };
})();
