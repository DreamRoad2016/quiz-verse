package net.quizverse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quiz")
public class QuizProperties {

    private final Match match = new Match();
    private final Packs packs = new Packs();
    private final Security security = new Security();
    private final Wechat wechat = new Wechat();

    public Match getMatch() {
        return match;
    }

    public Packs getPacks() {
        return packs;
    }

    public Security getSecurity() {
        return security;
    }

    public Wechat getWechat() {
        return wechat;
    }

    public static class Match {
        /** memory | redis */
        private String store = "memory";
        private int ttlHours = 24;

        public String getStore() {
            return store;
        }

        public void setStore(String store) {
            this.store = store;
        }

        public int getTtlHours() {
            return ttlHours;
        }

        public void setTtlHours(int ttlHours) {
            this.ttlHours = ttlHours;
        }
    }

    public static class Packs {
        private String classpathLocation = "classpath:/packs";
        private String extraDir = "";

        public String getClasspathLocation() {
            return classpathLocation;
        }

        public void setClasspathLocation(String classpathLocation) {
            this.classpathLocation = classpathLocation;
        }

        public String getExtraDir() {
            return extraDir;
        }

        public void setExtraDir(String extraDir) {
            this.extraDir = extraDir;
        }
    }

    public static class Security {
        /** false = 本地网页免登录；aliyun 建议 true */
        private boolean enabled = false;
        private int sessionTtlHours = 72;
        private int guestPerIpPerHour = 60;
        private int startPerSessionPerHour = 30;
        private int guessPerSessionPerHour = 120;
        private int briefsPerSessionPerHour = 60;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getSessionTtlHours() {
            return sessionTtlHours;
        }

        public void setSessionTtlHours(int sessionTtlHours) {
            this.sessionTtlHours = sessionTtlHours;
        }

        public int getGuestPerIpPerHour() {
            return guestPerIpPerHour;
        }

        public void setGuestPerIpPerHour(int guestPerIpPerHour) {
            this.guestPerIpPerHour = guestPerIpPerHour;
        }

        public int getStartPerSessionPerHour() {
            return startPerSessionPerHour;
        }

        public void setStartPerSessionPerHour(int startPerSessionPerHour) {
            this.startPerSessionPerHour = startPerSessionPerHour;
        }

        public int getGuessPerSessionPerHour() {
            return guessPerSessionPerHour;
        }

        public void setGuessPerSessionPerHour(int guessPerSessionPerHour) {
            this.guessPerSessionPerHour = guessPerSessionPerHour;
        }

        public int getBriefsPerSessionPerHour() {
            return briefsPerSessionPerHour;
        }

        public void setBriefsPerSessionPerHour(int briefsPerSessionPerHour) {
            this.briefsPerSessionPerHour = briefsPerSessionPerHour;
        }
    }

    public static class Wechat {
        private String appId = "";
        private String appSecret = "";

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }
    }
}
