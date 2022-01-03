package com.api.output;

import lombok.*;

import java.io.Serializable;
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResultConflictNotification implements Serializable {

    private String firstPlayerName;
    private String firstPlayerResult;
    private String secondPlayerName;
    private String secondPlayerResult;
}
