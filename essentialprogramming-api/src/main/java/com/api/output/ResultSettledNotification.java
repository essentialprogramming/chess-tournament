package com.api.output;

import lombok.*;

import java.io.Serializable;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResultSettledNotification implements Serializable {

    private String firstPlayerName;
    private String secondPlayerName;
    private String result;
}
