package com.sjh.mapleguildtd.domain.guild.entity;

import com.sjh.mapleguildtd.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "guild_cache",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_guild_name_world",
                columnNames = {"guild_name", "world_name"}
        ))
public class GuildCache extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guild_name", nullable = false, length = 50)
    private String guildName;

    @Column(name = "world_name", nullable = false, length = 20)
    private String worldName;

    @Column(nullable = false, length = 100)
    private String oguildId;

    @Column(nullable = false, length = 50)
    private String masterName;

    @Column(nullable = false)
    private int guildLevel;

    @Column(nullable = false)
    private int memberCount;

    @OneToMany(mappedBy = "guildCache", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GuildMember> members = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime cachedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder
    private GuildCache(String guildName, String worldName, String oguildId,
                       String masterName, int guildLevel, int memberCount) {
        this.guildName = guildName;
        this.worldName = worldName;
        this.oguildId = oguildId;
        this.masterName = masterName;
        this.guildLevel = guildLevel;
        this.memberCount = memberCount;
        this.cachedAt = LocalDateTime.now();
        this.expiresAt = this.cachedAt.plusHours(24);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void addMember(GuildMember member) {
        members.add(member);
    }
}