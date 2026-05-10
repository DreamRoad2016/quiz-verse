package net.qihoo.guessthepattern.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 双端口：主连接器为 {@code server.port}，额外再开一个 HTTP {@code server.http-port}。
 * 仅当配置了 {@code server.http-port} 时生效；本地默认单端口 8098 不加载此类。
 */
@Configuration
@ConditionalOnProperty(name = "server.http-port")
public class TomcatHttpConfig {

    @Value("${server.http-port}")
    private int httpPort;

    @Bean
    public ServletWebServerFactory webServerFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addAdditionalTomcatConnectors(createStandardConnector());
        factory.addConnectorCustomizers(connector ->
                connector.setProperty("relaxedQueryChars", "|{}[]"));
        return factory;
    }

    private Connector createStandardConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setPort(httpPort);
        return connector;
    }
}
