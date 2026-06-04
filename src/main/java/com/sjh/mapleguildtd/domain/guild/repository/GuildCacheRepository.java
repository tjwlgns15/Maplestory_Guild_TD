package com.sjh.mapleguildtd.domain.guild.repository;

import com.sjh.mapleguildtd.domain.guild.entity.GuildCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuildCacheRepository extends JpaRepository<GuildCache, Long> {

    Optional<GuildCache> findByGuildNameAndWorldName(String guildName, String worldName);
}