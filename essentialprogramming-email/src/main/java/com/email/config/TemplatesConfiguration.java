package com.email.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.email.service.EmailService;
import com.email.service.SendGridEmailService;
import com.email.service.TemplateService;
import com.email.service.ThymeleafTemplateService;

@Configuration
public class TemplatesConfiguration {

    @Bean
    public EmailService loadEmailService() {
        return new SendGridEmailService();
    }

    @Bean
    public TemplateService loadTemplateService() {
        return new ThymeleafTemplateService("mail");
    }


}
