package com.sjh.mapleguildtd.domain.guild.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "guild_member",
        indexes = @Index(name = "idx_guild_cache_id", columnList = "guild_cache_id"))
public class GuildMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guild_cache_id", nullable = false)
    private GuildCache guildCache;

    @Column(nullable = false, length = 50)
    private String characterName;

    @Builder
    private GuildMember(GuildCache guildCache, String characterName) {
        this.guildCache = guildCache;
        this.characterName = characterName;
    }
}