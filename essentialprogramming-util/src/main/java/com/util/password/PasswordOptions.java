package com.util.password;


import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordOptions {

    //     Minimum required length. Defaults to 6.
    public int requiredLength;

    //     Minimum number of unique chars. Defaults to 1.
    public int requiredUniqueChars;

    //     Flag indicating if passwords must contain a non-alphanumeric character. Defaults to true.
    public boolean requireNonAlphanumeric;

    //     Flag indicating if passwords must contain a lower case ASCII character. Defaults to true.
    public boolean requireLowercase;

    //     Flag indicating if passwords must contain a upper case ASCII character. Defaults to true.
    public boolean requireUppercase;

    //     Flag indicating if passwords must contain a digit. Defaults to true.
    public boolean requireDigit;

    public boolean alphanumericOnly;

}
