package com.sjh.mapleguildtd.domain.character.repository;

import com.sjh.mapleguildtd.domain.character.entity.CharacterCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CharacterCacheRepository extends JpaRepository<CharacterCache, Long> {

    Optional<CharacterCache> findByCharacterName(String characterName);
}
