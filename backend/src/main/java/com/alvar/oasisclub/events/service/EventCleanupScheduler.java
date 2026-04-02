package com.alvar.oasisclub.events.service;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class EventCleanupScheduler {

  private static final Logger log = LoggerFactory.getLogger(EventCleanupScheduler.class);

  private final EventService eventService;

  @Scheduled(cron = "0 0 3 * * *")
  public void cleanupPastEvents() {
    log.info("Running scheduled cleanup of past events...");
    eventService.cleanupPastEvents();
  }
}
