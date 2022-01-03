package com.api.entities;

import com.util.enums.PlatformType;
import com.util.jpa.StringListConverter;
import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "user_platform")
@Table(name = "user_platform")
public class UserPlatform {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private int id;

    @Convert(converter = StringListConverter.class)
    @Column(name = "roles")
    private List<String> roles = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private PlatformType platformType;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;
}
