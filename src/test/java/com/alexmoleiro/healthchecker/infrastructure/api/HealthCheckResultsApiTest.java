package com.alexmoleiro.healthchecker.infrastructure.api;

import com.alexmoleiro.healthchecker.core.healthCheck.Endpoint;
import com.alexmoleiro.healthchecker.core.healthCheck.HealthCheckRepository;
import com.alexmoleiro.healthchecker.core.healthCheck.HealthCheckResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URL;
import java.time.LocalDateTime;

import static java.time.LocalDateTime.now;
import static java.time.LocalDateTime.of;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class HealthCheckResultsApiTest {

  public static final String URL_STRING = "https://www.a.com";
  @Autowired
  MockMvc mockMvc;

  @Autowired
  HealthCheckRepository repository;

  @Test
  void shouldReturnHistoricValues() throws Exception {

    final LocalDateTime first = of(2020, 12, 8, 23, 20);
    final LocalDateTime second = of(2020, 12, 8, 23, 25);

    repository.add(
        new Endpoint("www.a.com"),
        new HealthCheckResponse(new URL(URL_STRING), OK.value(), first.minusHours(1), first)
    );

    repository.add(
        new Endpoint("www.a.com"),
        new HealthCheckResponse(new URL(URL_STRING), INTERNAL_SERVER_ERROR.value(), second.minusHours(1), second)
    );

    this.mockMvc.perform(get("/historical/www.a.com"))
        .andExpect(status().isOk())
        .andExpect(content().json("""
              [
              {"url":"https://www.a.com","delay":3600000,"status":200,"time":"2020-12-08T23:20"},
              {"url":"https://www.a.com","delay":3600000,"status":500,"time":"2020-12-08T23:25"}
              ]"""));
    }

    //TODO flaky tests
  @Test
  void shouldReturnLandingListSites() throws Exception {
    repository.add(
        new Endpoint("www.a.com"), new HealthCheckResponse(new URL("https://www.a.com"), OK.value(),
        now().minusHours(1), now()));
    repository.add(new Endpoint("www.b.com"),
        new HealthCheckResponse(new URL("https://www.b.com"), INTERNAL_SERVER_ERROR.value(),
        now().minusHours(1), now()
    ));

      this.mockMvc.perform(post("/landing-list"))
          .andExpect(status().isOk())
          .andExpect(content().json("""
              {"responses":[
              {"url":"https://www.a.com","delay":3600000,"status":200,"id":"www.a.com"},
              {"url":"https://www.b.com","delay":3600000,"status":500,"id":"www.b.com"}
              ],
              "numUrls":2}"""));
  }
}