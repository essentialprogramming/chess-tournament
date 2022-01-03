package com.api.output;

import lombok.*;

import java.io.Serializable;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantStatusJSON implements Serializable {

   private UserJSON userJSON;
   private String status;
}
