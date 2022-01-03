package com.email.model;

import java.util.Optional;

public enum Template {

    PARTICIPATE_HTML("html/participate"),

    SUCCESS_HTML("html/success"),

    PARENT_HTML("html/parent"),

    APOLOGY_HTML("html/apology_email");

    public String page;
    public String fragment = null;
    public Template master = null;

    Template(String page) {
        this.page = page;
    }

    Template(String page, String fragment, Template master) {
        this.page = page;
        this.fragment = fragment;
        this.master = master;
    }

    public Optional<String> getPage() {
        return Optional.of(page);
    }

    public Optional<String> getFragment() {
        return Optional.ofNullable(fragment);
    }

    public Optional<Template> getMaster() {
        return Optional.ofNullable(master);
    }
}
