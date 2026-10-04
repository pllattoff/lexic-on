package com.lexicon.backend.service;

import com.lexicon.backend.enums.SourceLanguage;
import com.lexicon.backend.enums.TargetLanguage;
import com.lexicon.backend.exception.TranslationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// Endpoint, key and region come from src/test/resources/application.properties
@RestClientTest(AzureTranslatorClient.class)
class AzureTranslatorClientTest {

    @Autowired
    private AzureTranslatorClient azureTranslatorClient;

    @Autowired
    private MockRestServiceServer mockRestServiceServer;

    @Test
    void translate_sendsBatchRequestWithCredentials_whenGivenTexts() {
        // GIVEN
        mockRestServiceServer
                .expect(method(HttpMethod.POST))
                .andExpect(requestTo("http://localhost/translate?api-version=3.0&from=en&to=de"))
                .andExpect(header("Ocp-Apim-Subscription-Key", "test-key"))
                .andExpect(header("Ocp-Apim-Subscription-Region", "test-region"))
                .andExpect(content().json("""
                        [{"text": "hello"}, {"text": "world"}]
                        """))
                .andRespond(withSuccess("""
                        [
                          {"translations": [{"text": "hallo", "to": "de"}]},
                          {"translations": [{"text": "Welt", "to": "de"}]}
                        ]
                        """, MediaType.APPLICATION_JSON));

        // WHEN
        azureTranslatorClient.translate(List.of("hello", "world"), SourceLanguage.EN, TargetLanguage.DE);

        // THEN the request carries the texts, the language pair and the credentials
        mockRestServiceServer.verify();
    }

    @Test
    void translate_returnsTranslationsInInputOrder_whenResponseIsValid() {
        // GIVEN
        mockRestServiceServer
                .expect(requestTo("http://localhost/translate?api-version=3.0&from=en&to=uk"))
                .andRespond(withSuccess("""
                        [
                          {"translations": [{"text": "привіт", "to": "uk"}]},
                          {"translations": [{"text": "світ", "to": "uk"}]}
                        ]
                        """, MediaType.APPLICATION_JSON));

        // WHEN
        List<String> translations = azureTranslatorClient.translate(List.of("hello", "world"), SourceLanguage.EN, TargetLanguage.UK);

        // THEN
        assertThat(translations).containsExactly("привіт", "світ");
    }

    @Test
    void translate_throwsTranslationException_whenAzureReturnsServerError() {
        // GIVEN
        mockRestServiceServer
                .expect(requestTo("http://localhost/translate?api-version=3.0&from=en&to=de"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // WHEN
        assertThatThrownBy(() -> azureTranslatorClient.translate(List.of("hello"), SourceLanguage.EN, TargetLanguage.DE))
        // THEN the HTTP failure is wrapped, keeping the original cause
                .isInstanceOf(TranslationException.class)
                .hasMessage("Azure Translator request failed")
                .hasCauseInstanceOf(RestClientException.class);
    }

    @Test
    void translate_throwsTranslationException_whenResponseSizeDiffersFromInput() {
        // GIVEN a response with one item for two requested texts
        mockRestServiceServer
                .expect(requestTo("http://localhost/translate?api-version=3.0&from=en&to=de"))
                .andRespond(withSuccess("""
                        [{"translations": [{"text": "hallo", "to": "de"}]}]
                        """, MediaType.APPLICATION_JSON));

        // WHEN
        assertThatThrownBy(() -> azureTranslatorClient.translate(List.of("hello", "world"), SourceLanguage.EN, TargetLanguage.DE))
        // THEN
                .isInstanceOf(TranslationException.class)
                .hasMessage("Unexpected Azure Translator response shape");
    }

    @Test
    void translate_throwsTranslationException_whenResponseBodyIsNull() {
        // GIVEN
        mockRestServiceServer
                .expect(requestTo("http://localhost/translate?api-version=3.0&from=en&to=de"))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

        // WHEN
        assertThatThrownBy(() -> azureTranslatorClient.translate(List.of("hello"), SourceLanguage.EN, TargetLanguage.DE))
        // THEN
                .isInstanceOf(TranslationException.class)
                .hasMessage("Unexpected Azure Translator response shape");
    }

    @Test
    void translate_throwsTranslationException_whenResponseItemHasNullTranslations() {
        // GIVEN a response item without the "translations" field
        mockRestServiceServer
                .expect(requestTo("http://localhost/translate?api-version=3.0&from=en&to=de"))
                .andRespond(withSuccess("""
                        [{}]
                        """, MediaType.APPLICATION_JSON));

        // WHEN
        assertThatThrownBy(() -> azureTranslatorClient.translate(List.of("hello"), SourceLanguage.EN, TargetLanguage.DE))
        // THEN
                .isInstanceOf(TranslationException.class)
                .hasMessage("Azure Translator returned no translation");
    }

    @Test
    void translate_throwsTranslationException_whenResponseItemHasNoTranslations() {
        // GIVEN
        mockRestServiceServer
                .expect(requestTo("http://localhost/translate?api-version=3.0&from=en&to=de"))
                .andRespond(withSuccess("""
                        [{"translations": []}]
                        """, MediaType.APPLICATION_JSON));

        // WHEN
        assertThatThrownBy(() -> azureTranslatorClient.translate(List.of("hello"), SourceLanguage.EN, TargetLanguage.DE))
        // THEN
                .isInstanceOf(TranslationException.class)
                .hasMessage("Azure Translator returned no translation");
    }
}
