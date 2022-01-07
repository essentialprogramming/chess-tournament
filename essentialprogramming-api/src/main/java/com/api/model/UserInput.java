package com.api.model;

import com.api.model.constraint.FieldMatch;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.validation.constraints.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldMatch(first = "password", second = "confirmPassword")

public class UserInput {

    @NotNull(message = "You need to provide a valid email")
    @Email(message = "Email should be valid", regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$")
    @JsonProperty("email")
    private String email;

    @NotNull(message = "You need to provide a valid name")
    @Pattern(regexp = "[a-zA-Z]+", message = "Invalid first name")
    @JsonProperty("firstName")
    private String firstName;

    @NotNull(message = "You need to provide a valid name")
    @Pattern(regexp = "[a-zA-Z]+", message = "Invalid second name")
    @JsonProperty("lastName")
    private String lastName;

    @Size(min = 5, max = 20, message = "Your phone number may have 5 to 20 numbers")
    @Pattern(regexp = "^\\(?(\\d{1,20})\\)?[- ]?(\\d{1,20})[- ]?(\\d{1,20})$",
            message = "Invalid phone number")
    @JsonProperty("phone")
    private String phone;

    @NotNull(message = "You need to provide a valid password")
    @Size(min = 8, max = 20, message = "Your password must have at least 8 characters")
    @JsonProperty("password")
    private String password;

    @JsonProperty("confirmPassword")
    private String confirmPassword;


}
