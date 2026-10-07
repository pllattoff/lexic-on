package com.lexicon.backend.repository;

import com.lexicon.backend.enums.UserRole;
import com.lexicon.backend.model.AppUser;
import com.lexicon.backend.model.UserIdentity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Integration tests for repository queries, entity mappings, and database constraints.
// Flyway migrations are applied before the tests.
// @DataJpaTest rolls back each test transaction.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AppUserRepositoryTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private UserIdentityRepository userIdentityRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByProviderAndProviderUserId_returnsIdentityWithLoadedAppUser_whenIdentityExists() {
        // GIVEN
        AppUser appUser = appUserRepository.save(new AppUser("test@example.com"));
        userIdentityRepository.saveAndFlush(new UserIdentity(appUser, "github", "42"));
        // Clear the persistence context so the test can verify that @EntityGraph eagerly loads appUser from the database,
        // which is required because authentication accesses it outside the Hibernate session
        entityManager.clear();

        // WHEN
        UserIdentity found = userIdentityRepository.findByProviderAndProviderUserId("github", "42").orElseThrow();

        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        assertThat(persistenceUnitUtil.isLoaded(found.getAppUser())).isTrue();
        assertThat(found.getAppUser().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void findByProviderAndProviderUserId_returnsEmpty_whenNoIdentityMatches() {
        // GIVEN
        AppUser appUser = appUserRepository.save(new AppUser("test@example.com"));
        userIdentityRepository.saveAndFlush(new UserIdentity(appUser, "github", "42"));

        // WHEN
        Optional<UserIdentity> found = userIdentityRepository.findByProviderAndProviderUserId("github", "43");

        // THEN
        assertThat(found).isEmpty();
    }

    @Test
    void save_setsRoleUserAndCreationTime_whenUserIsNew() {
        // WHEN
        AppUser saved = appUserRepository.saveAndFlush(new AppUser("test@example.com"));

        // THEN the user gets the default role and a creation time
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRole()).isEqualTo(UserRole.USER);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void save_throwsException_whenEmailAlreadyExists() {
        // GIVEN a stored user
        appUserRepository.saveAndFlush(new AppUser("test@example.com"));
        AppUser duplicate = new AppUser("test@example.com");

        // WHEN saving another user with the same email
        assertThatThrownBy(() -> appUserRepository.saveAndFlush(duplicate))
        // THEN the unique constraint rejects it
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_throwsException_whenProviderIdentityAlreadyExists() {
        // GIVEN
        AppUser first = appUserRepository.save(new AppUser("first@example.com"));
        AppUser second = appUserRepository.save(new AppUser("second@example.com"));
        userIdentityRepository.saveAndFlush(new UserIdentity(first, "github", "42"));
        UserIdentity duplicate = new UserIdentity(second, "github", "42");

        // WHEN storing the same provider identity for another user
        assertThatThrownBy(() -> userIdentityRepository.saveAndFlush(duplicate))
        // THEN the unique constraint rejects it
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}