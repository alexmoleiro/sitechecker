package com.alexmoleiro.healthchecker.infrastructure.dto;

import com.alexmoleiro.healthchecker.core.healthCheck.HealthCheckResponses;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class HistoricResultsDto {

    private String url;
    private long delay;
    private long status;
    private LocalDateTime time;


    private HistoricResultsDto(String url, long delay, long status, LocalDateTime time) {
        this.url = url;
        this.delay = delay;
        this.status = status;
        this.time = time;
    }

    public static List<HistoricResultsDto> list(HealthCheckResponses responses) {
        if (responses == null) {
            return emptyList();
        }
        return responses.getHealthCheckResponse().stream()
                .map(
                    response ->
                            new HistoricResultsDto(
                                    response.getUrl(),
                                    response.getDelay(),
                                    response.getStatus(),
                                    response.getTime()))
                .collect(toList());
    }

    public String getUrl() {
        return url;
    }

    public long getDelay() {
        return delay;
    }

    public long getStatus() {
        return status;
    }

    public String getTime() {
        return time.toString();
    }
}
