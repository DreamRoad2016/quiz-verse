/** 公开站内容目录（人物资料收藏；仅 ready 可浏览） */
window.CCYZ_WORLDS = (function () {
  const DEFAULT_PACK_ID = 'zhenhuan_2011';
  const CATEGORIES = [
    {
      id: 'drama',
      name: '影视剧',
      hint: '剧集人物辑录',
      worlds: [
        {
          id: 'zhenhuan',
          packId: 'zhenhuan_2011',
          title: '甄嬛传',
          subtitle: '清宫人物资料辑',
          badge: '可查阅',
          ready: true
        },
        { id: 'daming', title: '大明王朝1566', subtitle: '整理中', badge: '整理中', ready: false },
        { id: 'langya', title: '琅琊榜', subtitle: '整理中', badge: '整理中', ready: false },
        { id: 'qingyunian', title: '庆余年', subtitle: '整理中', badge: '整理中', ready: false }
      ]
    },
    {
      id: 'novel',
      name: '小说',
      hint: '文学作品人物辑录',
      worlds: [
        { id: 'honglou', title: '红楼梦', subtitle: '四大名著', badge: '整理中', ready: false },
        { id: 'xiyou', title: '西游记', subtitle: '四大名著', badge: '整理中', ready: false },
        {
          id: 'shuihu',
          packId: 'shuihu_120',
          title: '水浒传',
          subtitle: '120 回人物资料辑',
          badge: '可查阅',
          ready: true
        },
        { id: 'sanguo', title: '三国演义', subtitle: '四大名著', badge: '整理中', ready: false },
        { id: 'santi', title: '三体', subtitle: '科幻作品', badge: '整理中', ready: false }
      ]
    },
    {
      id: 'history',
      name: '历史人物',
      hint: '按朝代整理',
      worlds: [
        { id: 'xianqin', title: '先秦', subtitle: '诸子 · 列国', badge: '整理中', ready: false },
        { id: 'qinhan', title: '秦汉', subtitle: '一统与开疆', badge: '整理中', ready: false },
        { id: 'weijin', title: '魏晋南北朝', subtitle: '乱世风流', badge: '整理中', ready: false },
        { id: 'suitang', title: '隋唐', subtitle: '盛世气象', badge: '整理中', ready: false },
        { id: 'songyuan', title: '宋元', subtitle: '文治武功', badge: '整理中', ready: false },
        { id: 'mingqing', title: '明清', subtitle: '帝制时期', badge: '整理中', ready: false }
      ]
    }
  ];

  function findWorld(id) {
    for (const cat of CATEGORIES) {
      const hit = cat.worlds.find((w) => w.id === id);
      if (hit) return Object.assign({ categoryName: cat.name }, hit);
    }
    return null;
  }

  function findWorldByPackId(packId) {
    for (const cat of CATEGORIES) {
      const hit = cat.worlds.find((w) => w.packId === packId);
      if (hit) return Object.assign({ categoryName: cat.name }, hit);
    }
    return null;
  }

  return { DEFAULT_PACK_ID, CATEGORIES, findWorld, findWorldByPackId };
})();
