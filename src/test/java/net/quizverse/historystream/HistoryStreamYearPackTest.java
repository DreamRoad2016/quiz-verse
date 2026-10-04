package net.quizverse.historystream;

import net.quizverse.historystream.model.StreamYearPack;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class HistoryStreamYearPackTest {

    @Autowired
    HistoryStreamRegistry registry;

    @Test
    void jiajingFirstYearHasMingReignAndRuler() {
        StreamYearPack pack = registry.yearPack(1522);
        assertEquals(1522, pack.getYear());
        assertTrue(pack.getPolities().stream().anyMatch(p -> "ming".equals(p.getId())));
        assertTrue(pack.getReigns().stream().anyMatch(r -> "嘉靖".equals(r.getName())));
        assertTrue(pack.getRulers().stream().anyMatch(r -> "ming_shizong".equals(r.getId())));
        assertEquals(1, pack.getReigns().stream().filter(r -> "嘉靖".equals(r.getName())).findFirst().orElseThrow().getYearOfEra());
    }

    @Test
    void sparseYearStillHasPolityAndNearestEvents() {
        StreamYearPack pack = registry.yearPack(100);
        assertFalse(pack.getPolities().isEmpty());
        if (pack.isEmptyEvents()) {
            assertTrue(pack.getPrevRecordedYear() != null || pack.getNextRecordedYear() != null);
            assertTrue(pack.getHeadline().contains("本年无大事记载"));
        }
    }

    @Test
    void nearbyHooksAroundAnshi() {
        StreamYearPack pack = registry.yearPack(755);
        assertFalse(pack.getEvents().isEmpty());
        assertTrue(pack.getAfterHooks().stream().anyMatch(h -> h.getYear() == 756));
    }

    @Test
    void sparseJiajingYearPointsToNearestRecorded() {
        StreamYearPack pack = registry.yearPack(1525);
        assertTrue(pack.isEmptyEvents());
        assertNotNull(pack.getPrevRecorded());
        assertNotNull(pack.getNextRecorded());
        assertTrue(pack.getHeadline().contains("嘉靖"));
        assertEquals(1524, pack.getPrevRecordedYear());
        assertEquals(1542, pack.getNextRecordedYear());
    }

    @Test
    void eventKindsAreBattlePoliticsOrCulture() {
        registry.eventsInRange(null, null, null, null).forEach(e ->
                assertTrue(
                        List.of("battle", "politics", "culture").contains(e.getKind()),
                        () -> e.getEventId() + " kind=" + e.getKind()));
    }

    @Test
    void chuHanJuluHasIdiomAndZhangLiang() {
        StreamYearPack pack = registry.yearPack(-207);
        assertTrue(pack.getEvents().stream().anyMatch(e -> "HS-QIN-0207-JULU".equals(e.getEventId())));
        assertTrue(pack.getEvents().stream()
                .filter(e -> "HS-QIN-0207-JULU".equals(e.getEventId()))
                .findFirst()
                .orElseThrow()
                .getIdioms()
                .contains("破釜沉舟"));
        assertTrue(pack.getFigures().stream().anyMatch(f -> "fig_zhang_liang".equals(f.getId())));
    }

    @Test
    void weiZhengAndYueFeiAppearInActiveYears() {
        assertTrue(registry.yearPack(627).getFigures().stream().anyMatch(f -> "fig_wei_zheng".equals(f.getId())));
        StreamYearPack yancheng = registry.yearPack(1140);
        assertTrue(yancheng.getEvents().stream().anyMatch(e -> "HS-SS-1140-YANCHENG".equals(e.getEventId())));
        assertTrue(yancheng.getFigures().stream().anyMatch(f -> "fig_yue_fei".equals(f.getId())));
    }

    @Test
    void sameFactIsNotDuplicatedOnAxis() {
        long guandu = registry.eventsInRange(200, 200, null, null).stream()
                .filter(e -> e.getTitle().contains("官渡")).count();
        long chibi = registry.eventsInRange(208, 208, null, null).stream()
                .filter(e -> e.getTitle().contains("赤壁")).count();
        assertEquals(1, guandu);
        assertEquals(1, chibi);
    }

    @Test
    void sixteenKingdomsHasFoundingAndNorthernUnification() {
        StreamYearPack y304 = registry.yearPack(304);
        assertTrue(y304.getEvents().stream().anyMatch(e -> "HS-SK-0304-LIUYUAN".equals(e.getEventId())));
        assertTrue(y304.getRulers().stream().anyMatch(r -> "hz-liuyuan".equals(r.getId())));
        StreamYearPack y376 = registry.yearPack(376);
        assertTrue(y376.getEvents().stream().anyMatch(e -> "HS-SK-0376-NORTHUNI".equals(e.getEventId())));
        assertTrue(y376.getRulers().stream().anyMatch(r -> "qq-fujianjian".equals(r.getId())));
        assertTrue(registry.yearPack(360).getRulers().stream().anyMatch(r -> "qy-murongwei".equals(r.getId())));
        assertTrue(registry.yearPack(349).getRulers().stream().anyMatch(r -> "hzh-shihu".equals(r.getId())));
    }

    @Test
    void tangLateAndTenKingdomsNodes() {
        assertTrue(registry.yearPack(817).getEvents().stream().anyMatch(e -> "HS-TANG-0817-CAIZHOU".equals(e.getEventId())));
        assertTrue(registry.yearPack(880).getEvents().stream().anyMatch(e -> "HS-TANG-0880-CHANGAN".equals(e.getEventId())));
        StreamYearPack y975 = registry.yearPack(975);
        assertTrue(y975.getEvents().stream().anyMatch(e -> "HS-TK-0975-NTANGFALL".equals(e.getEventId())));
        assertTrue(y975.getRulers().stream().anyMatch(r -> "st-houzhu".equals(r.getId())));
        assertTrue(registry.yearPack(920).getRulers().stream().anyMatch(r -> "wu-yangpu".equals(r.getId())));
        assertTrue(registry.yearPack(971).getRulers().stream().anyMatch(r -> "sh-liuchang".equals(r.getId())));
        assertTrue(registry.yearPack(979).getEvents().stream().anyMatch(e -> "HS-TK-0979-NHANFALL".equals(e.getEventId())));
    }

    @Test
    void tangAndSongReignsCoverOrdinaryYears() {
        StreamYearPack kaiyuan = registry.yearPack(730);
        assertTrue(kaiyuan.getReigns().stream().anyMatch(r -> "开元".equals(r.getName())));
        StreamYearPack dazhong = registry.yearPack(850);
        assertTrue(dazhong.getReigns().stream().anyMatch(r -> "大中".equals(r.getName())));
        StreamYearPack chunxi = registry.yearPack(1180);
        assertTrue(chunxi.getReigns().stream().anyMatch(r -> "淳熙".equals(r.getName())));
        StreamYearPack xianchun = registry.yearPack(1270);
        assertTrue(xianchun.getReigns().stream().anyMatch(r -> "咸淳".equals(r.getName())));
        StreamYearPack ganlu = registry.yearPack(-50);
        assertTrue(ganlu.getReigns().stream().anyMatch(r -> "甘露".equals(r.getName())));
        StreamYearPack yongning = registry.yearPack(120);
        assertTrue(yongning.getReigns().stream().anyMatch(r -> "永宁".equals(r.getName())));
        StreamYearPack jiaping = registry.yearPack(250);
        assertTrue(jiaping.getReigns().stream().anyMatch(r -> "嘉平".equals(r.getName())));
        StreamYearPack dade = registry.yearPack(1300);
        assertTrue(dade.getReigns().stream().anyMatch(r -> "大德".equals(r.getName())));
        StreamYearPack yonghe = registry.yearPack(350);
        assertTrue(yonghe.getReigns().stream().anyMatch(r -> "永和".equals(r.getName())));
        StreamYearPack yuanjia = registry.yearPack(440);
        assertTrue(yuanjia.getReigns().stream().anyMatch(r -> "元嘉".equals(r.getName())));
        StreamYearPack taihe = registry.yearPack(490);
        assertTrue(taihe.getReigns().stream().anyMatch(r -> "太和".equals(r.getName())));
        StreamYearPack tonghe = registry.yearPack(1000);
        assertTrue(tonghe.getReigns().stream().anyMatch(r -> "统和".equals(r.getName())));
        StreamYearPack dading = registry.yearPack(1170);
        assertTrue(dading.getReigns().stream().anyMatch(r -> "大定".equals(r.getName())));
        StreamYearPack xiande = registry.yearPack(955);
        assertTrue(xiande.getReigns().stream().anyMatch(r -> "显德".equals(r.getName())));
        StreamYearPack tiansheng = registry.yearPack(1160);
        assertTrue(tiansheng.getReigns().stream().anyMatch(r -> "天盛".equals(r.getName())));
        StreamYearPack jianyuan = registry.yearPack(370);
        assertTrue(jianyuan.getReigns().stream().anyMatch(r -> "建元".equals(r.getName())));
        StreamYearPack baoda = registry.yearPack(950);
        assertTrue(baoda.getReigns().stream().anyMatch(r -> "保大".equals(r.getName())));
    }

    @Test
    void laterGapsHaveTextbookNodes() {
        assertTrue(registry.yearPack(986).getEvents().stream().anyMatch(e -> "HS-NS-0986-YONGXI".equals(e.getEventId())));
        assertTrue(registry.yearPack(1550).getEvents().stream().anyMatch(e -> "HS-MING-1550-GENGXU".equals(e.getEventId())));
        assertTrue(registry.yearPack(1759).getEvents().stream().anyMatch(e -> "HS-QING-1759-XINJIANG".equals(e.getEventId())));
        assertTrue(registry.yearPack(-81).getEvents().stream().anyMatch(e -> "HS-HAN-0081-YANTIE".equals(e.getEventId())));
        assertTrue(registry.yearPack(249).getEvents().stream().anyMatch(e -> "HS-WEI-0249-GAOPING".equals(e.getEventId())));
        assertTrue(registry.yearPack(1894).getEvents().stream().anyMatch(e -> "HS-QING-1894-JIAWU".equals(e.getEventId())));
        assertTrue(registry.yearPack(-154).getEvents().stream().anyMatch(e -> "HS-HAN-0154-QIGUO".equals(e.getEventId())));
        assertTrue(registry.yearPack(528).getEvents().stream().anyMatch(e -> "HS-ND-0528-HEYIN".equals(e.getEventId())));
        assertTrue(registry.yearPack(1919).getEvents().stream().anyMatch(e -> "HS-ROC-1919-WUSI".equals(e.getEventId())));
        assertTrue(registry.yearPack(-180).getEvents().stream().anyMatch(e -> "HS-HAN-0180-WENDI".equals(e.getEventId())));
        assertTrue(registry.yearPack(1041).getEvents().stream().anyMatch(e -> "HS-NS-1041-HAOSHUI".equals(e.getEventId())));
        assertTrue(registry.yearPack(1931).getEvents().stream().anyMatch(e -> "HS-ROC-1931-918".equals(e.getEventId())));
        assertTrue(registry.yearPack(-722).getEvents().stream().anyMatch(e -> "HS-PQ-0722-CHUNQIU".equals(e.getEventId())));
        assertTrue(registry.yearPack(1561).getEvents().stream().anyMatch(e -> "HS-MING-1561-TAIZHOU".equals(e.getEventId())));
        assertTrue(registry.yearPack(-196).getEvents().stream().anyMatch(e -> "HS-HAN-0196-HANXIN".equals(e.getEventId())));
        assertTrue(registry.yearPack(630).getEvents().stream().anyMatch(e -> "HS-ND-0630-JIELI".equals(e.getEventId())));
        assertTrue(registry.yearPack(1936).getEvents().stream().anyMatch(e -> "HS-ROC-1936-XIAN".equals(e.getEventId())));
        assertTrue(registry.yearPack(1878).getEvents().stream().anyMatch(e -> "HS-QING-1878-XINJIANG".equals(e.getEventId())));
        assertTrue(registry.yearPack(-200).getEvents().stream().anyMatch(e -> "HS-HAN-0200-BAIDENG".equals(e.getEventId())));
        assertTrue(registry.yearPack(196).getEvents().stream().anyMatch(e -> "HS-EH-0196-XUDU".equals(e.getEventId())));
        assertTrue(registry.yearPack(961).getEvents().stream().anyMatch(e -> "HS-NS-0961-BEIJIU".equals(e.getEventId())));
        assertTrue(registry.yearPack(1842).getEvents().stream().anyMatch(e -> "HS-QING-1842-NANJING".equals(e.getEventId())));
        assertTrue(registry.yearPack(1945).getEvents().stream().anyMatch(e -> "HS-ROC-1945-VICTORY".equals(e.getEventId())));
        assertTrue(registry.yearPack(-134).getEvents().stream().anyMatch(e -> "HS-HAN-0134-RUJIA".equals(e.getEventId())));
        assertTrue(registry.yearPack(485).getEvents().stream().anyMatch(e -> "HS-ND-0485-JUNTIAN".equals(e.getEventId())));
        assertTrue(registry.yearPack(1895).getEvents().stream().anyMatch(e -> "HS-QING-1895-MAGUAN".equals(e.getEventId())));
        assertTrue(registry.yearPack(1935).getEvents().stream().anyMatch(e -> "HS-ROC-1935-ZUNYI".equals(e.getEventId())));
        assertTrue(registry.yearPack(1521).getEvents().stream().anyMatch(e -> "HS-MING-1521-JIAJING".equals(e.getEventId())));
        assertTrue(registry.yearPack(1524).getEvents().stream().anyMatch(e -> "HS-MING-1524-DALI".equals(e.getEventId())));
        assertTrue(registry.yearPack(-684).getEvents().stream().anyMatch(e -> "HS-PQ-0684-CHANGSHAO".equals(e.getEventId())));
        assertTrue(registry.yearPack(-247).getEvents().stream().anyMatch(e -> "HS-PQ-0247-YINGZHENG".equals(e.getEventId())));
        assertTrue(registry.yearPack(-1042).getEvents().stream().anyMatch(e -> "HS-PQ-1042-ZHOUGONG".equals(e.getEventId())));
    }

    @Test
    void springAutumnWarringStatesRoyalLines() {
        assertTrue(registry.yearPack(-685).getRulers().stream().anyMatch(r -> "sq-huan".equals(r.getId())));
        assertTrue(registry.yearPack(-356).getRulers().stream().anyMatch(r -> "se-xiaogong".equals(r.getId())));
        assertTrue(registry.yearPack(-325).getRulers().stream().anyMatch(r -> "sz-wuling".equals(r.getId())));
        assertTrue(registry.yearPack(-514).getRulers().stream().anyMatch(r -> "wu-helu".equals(r.getId())));
        assertTrue(registry.yearPack(-506).getRulers().stream().anyMatch(r -> "wu-helu".equals(r.getId())));
        assertTrue(registry.yearPack(-473).getRulers().stream().anyMatch(r -> "yue-goujian".equals(r.getId())));
        assertTrue(registry.yearPack(-256).getRulers().stream().anyMatch(r -> "ez-nan".equals(r.getId())));
        assertTrue(registry.yearPack(-246).getRulers().stream().anyMatch(r -> "se-zheng".equals(r.getId())));
        assertTrue(registry.yearPack(-221).getRulers().stream().noneMatch(r -> "se-zheng".equals(r.getId())));
        assertTrue(registry.yearPack(-221).getRulers().stream().anyMatch(r -> "qin-shihuang".equals(r.getId())));
    }

    @Test
    void moreTextbookFiguresAppearInActiveYears() {
        assertTrue(registry.yearPack(-551).getFigures().stream().anyMatch(f -> "fig_kongzi".equals(f.getId())));
        assertTrue(registry.yearPack(200).getFigures().stream().anyMatch(f -> "fig_cao_cao".equals(f.getId())));
        assertTrue(registry.yearPack(1839).getFigures().stream().anyMatch(f -> "fig_lin_zexu".equals(f.getId())));
        assertTrue(registry.yearPack(-320).getFigures().stream().anyMatch(f -> "fig_mengzi".equals(f.getId())));
        assertTrue(registry.yearPack(755).getFigures().stream().anyMatch(f -> "fig_du_fu".equals(f.getId())));
        assertTrue(registry.yearPack(1405).getFigures().stream().anyMatch(f -> "fig_zheng_he".equals(f.getId())));
        assertTrue(registry.yearPack(353).getFigures().stream().anyMatch(f -> "fig_wang_xizhi".equals(f.getId())));
        assertTrue(registry.yearPack(629).getFigures().stream().anyMatch(f -> "fig_xuan_zang".equals(f.getId())));
        assertTrue(registry.yearPack(1449).getFigures().stream().anyMatch(f -> "fig_yu_qian".equals(f.getId())));
        assertTrue(registry.yearPack(-99).getFigures().stream().anyMatch(f -> "fig_su_wu".equals(f.getId())));
        assertTrue(registry.yearPack(313).getFigures().stream().anyMatch(f -> "fig_zu_ti".equals(f.getId())));
        assertTrue(registry.yearPack(1279).getFigures().stream().anyMatch(f -> "fig_wen_tianxiang".equals(f.getId())));
        assertTrue(registry.yearPack(-1042).getFigures().stream().anyMatch(f -> "fig_zhou_gong".equals(f.getId())));
        assertTrue(registry.yearPack(-506).getFigures().stream().anyMatch(f -> "fig_sun_wu".equals(f.getId())));
        assertTrue(registry.yearPack(-278).getFigures().stream().anyMatch(f -> "fig_qu_yuan".equals(f.getId())));
    }
}
