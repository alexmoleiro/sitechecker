package com.alexmoleiro.healthchecker.core;

import java.util.List;

public interface HealthCheckResultsRepository {

  List<TimedHealthCheckResponses> getTimedResults();
  TimedHealthCheckResponses getResponses(Id id);
  void add(Id id, HealthCheckResponse response);
}