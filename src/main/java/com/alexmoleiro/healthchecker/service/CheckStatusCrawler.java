package com.alexmoleiro.healthchecker.service;

import com.alexmoleiro.healthchecker.core.WebStatusRequest;
import com.alexmoleiro.healthchecker.infrastructure.SiteCheckerResponse;
import com.alexmoleiro.healthchecker.infrastructure.WebStatusRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentLinkedDeque;

import static java.time.Clock.systemUTC;
import static java.time.LocalDateTime.now;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.IntStream.rangeClosed;

public class CheckStatusCrawler {
  private static final Logger LOGGER = LoggerFactory.getLogger(CheckStatusCrawler.class);
  private final HttpChecker httpChecker;
  private final int nThreads;

  public CheckStatusCrawler(HttpChecker httpChecker, int nThreads) {
    this.httpChecker = httpChecker;
    this.nThreads = nThreads;
  }

  public void run(ConcurrentLinkedDeque<String> domains) {
    rangeClosed(1, nThreads).forEach(x -> runAsync(() -> getCheck(domains, now(systemUTC()))));
  }

  private void getCheck(ConcurrentLinkedDeque<String> domains, LocalDateTime now) {

    while (domains.peek() != null) {
      final SiteCheckerResponse check =
          httpChecker.check(new WebStatusRequest(new WebStatusRequestDto(domains.poll())));
      LOGGER.info(check.toString());
    }
  }
}