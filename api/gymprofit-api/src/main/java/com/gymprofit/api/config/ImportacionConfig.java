package com.gymprofit.api.config;

import com.gymprofit.api.config.security.ClaveImportacion;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// ============================================================
// ImportacionConfig — pone ClaveImportacion delante de /importacion/** (GP-164)
// ============================================================
@Configuration
public class ImportacionConfig implements WebMvcConfigurer {

    private final ClaveImportacion claveImportacion;

    public ImportacionConfig(ClaveImportacion claveImportacion) {
        this.claveImportacion = claveImportacion;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(claveImportacion).addPathPatterns("/importacion/**");
    }
}
