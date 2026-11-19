package medibridgepayermock.medibridgepayermock;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebSecurityConfig implements WebMvcConfigurer {

    private final SmartSecurityInterceptor smartSecurityInterceptor;

    public WebSecurityConfig(SmartSecurityInterceptor smartSecurityInterceptor) {
        this.smartSecurityInterceptor = smartSecurityInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(smartSecurityInterceptor)
                .addPathPatterns("/fhir/Claim/$submit", "/fhir/Questionnaire/**");
    }
}
