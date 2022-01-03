package com.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Getter
public enum Result {
    FIRST("1 - 0"),
    SECOND("0 - 1"),
    DRAW("1/2 - 1/2");

    @JsonProperty("value")
    private String value;

    Result(String value) {
        this.value = value;
    }

    @JsonCreator
    public static Result fromJson(@JsonProperty("result") String result) {
        return valueOf(result);
    }
}
