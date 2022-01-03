package com.api.output;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoundJSON implements Serializable {
    private String roundKey;
    private int number;
    private String tournamentKey;
    private List<MatchJSON> matches;

    @Override
    public String toString() {
        return "\n" + "  Round key: " + roundKey + "\n" +
                "  Number: " + number + "\n" +
                "  Matches:" + "\n" + "           {" +"\n"+ matches + "\n" +"          }" + "\n" ;
    }
}
