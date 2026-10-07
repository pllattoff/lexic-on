package com.lexicon.backend.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

@Component
public class GithubApiClient {

    private final RestClient restClient;

    public GithubApiClient(RestClient.Builder restClientBuilder, @Value("${github.api.url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    // GitHub may hide a private email from the profile, but exposes account emails
    // through this endpoint when the "user:email" scope is granted.
    public Optional<String> findPrimaryVerifiedEmail(String accessToken) {
        List<GithubEmail> emails;
        try {
            emails = restClient.get()
                    .uri("/user/emails")
                    .accept(MediaType.parseMediaType("application/vnd.github+json"))
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<GithubEmail>>() {});
        } catch (RestClientException e) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("github_emails_request_failed"), "GitHub emails request failed", e);
        }

        if (emails == null) {
            return Optional.empty();
        }

        return emails.stream()
                .filter(email -> email.primary() && email.verified())
                .map(GithubEmail::email)
                .findFirst();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GithubEmail(String email, boolean primary, boolean verified) {
    }
}