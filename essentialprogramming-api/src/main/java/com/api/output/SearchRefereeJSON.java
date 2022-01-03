package com.api.output;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchRefereeJSON {

    private String userKey;
    public String firstName;
    private String lastName;
    private String email;
    private Long numberOfAssignedMatches;

    public SearchRefereeJSON(String userKey, String firstName, String lastName, String email) {
        this.userKey = userKey;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }
}
