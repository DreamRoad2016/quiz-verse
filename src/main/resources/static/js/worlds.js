/**
 * 公开站内容目录：从 GET /api/catalog 加载（与小程序同一接口）。
 * 失败时回退到内置默认。
 */
window.CCYZ_WORLDS = (function () {
  const DEFAULT_PACK_ID = 'zhenhuan_2011';
  const DEFAULT_HOT = {
    id: 'hot',
    name: '热点',
    hint: '正在开放的人物辑',
    worldIds: ['zhenhuan', 'shuihu', 'genshin']
  };
  const DEFAULT_CATEGORIES = [
    {
      id: 'drama',
      name: '影视剧',
      hint: '剧集人物辑录',
      worlds: [
        { id: 'zhenhuan', packId: 'zhenhuan_2011', title: '甄嬛传', subtitle: '清宫人物资料辑', badge: '可查阅', ready: true, status: 'ready' },
        { id: 'daming', title: '大明王朝1566', subtitle: '整理中', badge: '整理中', ready: false, status: 'soon' },
        { id: 'langya', title: '琅琊榜', subtitle: '整理中', badge: '整理中', ready: false, status: 'soon' },
        { id: 'qingyunian', title: '庆余年', subtitle: '整理中', badge: '整理中', ready: false, status: 'soon' }
      ]
    },
    {
      id: 'novel',
      name: '小说',
      hint: '文学作品人物辑录',
      worlds: [
        { id: 'honglou', title: '红楼梦', subtitle: '四大名著', badge: '整理中', ready: false, status: 'soon' },
        { id: 'xiyou', title: '西游记', subtitle: '四大名著', badge: '整理中', ready: false, status: 'soon' },
        { id: 'shuihu', packId: 'shuihu_120', title: '水浒传', subtitle: '120 回人物资料辑', badge: '可查阅', ready: true, status: 'ready' },
        { id: 'sanguo', title: '三国演义', subtitle: '四大名著', badge: '整理中', ready: false, status: 'soon' },
        { id: 'santi', title: '三体', subtitle: '科幻作品', badge: '整理中', ready: false, status: 'soon' }
      ]
    },
    {
      id: 'history',
      name: '历史人物',
      hint: '按朝代整理',
      worlds: [
        { id: 'xianqin', title: '先秦', subtitle: '诸子 · 列国', badge: '整理中', ready: false, status: 'soon' },
        { id: 'qinhan', title: '秦汉', subtitle: '一统与开疆', badge: '整理中', ready: false, status: 'soon' },
        { id: 'weijin', title: '魏晋南北朝', subtitle: '乱世风流', badge: '整理中', ready: false, status: 'soon' },
        { id: 'suitang', title: '隋唐', subtitle: '盛世气象', badge: '整理中', ready: false, status: 'soon' },
        { id: 'songyuan', title: '宋元', subtitle: '文治武功', badge: '整理中', ready: false, status: 'soon' },
        { id: 'mingqing', title: '明清', subtitle: '帝制时期', badge: '整理中', ready: false, status: 'soon' }
      ]
    },
    {
      id: 'games',
      name: '游戏',
      hint: '游戏角色辑录',
      tone: 'cool',
      worlds: [
        { id: 'genshin', packId: 'genshin_impact', title: '原神', subtitle: '提瓦特人物资料辑', badge: '可查阅', ready: true, status: 'ready', tone: 'cool' },
        { id: 'hsr', title: '崩坏：星穹铁道', subtitle: '整理中', badge: '整理中', ready: false, status: 'soon', tone: 'cool' },
        { id: 'wzry', title: '王者荣耀', subtitle: '整理中', badge: '整理中', ready: false, status: 'soon', tone: 'cool' },
        { id: 'lol', title: '英雄联盟', subtitle: '整理中', badge: '整理中', ready: false, status: 'soon', tone: 'cool' }
      ]
    }
  ];

  let HOT = Object.assign({}, DEFAULT_HOT);
  let CATEGORIES = DEFAULT_CATEGORIES.map(cloneCat);
  let HOT_WORLDS = [];
  let loaded = false;
  let loadPromise = null;

  function cloneWorld(w) {
    const ready = !!w.ready;
    return {
      id: w.id,
      packId: w.packId || '',
      title: w.title,
      subtitle: w.subtitle || '',
      description: w.description || '',
      tone: w.tone || '',
      badge: w.badge || (ready ? '可查阅' : '整理中'),
      ready,
      status: w.status || (ready ? 'ready' : 'soon')
    };
  }

  function cloneCat(c) {
    return {
      id: c.id,
      name: c.name,
      hint: c.hint || '',
      tone: c.tone || '',
      worlds: (c.worlds || []).map(cloneWorld)
    };
  }

  function rebuildHot(worldIds) {
    const ids = Array.isArray(worldIds) ? worldIds : [];
    const byId = {};
    CATEGORIES.forEach((cat) => {
      (cat.worlds || []).forEach((w) => {
        if (w.id) byId[w.id] = Object.assign({ categoryName: cat.name }, w);
      });
    });
    HOT_WORLDS = [];
    ids.forEach((id) => {
      if (HOT_WORLDS.length >= 4) return;
      const w = byId[id];
      if (w && w.ready && w.packId) HOT_WORLDS.push(w);
    });
  }

  rebuildHot(DEFAULT_HOT.worldIds);

  function applyCatalog(data) {
    if (!data || !Array.isArray(data.categories) || !data.categories.length) return false;
    if (data.hot) {
      HOT = {
        id: 'hot',
        name: data.hot.name || DEFAULT_HOT.name,
        hint: data.hot.hint || DEFAULT_HOT.hint,
        worldIds: Array.isArray(data.hot.worldIds) ? data.hot.worldIds.slice() : []
      };
    }
    CATEGORIES = data.categories.map(cloneCat).filter((c) => c.id !== 'more' || c.worlds.some((w) => w.ready));
    if (Array.isArray(data.hotWorlds)) {
      HOT_WORLDS = data.hotWorlds.map(cloneWorld).filter((w) => w.ready && w.packId).slice(0, 4);
    } else {
      rebuildHot(HOT.worldIds);
    }
    return true;
  }

  function findWorld(id) {
    for (const cat of CATEGORIES) {
      const hit = cat.worlds.find((w) => w.id === id);
      if (hit) return Object.assign({ categoryName: cat.name }, hit);
    }
    return HOT_WORLDS.find((w) => w.id === id) || null;
  }

  function findWorldByPackId(packId) {
    for (const cat of CATEGORIES) {
      const hit = cat.worlds.find((w) => w.packId === packId);
      if (hit) return Object.assign({ categoryName: cat.name }, hit);
    }
    return HOT_WORLDS.find((w) => w.packId === packId) || null;
  }

  async function ensureLoaded() {
    if (loaded) return true;
    if (loadPromise) return loadPromise;
    loadPromise = (async () => {
      try {
        if (!window.QuizApi || !window.QuizApi.apiFetch) {
          throw new Error('QuizApi missing');
        }
        const r = await window.QuizApi.apiFetch('/api/catalog');
        if (!r.ok) throw new Error('catalog HTTP ' + r.status);
        const data = await r.json();
        if (!applyCatalog(data)) {
          CATEGORIES = DEFAULT_CATEGORIES.map(cloneCat);
          HOT = Object.assign({}, DEFAULT_HOT);
          rebuildHot(DEFAULT_HOT.worldIds);
        }
      } catch (e) {
        console.warn('catalog fallback', e);
        CATEGORIES = DEFAULT_CATEGORIES.map(cloneCat);
        HOT = Object.assign({}, DEFAULT_HOT);
        rebuildHot(DEFAULT_HOT.worldIds);
      }
      loaded = true;
      return true;
    })();
    return loadPromise;
  }

  return {
    DEFAULT_PACK_ID,
    ensureLoaded,
    get HOT() { return HOT; },
    get CATEGORIES() { return CATEGORIES; },
    get HOT_WORLDS() { return HOT_WORLDS; },
    findWorld,
    findWorldByPackId
  };
})();
