package com.backend.repository;

import com.backend.model.AppSetting;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AppSettingRepository extends MongoRepository<AppSetting, String> {
    Optional<AppSetting> findByKey(String key);
    List<AppSetting> findByKeyStartingWith(String prefix);
}
