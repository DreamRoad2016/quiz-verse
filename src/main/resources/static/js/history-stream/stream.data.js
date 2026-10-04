/**
 * 历史源流 · 数据层：加载 meta/eras、区间缓存、选中年包（含相邻年预取）。
 * 依赖 api.js 的全局 QuizApi.apiFetch。
 */
window.HistoryStream = window.HistoryStream || {};
HistoryStream.data = (function () {
  const PACK_CACHE_MAX = 6;
  const RADIUS_PRESETS = [10, 50, 100, 200, 500];

  const cache = { polities: [], events: [], rulers: [], reigns: [], markers: [] };
  const packCache = new Map(); // year -> StreamYearPack

  async function loadMeta() {
    const r = await QuizApi.apiFetch('/api/history/stream/meta');
    if (!r.ok) throw new Error('meta 加载失败');
    return r.json();
  }

  async function loadEras() {
    const r = await QuizApi.apiFetch('/api/history/stream/eras');
    if (!r.ok) throw new Error('eras 加载失败');
    return r.json();
  }

  /** 拉取区间内所有泳道数据，写入 cache。 */
  async function loadRange(from, to, tiers) {
    const q = 'from=' + from + '&to=' + to;
    const [pr, mr, er, rr, nr] = await Promise.all([
      QuizApi.apiFetch('/api/history/stream/polities?' + q + '&tiers=' + encodeURIComponent(tiers)),
      QuizApi.apiFetch('/api/history/stream/markers?' + q),
      QuizApi.apiFetch('/api/history/stream/events?' + q),
      QuizApi.apiFetch('/api/history/stream/rulers?' + q),
      QuizApi.apiFetch('/api/history/stream/reigns?' + q),
    ]);
    if (!pr.ok || !mr.ok || !er.ok || !rr.ok || !nr.ok) throw new Error('区间数据加载失败');
    cache.polities = await pr.json();
    cache.markers = await mr.json();
    const ev = await er.json();
    cache.events = ev.items || [];
    cache.rulers = await rr.json();
    cache.reigns = await nr.json();
  }

  /** 选中年信息包；命中缓存直接返回。 */
  async function loadYearPack(year) {
    const hit = packCache.get(year);
    if (hit) return hit;
    const r = await QuizApi.apiFetch('/api/history/stream/year?y=' + year);
    if (!r.ok) throw new Error('本年信息加载失败');
    const pack = await r.json();
    putPack(year, pack);
    return pack;
  }

  function putPack(year, pack) {
    packCache.set(year, pack);
    if (packCache.size > PACK_CACHE_MAX) {
      packCache.delete(packCache.keys().next().value);
    }
  }

  /** 后台预取相邻年，命中翻年时即时渲染。 */
  function prefetchNeighbors(year) {
    [year - 1, year + 1].forEach((y) => {
      if (packCache.has(y)) return;
      QuizApi.apiFetch('/api/history/stream/year?y=' + y)
        .then((r) => (r.ok ? r.json() : null))
        .then((p) => { if (p) putPack(y, p); })
        .catch(() => {});
    });
  }

  function readUrl() {
    const u = new URL(location.href);
    const ys = u.searchParams.get('year');
    const rs = u.searchParams.get('radius');
    const y = ys == null ? NaN : Number(ys);
    const r = rs == null ? NaN : Number(rs);
    return {
      year: Number.isFinite(y) ? y : null,
      radius: Number.isFinite(r) && RADIUS_PRESETS.includes(r) ? r : null,
    };
  }

  function writeUrl(state) {
    const u = new URL(location.href);
    u.searchParams.set('year', String(state.year));
    u.searchParams.set('radius', String(state.radius));
    history.replaceState({ year: state.year, radius: state.radius }, '', u);
  }

  return {
    cache,
    loadMeta,
    loadEras,
    loadRange,
    loadYearPack,
    prefetchNeighbors,
    readUrl,
    writeUrl,
  };
})();
