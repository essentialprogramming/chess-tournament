package com.email.config;

import com.crypto.Crypt;
import com.util.cloud.ConfigurationManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import com.email.service.EmailService;
import com.email.service.SendGridEmailService;
import com.email.service.TemplateService;
import com.email.service.ThymeleafTemplateService;

import java.security.GeneralSecurityException;
import java.util.Collections;


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
