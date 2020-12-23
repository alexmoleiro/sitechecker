package com.alexmoleiro.healthchecker.infrastructure.api;

import com.alexmoleiro.healthchecker.core.healthCheck.Endpoint;
import com.alexmoleiro.healthchecker.core.healthCheck.HealthCheckRepository;
import com.alexmoleiro.healthchecker.core.healthCheck.HealthCheckResponse;
import com.alexmoleiro.healthchecker.core.profile.OauthService;
import com.alexmoleiro.healthchecker.core.profile.ProfileRepository;
import com.alexmoleiro.healthchecker.core.profile.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Set;

import static java.time.LocalDateTime.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileApiTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  HealthCheckRepository healthCheckRepository;

  @MockBean
  OauthService oauthService;

  @Autowired
  ProfileRepository profileRepository;

  @Test
  void shouldAddDomain() throws Exception {

    final User user = new User("endpoint", "alex@email.com");
    when(oauthService.getUser(anyString())).thenReturn(user);
    String aToken = "aToken";
    final String validUrl = "https://www.as.com";

    this.mockMvc.perform(
        post("/profile/addurl")
            .header("Token", aToken)
            .contentType(APPLICATION_JSON)
    .content("""
        {"url":"%s"}""".formatted(validUrl)))
        .andExpect(status().isCreated());

    assertThat(profileRepository.get(user).getFollowing())
        .isEqualTo(Set.of(new Endpoint("https://www.as.com")));
    }

  @Test
  void shouldReturn404whenInvalidDomain() throws Exception {
    String aToken = "aToken";
    this.mockMvc.perform(
        post("/profile/addurl")
            .header("Token", aToken)
            .contentType(APPLICATION_JSON)
            .content("""
        {"url":"invalidUrl"}"""))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnForbiddenWhenInvalidTokenTryingToAddAUrl() throws Exception {
    doThrow(new InvalidTokenException(new Exception()))
        .when(oauthService).getUser(anyString());

    this.mockMvc.perform(post("/profile/addurl")
        .header("Token", "")
        .contentType(APPLICATION_JSON)
        .content("""
        {"url":"https://www.as.com"}"""))
        .andExpect(status().isForbidden());
    }

  @Test
  void shouldReturnForbiddenWhenInvalidToken() throws Exception {
    doThrow(new InvalidTokenException(new Exception()))
        .when(oauthService).getUser(anyString());

    this.mockMvc.perform(get("/profile")
        .header("Token", ""))
        .andExpect(status().isForbidden());
    }

  @Test
  void shouldRespondFollowebWebsites() throws Exception {

    final String anId = "endpoint";
    final String anEmail = "alex@email.com";
    final String aToken = "anything";
    LocalDateTime time = of(2020, 11, 30, 12, 00);
    final User aUSer = new User(anId, anEmail);
    when(oauthService.getUser(aToken)).thenReturn(aUSer);

    profileRepository.addUrl(aUSer, new Endpoint("https://amazon.com"));
    profileRepository.addUrl(aUSer, new Endpoint("sport.it"));
    profileRepository.addUrl(aUSer, new Endpoint("joindrover.com"));

    healthCheckRepository.add(new Endpoint("https://amazon.com"), new HealthCheckResponse(new URL("https://amazon.com"), 200,
        time.minusMinutes(1), time ));
    healthCheckRepository.add(new Endpoint("https://amazon.com"), new HealthCheckResponse(new URL("https://amazon.com"), 200,
        time.minusMinutes(1), time ));

    healthCheckRepository.add(new Endpoint("sport.it"), new HealthCheckResponse(new URL("https://sport.it"), 200,
        time.minusMinutes(1), time ));
    healthCheckRepository.add(new Endpoint("sport.it"), new HealthCheckResponse(new URL("https://sport.it"), 200,
        time.minusMinutes(1), time ));

    healthCheckRepository.add(new Endpoint("joindrover.com"), new HealthCheckResponse(new URL("https://joindrover.com"), 200,
        time.minusMinutes(1), time ));
    healthCheckRepository.add(new Endpoint("joindrover.com"), new HealthCheckResponse(new URL("https://joindrover.com"), 200,
        time.minusMinutes(1), time ));

    this.mockMvc.perform(get("/profile").header("Token", aToken))
        .andExpect(status().isOk())
        .andExpect(content().json("""              
              {"responses":[
              {"endpoint":{"url":"https://amazon.com"},
              "healthCheckResponse":[
              {"time":"2020-11-30T12:00:00","url":"https://amazon.com","delay":60000,"status":200},
              {"time":"2020-11-30T12:00:00","url":"https://amazon.com","delay":60000,"status":200}]}
              ,{"endpoint":{"url":"sport.it"},
              "healthCheckResponse":[
              {"time":"2020-11-30T12:00:00","url":"https://sport.it","delay":60000,"status":200},
              {"time":"2020-11-30T12:00:00","url":"https://sport.it","delay":60000,"status":200}]},
              {"endpoint":{"url":"joindrover.com"},
              "healthCheckResponse":[
              {"time":"2020-11-30T12:00:00","url":"https://joindrover.com","delay":60000,"status":200},
              {"time":"2020-11-30T12:00:00","url":"https://joindrover.com","delay":60000,"status":200}]}],
              "userId":"endpoint"}
              """));
  }
}