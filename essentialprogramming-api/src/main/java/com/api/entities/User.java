package com.api.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "user")
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
        name = "type",
        discriminatorType = DiscriminatorType.STRING)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private int id;

    @Column(name = "firstname")
    private String firstName;

    @Column(name = "lastname")
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone")
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_language_id")
    private Language language;

    @Column(name = "validated")
    private boolean validated;

    @Column(name = "user_key")
    private String userKey;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @Column(name = "active")
    private boolean active;

    @Column(name = "deleted")
    private boolean deleted;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "modified_by")
    private Integer modifiedBy;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "password")
    private String password;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
    private List<UserPlatform> userPlatformList = new ArrayList<>();

    public String getFullName() {
        return Optional.ofNullable(firstName).orElse("") + " " + Optional.ofNullable(lastName).orElse("");
    }

    @OneToMany(mappedBy = "user")
    private List<TournamentUser> tournamentUsers;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "tournament_referee",
            joinColumns = {@JoinColumn(name = "referee_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "tournament_id", referencedColumnName = "id")})
    private Set<Tournament> tournaments = new HashSet<>();

    @OneToMany(mappedBy = "referee", cascade = CascadeType.ALL)
    private List<Match> matches;
}
