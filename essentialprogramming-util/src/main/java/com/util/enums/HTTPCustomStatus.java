package com.util.enums;

public enum HTTPCustomStatus {

    OK(200),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    /**
     * The request is missing a required parameter, includes an unsupported parameter value,
     * repeats a parameter or is otherwise malformed.
     *
     */
    INVALID_REQUEST(422),
    BUSINESS_EXCEPTION(500)
;

    private final Integer value;

    HTTPCustomStatus(Integer value) {
        this.value = value;
    }

    public int value(){
        return value;
    }
}
