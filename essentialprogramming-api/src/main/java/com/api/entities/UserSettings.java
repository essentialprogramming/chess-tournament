package com.api.entities;

import com.api.model.Type;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private int id;

    @Column(name = "active")
    private boolean active;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private Player player;

    @ManyToOne
    @JoinColumn(name = "tournament_id", referencedColumnName = "id", nullable = false, unique = true)
    private Tournament tournament;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private Type type;
}
