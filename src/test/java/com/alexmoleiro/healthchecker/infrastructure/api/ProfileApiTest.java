package com.alexmoleiro.healthchecker.infrastructure.api;


import com.alexmoleiro.healthchecker.core.healthCheck.Endpoint;
import com.alexmoleiro.healthchecker.core.healthCheck.EndpointRepository;
import com.alexmoleiro.healthchecker.core.healthCheck.HealthCheckRepository;
import com.alexmoleiro.healthchecker.core.healthCheck.HealthCheckResponse;
import com.alexmoleiro.healthchecker.core.healthCheck.HttpUrl;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.alexmoleiro.healthchecker.core.healthCheck.CheckResultCode.MAXIMUM_ENDPOINT_PER_USER_EXCEEDED;
import static java.time.LocalDateTime.of;
import static java.time.Month.NOVEMBER;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;
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

  @Autowired
  EndpointRepository endpointRepository;

  @MockBean
  OauthService oauthService;

  @Autowired
  ProfileRepository profileRepository;

  private static final String A_TOKEN = UUID.randomUUID().toString();

  @Test
  void shouldAddDomain() throws Exception {
 User user = createUser();
    when(oauthService.getUser(anyString())).thenReturn(user);
    String aToken = "aToken";
    final String validUrl = "https://www.c.com";

    this.mockMvc.perform(
        post("/profile/addurl")
            .header("Token", aToken)
            .contentType(APPLICATION_JSON)
    .content("""
        {"url":"%s"}""".formatted(validUrl)))
        .andExpect(status().isCreated());

    assertThat(profileRepository.get(user).get().getFollowing()).usingRecursiveComparison()
        .isEqualTo(Set.of(new Endpoint(new HttpUrl("https://www.c.com"))));
    }

  @Test
  void shouldReturn701() throws Exception {

    when(oauthService.getUser(anyString())).thenReturn(createUser());
    String aToken = "aToken";
    final String validUrl = "https://www.as.com";

    this.mockMvc.perform(
        post("/profile/addurl")
            .header("Token", aToken)
            .contentType(APPLICATION_JSON)
            .content("""
        {"url":"%s"}""".formatted(validUrl)))
        .andExpect(status().isCreated());

    this.mockMvc.perform(
        post("/profile/addurl")
            .header("Token", aToken)
            .contentType(APPLICATION_JSON)
            .content("""
        {"url":"%s"}""".formatted(validUrl)))
        .andExpect(status().is(MAXIMUM_ENDPOINT_PER_USER_EXCEEDED.value()));

  }

  @Test
  void shouldReturn404whenInvalidDomain() throws Exception {
    this.mockMvc.perform(
        post("/profile/addurl")
            .header("Token", A_TOKEN)
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
  void deleteUrl() throws Exception {
    User user = createUser();
    when(oauthService.getUser(anyString())).thenReturn(user);
    Endpoint endpointA = new Endpoint(new HttpUrl("a.com"));

    profileRepository.addEndpoint(user, endpointA);
    endpointRepository.add(endpointA);

    assertThat(profileRepository.get(user).get().getFollowing()).containsOnly(endpointA);

    this.mockMvc.perform(
            MockMvcRequestBuilders.delete("/profile/deleteurls")
                    .header("Token", A_TOKEN)
                    .contentType(APPLICATION_JSON)
                    .content("""
        {"ids":["%s"]}""".formatted(endpointA.getId())))
            .andExpect(status().isOk());

    assertThat(profileRepository.get(user).get().getFollowing()).isEmpty();

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

    final String aToken = randomString();
    LocalDateTime time = of(2020, NOVEMBER.getValue(), 30, 12, 0);
    final User aUser = createUser();

    when(oauthService.getUser(aToken)).thenReturn(aUser);

    Endpoint endpointA = new Endpoint(new HttpUrl("a.com"));
    Endpoint endPointB = new Endpoint(new HttpUrl("b.it"));
    Endpoint endpointC = new Endpoint(new HttpUrl("c.es"));

    List.of(endpointA, endPointB, endpointC).forEach(e->
    {
      profileRepository.addEndpoint(aUser,e);
      healthCheckRepository.add(e, new HealthCheckResponse(e.getHttpUrl(), OK.value(), time.minusMinutes(2), time ));
      healthCheckRepository.add(e, new HealthCheckResponse(e.getHttpUrl(), FORBIDDEN.value(), time.minusMinutes(1), time ));
    });


    this.mockMvc.perform(get("/profile").header("Token", aToken))
        .andExpect(status().isOk())
        .andExpect(content().json("""              
              {"responses":[
              {"endpoint":{"id":"%s","url":"%s","group":"%s"},"uptime":50.0,"average":90000.0,
              "healthCheckResponse":[
              {"time":"2020-11-30T12:00:00","url":"http://a.com","delay":120000,"status":200},
              {"time":"2020-11-30T12:00:00","url":"http://a.com","delay":60000,"status":403}]},
              {"endpoint":{"id":"%s","url":"%s","group":"%s"},"uptime":50.0,"average":90000.0,
              "healthCheckResponse":[
              {"time":"2020-11-30T12:00:00","url":"http://b.it","delay":120000,"status":200},
              {"time":"2020-11-30T12:00:00","url":"http://b.it","delay":60000,"status":403}]},
              {"endpoint":{"id":"%s","url":"%s","group":"%s"},"uptime":50.0,"average":90000.0,
              "healthCheckResponse":[
              {"time":"2020-11-30T12:00:00","url":"http://c.es","delay":120000,"status":200},
              {"time":"2020-11-30T12:00:00","url":"http://c.es","delay":60000,"status":403}]}],
              "userId":"%s"}
              """.formatted(
                      endpointA.getId(), endpointA.getHttpUrl().toString(), endpointA.getGroup(),
                      endPointB.getId(), endPointB.getHttpUrl().toString(), endPointB.getGroup(),
                endpointC.getId(),  endpointC.getHttpUrl().toString(), endpointC.getGroup(),
                aUser.getId()
        )));
  }

  private User createUser() {
    return new User(randomUUID().toString(), "alex@email.com");
  }
  private String randomString() {
    return randomUUID().toString();
  }
}

