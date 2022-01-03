package com.email.service;

import com.email.model.Template;

import java.util.Locale;
import java.util.Map;

public interface TemplateService {

    String generateHTML(Template template, Map<String, Object> contentVariables);
    String generateHTML(Template template, Map<String, Object> contentVariables, Locale locale);
    byte[] generatePDF (Template template, Map<String, Object> contentVariables, Locale locale);
}
