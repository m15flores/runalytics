package com.runalytics.activity.config;

import com.runalytics.activity.filter.RateLimitFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the RateLimitFilter as a servlet filter.
 * Can be disabled in tests by setting runalytics.rate-limit.enabled=false.
 */
@Configuration
@ConditionalOnProperty(name = "runalytics.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitConfig {

    @Bean
    public RateLimitFilter rateLimitFilter() {
        return new RateLimitFilter();
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter rateLimitFilter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(rateLimitFilter);
        registration.addUrlPatterns("/activities", "/activities/fit");
        registration.setName("rateLimitFilter");
        registration.setOrder(1);
        return registration;
    }
}
