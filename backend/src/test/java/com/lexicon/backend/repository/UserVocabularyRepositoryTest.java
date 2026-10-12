package com.lexicon.backend.repository;

import com.lexicon.backend.dto.LanguagePairStatusCount;
import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.enums.VocabularyStatus;
import com.lexicon.backend.model.AppUser;
import com.lexicon.backend.model.UserVocabulary;
import com.lexicon.backend.model.WordEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

// Integration tests for repository queries, entity mappings, and database constraints.
// Flyway migrations are applied before the tests.
// @DataJpaTest rolls back each test transaction.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserVocabularyRepositoryTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private WordEntryRepository wordEntryRepository;

    @Autowired
    private UserVocabularyRepository userVocabularyRepository;

    private AppUser user;
    private AppUser otherUser;
    private WordEntry hello;
    private WordEntry world;

    @BeforeEach
    void setUp() {
        user = appUserRepository.save(new AppUser("user@example.com"));
        otherUser = appUserRepository.save(new AppUser("other@example.com"));
        hello = wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "hello"));
        world = wordEntryRepository.save(new WordEntry(SourceLanguage.EN, "world"));
    }

    @Test
    void findByAppUserIdAndTargetLanguageAndWordEntryIn_returnsOnlyRowsOfGivenUserAndLanguage() {
        // GIVEN the user's "hello" for German, the user's "world" for Ukrainian, and another user's "hello" for German
        userVocabularyRepository.save(new UserVocabulary(user, hello, TargetLanguage.DE, VocabularyStatus.REVIEW));
        userVocabularyRepository.save(new UserVocabulary(user, world, TargetLanguage.UK, VocabularyStatus.KNOWN));
        userVocabularyRepository.save(new UserVocabulary(otherUser, hello, TargetLanguage.DE, VocabularyStatus.KNOWN));

        // WHEN asking for the user's German statuses of both words
        List<UserVocabulary> rows = userVocabularyRepository
                .findByAppUserIdAndTargetLanguageAndWordEntryIn(user.getId(), TargetLanguage.DE, List.of(hello, world));

        // THEN neither the other language nor the other user's row is returned
        assertThat(rows)
                .extracting(row -> row.getWordEntry().getLemma(), UserVocabulary::getStatus)
                .containsExactly(tuple("hello", VocabularyStatus.REVIEW));
    }

    @Test
    void findByAppUserIdAndWordEntryAndTargetLanguage_returnsRow_whenWordIsTracked() {
        // GIVEN a tracked word
        userVocabularyRepository.save(new UserVocabulary(user, hello, TargetLanguage.DE, VocabularyStatus.REVIEW));

        // WHEN
        Optional<UserVocabulary> row = userVocabularyRepository
                .findByAppUserIdAndWordEntryAndTargetLanguage(user.getId(), hello, TargetLanguage.DE);

        // THEN
        assertThat(row).get().extracting(UserVocabulary::getStatus).isEqualTo(VocabularyStatus.REVIEW);
    }

    @Test
    void findByAppUserIdAndWordEntryAndTargetLanguage_returnsEmpty_whenWordIsTrackedForAnotherLanguage() {
        // GIVEN
        userVocabularyRepository.save(new UserVocabulary(user, hello, TargetLanguage.UK, VocabularyStatus.REVIEW));

        // WHEN
        Optional<UserVocabulary> row = userVocabularyRepository
                .findByAppUserIdAndWordEntryAndTargetLanguage(user.getId(), hello, TargetLanguage.DE);

        // THEN
        assertThat(row).isEmpty();
    }

    @Test
    void deleteByAppUserIdAndWordEntryAndTargetLanguage_deletesOnlyMatchingRow() {
        // GIVEN the same word tracked by two users, and by the first user for two languages
        userVocabularyRepository.save(new UserVocabulary(user, hello, TargetLanguage.DE, VocabularyStatus.REVIEW));
        userVocabularyRepository.save(new UserVocabulary(user, hello, TargetLanguage.UK, VocabularyStatus.KNOWN));
        userVocabularyRepository.save(new UserVocabulary(otherUser, hello, TargetLanguage.DE, VocabularyStatus.KNOWN));

        // WHEN the first user deletes the German row
        userVocabularyRepository.deleteByAppUserIdAndWordEntryAndTargetLanguage(user.getId(), hello, TargetLanguage.DE);

        // THEN the user's Ukrainian row and the other user's row are left
        assertThat(userVocabularyRepository.findAll())
                .extracting(row -> row.getAppUser().getId(), UserVocabulary::getTargetLanguage)
                .containsExactlyInAnyOrder(
                        tuple(user.getId(), TargetLanguage.UK),
                        tuple(otherUser.getId(), TargetLanguage.DE));
    }

    @Test
    void deleteLanguagePair_deletesAllStatusesOfTheLanguagePair_andKeepsOtherRows() {
        // GIVEN two words of the user for German with different statuses, the user's Ukrainian row,
        // and another user's German row
        userVocabularyRepository.save(new UserVocabulary(user, hello, TargetLanguage.DE, VocabularyStatus.REVIEW));
        userVocabularyRepository.save(new UserVocabulary(user, world, TargetLanguage.DE, VocabularyStatus.KNOWN));
        userVocabularyRepository.save(new UserVocabulary(user, hello, TargetLanguage.UK, VocabularyStatus.KNOWN));
        userVocabularyRepository.save(new UserVocabulary(otherUser, hello, TargetLanguage.DE, VocabularyStatus.KNOWN));

        // WHEN the user deletes the English → German dictionary
        userVocabularyRepository.deleteLanguagePair(user.getId(), SourceLanguage.EN, TargetLanguage.DE);

        // THEN only the user's Ukrainian row and the other user's row are left
        assertThat(userVocabularyRepository.findAll())
                .extracting(row -> row.getAppUser().getId(), UserVocabulary::getTargetLanguage)
                .containsExactlyInAnyOrder(
                        tuple(user.getId(), TargetLanguage.UK),
                        tuple(otherUser.getId(), TargetLanguage.DE));
    }

    @Test
    void countWordsByLanguagePairAndStatus_returnsCountsPerPairAndStatus_forGivenUserOnly() {
        // GIVEN the user's two German words (one REVIEW, one KNOWN), one Ukrainian word, and another user's word
        userVocabularyRepository.save(new UserVocabulary(user, hello, TargetLanguage.DE, VocabularyStatus.REVIEW));
        userVocabularyRepository.save(new UserVocabulary(user, world, TargetLanguage.DE, VocabularyStatus.KNOWN));
        userVocabularyRepository.save(new UserVocabulary(user, hello, TargetLanguage.UK, VocabularyStatus.REVIEW));
        userVocabularyRepository.save(new UserVocabulary(otherUser, hello, TargetLanguage.DE, VocabularyStatus.REVIEW));

        // WHEN
        List<LanguagePairStatusCount> counts = userVocabularyRepository.countWordsByLanguagePairAndStatus(user.getId());

        // THEN the counts are grouped by pair and status, sorted by pair, and the other user's word is not counted
        assertThat(counts)
                .extracting(
                        LanguagePairStatusCount::targetLanguage,
                        LanguagePairStatusCount::status,
                        LanguagePairStatusCount::wordCount)
                .containsExactlyInAnyOrder(
                        tuple(TargetLanguage.DE, VocabularyStatus.REVIEW, 1L),
                        tuple(TargetLanguage.DE, VocabularyStatus.KNOWN, 1L),
                        tuple(TargetLanguage.UK, VocabularyStatus.REVIEW, 1L));
        assertThat(counts.getFirst().targetLanguage()).isEqualTo(TargetLanguage.DE);
        assertThat(counts.getLast().targetLanguage()).isEqualTo(TargetLanguage.UK);
    }

    @Test
    void findUserVocabularies_returnsNewestFirst_whenSeveralWordsHaveTheStatus() {
        // GIVEN "hello" saved before "world", both REVIEW for German
        userVocabularyRepository.save(new UserVocabulary(user, hello, TargetLanguage.DE, VocabularyStatus.REVIEW));
        userVocabularyRepository.save(new UserVocabulary(user, world, TargetLanguage.DE, VocabularyStatus.REVIEW));

        // WHEN
        List<UserVocabulary> rows = userVocabularyRepository
                .findUserVocabularies(user.getId(), SourceLanguage.EN, TargetLanguage.DE, VocabularyStatus.REVIEW);

        // THEN the word saved last comes first
        assertThat(rows)
                .extracting(row -> row.getWordEntry().getLemma())
                .containsExactly("world", "hello");
    }

    @Test
    void findUserVocabularies_returnsOnlyRowsOfGivenUserPairAndStatus() {
        // GIVEN the user's REVIEW German word, a KNOWN German word, a REVIEW Ukrainian word, and another user's REVIEW German word
        userVocabularyRepository.save(new UserVocabulary(user, hello, TargetLanguage.DE, VocabularyStatus.REVIEW));
        userVocabularyRepository.save(new UserVocabulary(user, world, TargetLanguage.DE, VocabularyStatus.KNOWN));
        userVocabularyRepository.save(new UserVocabulary(user, world, TargetLanguage.UK, VocabularyStatus.REVIEW));
        userVocabularyRepository.save(new UserVocabulary(otherUser, world, TargetLanguage.DE, VocabularyStatus.REVIEW));

        // WHEN asking for the user's REVIEW words of the English-German dictionary
        List<UserVocabulary> rows = userVocabularyRepository
                .findUserVocabularies(user.getId(), SourceLanguage.EN, TargetLanguage.DE, VocabularyStatus.REVIEW);

        // THEN only the matching row is returned
        assertThat(rows)
                .extracting(row -> row.getWordEntry().getLemma())
                .containsExactly("hello");
    }

    @Test
    void save_throwsException_whenUserWordAndTargetLanguageAlreadyTracked() {
        // GIVEN a tracked word
        userVocabularyRepository.saveAndFlush(new UserVocabulary(user, hello, TargetLanguage.DE, VocabularyStatus.REVIEW));
        UserVocabulary duplicate = new UserVocabulary(user, hello, TargetLanguage.DE, VocabularyStatus.KNOWN);

        // WHEN tracking the same word for the same user and language again
        assertThatThrownBy(() -> userVocabularyRepository.saveAndFlush(duplicate))
        // THEN the unique constraint rejects it
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}