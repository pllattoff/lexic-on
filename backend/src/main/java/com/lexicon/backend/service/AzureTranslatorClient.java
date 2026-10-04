package com.lexicon.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.exception.TranslationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class AzureTranslatorClient {

    private final RestClient restClient;
    private final String subscriptionKey;
    private final String region;

    public AzureTranslatorClient(
            RestClient.Builder restClientBuilder,
            @Value("${azure.translator.endpoint}") String endpoint,
            @Value("${azure.translator.key}") String subscriptionKey,
            @Value("${azure.translator.region}") String region
    ) {
        this.restClient = restClientBuilder.baseUrl(endpoint).build();
        this.subscriptionKey = subscriptionKey;
        this.region = region;
    }

    // Azure returns translations in the same order as the input texts (response.get(i) corresponds to texts.get(i))
    public List<String> translate(List<String> texts, SourceLanguage sourceLanguage, TargetLanguage targetLanguage) {
        // Azure Translator expects a JSON array of text objects: [{"text": "Hello"}, {"text": "Good morning"}]
        List<TranslateRequestItem> requestBody = texts.stream()
                .map(TranslateRequestItem::new)
                .toList();

        // Azure returns a JSON array of translation objects:
        // [
        //   {"translations": [{"text": "Hallo", "to": "de"}]},
        //   {"translations": [{"text": "Guten Morgen", "to": "de"}]}
        // ]
        List<TranslateResponseItem> response;
        try {
            response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/translate")
                            .queryParam("api-version", "3.0")
                            .queryParam("from", sourceLanguage.code())
                            .queryParam("to", targetLanguage.code())
                            .build())
                    .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                    .header("Ocp-Apim-Subscription-Region", region)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<TranslateResponseItem>>() {});
        } catch (RestClientException e) {
            throw new TranslationException("Azure Translator request failed", e);
        }

        if (response == null || response.size() != texts.size()) {
            throw new TranslationException("Unexpected Azure Translator response shape");
        }

        // Only one target language is requested, so we need only the first translation
        return response.stream()
                .map(item -> {
                    if (item.translations() == null || item.translations().isEmpty()) {
                        throw new TranslationException("Azure Translator returned no translation");
                    }
                    return item.translations().get(0).text();
                })
                .toList();
    }

    private record TranslateRequestItem(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TranslateResponseItem(List<TranslatedText> translations) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TranslatedText(String text) {
    }
}