package com.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefereeSearchCriteria {

    private String name;

    @JsonIgnore
    public boolean isEmpty() {
        return name == null;
    }
}
