package com.api.output;

import lombok.*;

import java.io.Serializable;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerJSON implements Serializable {
    private String email;
    private String firstName;
    private String lastName;
    private double score;
    private String playerKey;

    @Override
    public String toString() {
        return "First name: " + firstName +
                ", Last name: " + lastName +
                ", Score:" + score ;
    }
}