package net.quizverse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quiz")
public class QuizProperties {

    private final Match match = new Match();
    private final Packs packs = new Packs();

    public Match getMatch() {
        return match;
    }

    public Packs getPacks() {
        return packs;
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
}
