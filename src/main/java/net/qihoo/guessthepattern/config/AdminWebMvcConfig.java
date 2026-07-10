package net.qihoo.guessthepattern.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class AdminWebMvcConfig implements WebMvcConfigurer {

    @Resource
    private AdminApiAuthInterceptor adminApiAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminApiAuthInterceptor)
                .addPathPatterns("/api/admin/**", "/api/studio/**");
    }
}
