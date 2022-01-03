package com.api.model;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Patterns {
    public static final String YYYY_MM_DD_HH_MM_REGEXP = "[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1]) (2[0-3]|[01][0-9]):[0-5][0-9]";
    public static final Pattern YYYY_MM_DD_HH_MM_PATTERN = Pattern.compile(YYYY_MM_DD_HH_MM_REGEXP);

    public static  final  String ASC_DESC_REGEXP = "(asc|desc)";
    public static final Pattern ASC_DESC_PATTERN = Pattern.compile(ASC_DESC_REGEXP);

    public static final String ROUND_NO_REGEXP = "^[1-9]*$";
    public static final Pattern ROUND_NO_PATTERN = Pattern.compile(ROUND_NO_REGEXP);

    public static final String GAME_STATE_REGEXP = "(CREATED|ACTIVE|ENDED)";
    public static final Pattern GAME_STATE_PATTERN = Pattern.compile(GAME_STATE_REGEXP);

}