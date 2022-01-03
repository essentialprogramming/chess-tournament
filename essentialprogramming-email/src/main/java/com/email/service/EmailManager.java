package com.email.service;

import com.email.model.EmailErrorCode;
import com.util.exceptions.ServiceException;
import com.email.model.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
public class EmailManager {

    private final EmailService emailService;
    private final TemplateService templateService;

    @Autowired
    public EmailManager(EmailService emailService, TemplateService templateService) {
        this.emailService = emailService;
        this.templateService = templateService;
    }

    public void send(String recipient, String subject, Template template, Map<String, Object> content, Locale locale) {
        if (recipient == null || subject == null || content == null || template == null) {
            throw new ServiceException(EmailErrorCode.UNABLE_TO_SEND_EMAIL,
                    "Mandatory non-null parameters: recipient, subject, template, content");
        }

        emailService.sendMail(recipient, subject, templateService.generateHTML(template, content, locale));
    }
}

