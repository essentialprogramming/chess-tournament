package com.api.output;

import lombok.*;

import java.io.Serializable;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchResultJSON implements Serializable {

    private String key;
    private PlayerJSON firstPlayer;
    private PlayerJSON secondPlayer;
    private String firstPlayerResult;
    private String secondPlayerResult;
    private String finalResult;

    @Override
    public String toString() {
        return "{\n\" +" +
                "                   First player: " + firstPlayer + "\n" +
                "                      Second player: " + secondPlayer + "\n" +
                "                      First player result: " + firstPlayerResult +  "\n" +
                "                      Second player result: " + secondPlayerResult +  "\n"  +
                "                      Final result: " + finalResult +  "\n" +
                "                         }";
    }
}
