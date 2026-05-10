package net.qihoo.guessthepattern.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @description: SwaggerConfig
 * @author: qiruyu
 * @date: 2020/4/27
 **/
@Configuration
@EnableSwagger2
@EnableKnife4j
public class SwaggerConfig {

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Quiz Verse API")
                .description("猜人物后端（当前含 10×10 猜飞机 Demo）")
                .termsOfServiceUrl("http://localhost:8098/doc.html")
                .version("1.0")
                .build();
    }

    @Bean(value = "defaultApi2")
    public Docket defaultApi2() {
        return new Docket(DocumentationType.SWAGGER_2)
                // 指定构建api文档的详细信息的方法：apiInfo()
                .apiInfo(apiInfo())
                .enable(true)
                .select()

                // 指定要生成api接口的包路径，这里把controller作为包路径，生成controller中的所有接口
                .apis(RequestHandlerSelectors.basePackage("net.qihoo"))
//                //错误路径不监控
//                .paths(Predicates.not(PathSelectors.regex("/error.*")))
//                //actuator路径跳过
//                .paths(Predicates.not(PathSelectors.regex("/actuator.*")))
                .paths(PathSelectors.any())
                .build();

    }





}
