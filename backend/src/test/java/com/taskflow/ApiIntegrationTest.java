package com.taskflow;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Integration-test base for endpoints that need signed-in users. Users are created through the
 * real registration endpoint, so every test exercises the real security filter chain.
 */
public abstract class ApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;

    /** A registered user and a valid access token for it. */
    public record TestUser(UUID id, String email, String displayName, String accessToken) {
    }

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    protected TestUser registerUser(String displayName) throws Exception {
        return registerUser(displayName, uniqueEmail());
    }

    protected TestUser registerUser(String displayName, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "s3cret-password", "displayName", displayName))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = body(result);
        return new TestUser(
                UUID.fromString(body.at("/user/id").asText()), email, displayName, body.get("accessToken").asText());
    }

    /** Performs the request as the given user. */
    protected ResultActions perform(TestUser user, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + user.accessToken()));
    }

    /** Performs a request with a JSON body as the given user. */
    protected ResultActions perform(TestUser user, MockHttpServletRequestBuilder request, Object jsonBody)
            throws Exception {
        return perform(user, request.contentType(MediaType.APPLICATION_JSON).content(json(jsonBody)));
    }

    protected JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected JsonNode body(ResultActions actions) throws Exception {
        return body(actions.andReturn());
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
