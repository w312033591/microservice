package com.xmair.restapi.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xmair.core.util.DateConverter;
import com.xmair.restapi.apiversion.VersionHandlerMapping;
import io.undertow.UndertowOptions;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig extends WebMvcConfigurationSupport {

    @Autowired
    private RequestMappingHandlerAdapter handlerAdapter;



    /**
     * 增加字符串转日期的功能
     */
    @PostConstruct
    public void initEditableValidation() {
        ConfigurableWebBindingInitializer initializer = (ConfigurableWebBindingInitializer) handlerAdapter
                .getWebBindingInitializer();
        if (initializer.getConversionService() != null) {
            GenericConversionService genericConversionService = (GenericConversionService) initializer
                    .getConversionService();
            genericConversionService.addConverter(new DateConverter());
        }
    }

    /*增加ajax跨域访问支持*/
    @Override
    protected void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**")
                .allowedMethods("get","post")
                .allowedOrigins("*")
                .allowedHeaders("*");
       // super.addCorsMappings(registry);
    }

    /*自定义jackson 消息序列化*/

    public MappingJackson2HttpMessageConverter getCustomJacksonConverter(){
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        //设置日期格式
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleDateFormat smt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        objectMapper.setDateFormat(smt);
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mappingJackson2HttpMessageConverter.setObjectMapper(objectMapper);
        //设置中文编码格式
        List<MediaType> list = new ArrayList<MediaType>();
        list.add(MediaType.APPLICATION_JSON_UTF8);
        list.add(MediaType.APPLICATION_JSON);
        mappingJackson2HttpMessageConverter.setSupportedMediaTypes(list);
        return mappingJackson2HttpMessageConverter;
    }

    //添加protobuf支持，需要client指定accept-type：application/x-protobuf
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
        stringConverter.setDefaultCharset(Charset.forName("utf-8"));
        converters.add(0,stringConverter);
        converters.add(0,getCustomJacksonConverter());

       // converters.add(1,new ProtobufHttpMessageConverter());
    }

    @Override
    @Bean
    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
        RequestMappingHandlerMapping handlerMapping = new VersionHandlerMapping("v");
        handlerMapping.setOrder(0);

        handlerMapping.setInterceptors(getInterceptors());
        return handlerMapping;
    }


    /**
     * 发现如果继承了WebMvcConfigurationSupport，则在yml中配置的相关内容会失效。
     * 需要重新指定静态资源
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        super.addResourceHandlers(registry);
    }



    /**
     * 配置servlet处理
     */
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer)
    {

        configurer.enable();
    }







    /*设置默认首页*/
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/getWeixinUser");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
        super.addViewControllers(registry);
    }

    /*忽略大小写*/
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        AntPathMatcher pathMatcher = new AntPathMatcher();
        pathMatcher.setCaseSensitive(false);
        configurer.setPathMatcher(pathMatcher);
        configurer.setUseSuffixPatternMatch(false).
                setUseTrailingSlashMatch(true);
    }


}