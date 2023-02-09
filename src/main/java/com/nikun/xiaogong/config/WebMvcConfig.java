package com.nikun.xiaogong.config;


import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import com.nikun.xiaogong.common.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.List;

/**
 * 关于 WebMvcConfigurationSupport 类和 WebMvcConfigurer 接口的注意事项：
 * 1. 对于多个@Configuratuion，WebMvcConfigurationSupport 和 WebMvcConfigurer不能同时出现
 * 2. 对于多个@Configuratuion，只有一个能继承 WebMvcConfigurationSupport，其他的都会失效
 * 3. 对于多个@Configuratuion，都可以实现 WebMvcConfigurer，不会出现失效的问题
 */

@Slf4j
@Configuration
@EnableSwagger2
@EnableKnife4j
// extends WebMvcConfigurationSupport
public class WebMvcConfig implements WebMvcConfigurer {
    /**
     * 设置静态资源映射
     * @param registry
     */

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始进行静态资源映射！");
        // addResourceHandler("xxxx/"**)  所有 /xxxx/ 开头的请求都会去后面配置的路径下查找资源
        registry.addResourceHandler("/backend/**").addResourceLocations("classpath:/backend/");
        registry.addResourceHandler("/front/**").addResourceLocations("classpath:/front/");
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * 扩展mvc框架的消息转换器
     * @param converters
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("扩展消息转换器");
        // 创建消息转换器对象
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();

        // 设置对象转换器，底层使用Jackson将Java对象转为json
        messageConverter.setObjectMapper(new JacksonObjectMapper());

        // 将上面的消息转换器对象追加到mvc框架的转换器集合中
        // 第一个参数index = 0其实是在设置这个转换器的优先级，优先使用这个自定义转换器
        converters.add(0, messageConverter);
    }

    @Bean
    public Docket createRestApi() {
        // 文档类型
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.nikun.xiaogong.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("小工外卖")
                .version("2.0")
                .description("小工外卖接口文档")
                .build();
    }
}
