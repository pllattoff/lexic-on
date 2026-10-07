package com.lexicon.backend.repository;

import com.lexicon.backend.model.UserIdentity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {

    // Include appUser in the entity graph to fetch it eagerly,
    // because authentication accesses it in the Spring Security filter outside the Hibernate session
    @EntityGraph(attributePaths = "appUser")
    Optional<UserIdentity> findByProviderAndProviderUserId(String provider, String providerUserId);
}