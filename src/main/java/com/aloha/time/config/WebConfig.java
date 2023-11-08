package com.aloha.time.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry
				.addResourceHandler("/.well-known/.pki-validation/**")
				.addResourceLocations("classpath:/.well-known/.pki-validation/")
				.setCachePeriod(20);
	}

	@Override
	        public void addCorsMappings(CorsRegistry registry) {
			        registry.addMapping("/**")
					                .allowedOrigins("*")
							                .allowedMethods("GET", "POST");
				    }

}

