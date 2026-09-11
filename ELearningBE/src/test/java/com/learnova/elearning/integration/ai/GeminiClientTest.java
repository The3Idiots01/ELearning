package com.learnova.elearning.integration.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiClientTest {

    @Test
    void parsesStructuredJsonFromLaterResponsePart() {
        AiProperties properties = new AiProperties();
        properties.setApiKey("test-key");
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiClient client = new GeminiClient(properties, new ObjectMapper(), builder.build());
        server.expect(requestTo(properties.getBaseUrl() + "/models/gemini-3.8-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(content().string(containsString("APPLICATION_JSON")))
                .andExpect(content().string(containsString("thinkingLevel")))
                .andRespond(withSuccess("""
                        {
                          "candidates": [{
                            "content": {"parts": [
                              {"text": "", "thoughtSignature": "signature"},
                              {"text": "{\\\"text\\\":\\\"Xin chào\\\"}"}
                            ]},
                            "finishReason": "STOP"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of("text", Map.of("type", "string")),
                "required", List.of("text"));

        Greeting result = client.generateStructured("Say hello", schema, Greeting.class);

        assertThat(result.text()).isEqualTo("Xin chào");
        server.verify();
    }

    private record Greeting(String text) {
    }
}
