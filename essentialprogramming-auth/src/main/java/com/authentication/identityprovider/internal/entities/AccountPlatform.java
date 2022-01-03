package com.authentication.identityprovider.internal.entities;

import com.util.enums.PlatformType;
import com.util.jpa.StringListConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "AccountPlatform")
@Table(name = "user_platform")
public class AccountPlatform {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private PlatformType platformType;

    @Convert(converter = StringListConverter.class)
    @Column(name = "roles")
    private List<String> roles = new ArrayList<>();

    public AccountPlatform(Account account, PlatformType platformType) {
        this.account = account;
        this.platformType = platformType;
    }

}

