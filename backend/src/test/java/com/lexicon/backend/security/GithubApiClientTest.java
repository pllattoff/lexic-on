package com.lexicon.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// GithubApiClient gets the GitHub API base URL from src/test/resources/application.properties
@RestClientTest(GithubApiClient.class)
class GithubApiClientTest {

    private static final String USER_EMAILS_PATH = "/user/emails";

    @Autowired
    private GithubApiClient githubApiClient;

    @Autowired
    private MockRestServiceServer mockRestServiceServer;

    @Value("${github.api.url}")
    private String githubApiBaseUrl;

    @Test
    void findPrimaryVerifiedEmail_returnsPrimaryVerifiedEmail_whenAccountHasSeveralEmails() {
        // GIVEN
        mockRestServiceServer
                .expect(method(HttpMethod.GET))
                .andExpect(requestTo(githubApiBaseUrl + USER_EMAILS_PATH))
                .andExpect(header("Authorization", "Bearer token-value"))
                .andRespond(withSuccess("""
                        [
                          {"email": "old@example.com", "primary": false, "verified": true, "visibility": null},
                          {"email": "test@example.com", "primary": true, "verified": true, "visibility": "private"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        // WHEN
        Optional<String> email = githubApiClient.findPrimaryVerifiedEmail("token-value");

        // THEN only the primary verified email is returned
        assertThat(email).contains("test@example.com");
        mockRestServiceServer.verify();
    }

    @Test
    void findPrimaryVerifiedEmail_returnsEmpty_whenPrimaryEmailIsNotVerified() {
        // GIVEN
        mockRestServiceServer
                .expect(method(HttpMethod.GET))
                .andExpect(requestTo(githubApiBaseUrl + USER_EMAILS_PATH))
                .andRespond(withSuccess("""
                        [{"email": "test@example.com", "primary": true, "verified": false}]
                        """, MediaType.APPLICATION_JSON));

        // WHEN
        Optional<String> email = githubApiClient.findPrimaryVerifiedEmail("token-value");

        // THEN
        assertThat(email).isEmpty();
        mockRestServiceServer.verify();
    }

    @Test
    void findPrimaryVerifiedEmail_throwsOAuth2AuthenticationException_whenGithubReturnsServerError() {
        // GIVEN
        mockRestServiceServer
                .expect(method(HttpMethod.GET))
                .andExpect(requestTo(githubApiBaseUrl + USER_EMAILS_PATH))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // WHEN
        assertThatThrownBy(() -> githubApiClient.findPrimaryVerifiedEmail("token-value"))
        // THEN the HTTP failure is wrapped, keeping the original cause
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasCauseInstanceOf(RestClientException.class);

        mockRestServiceServer.verify();
    }
}