package com.lightcare.server.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {
    List<ProfileEntity> findByOwnerUserId(Long ownerUserId);
    List<ProfileEntity> findByManagedByUserId(Long managedByUserId);
    long countByOwnerUserId(Long ownerUserId);
}
