package com.glamaya.sync.runner.common;

public final class LoggerConstants {
    private LoggerConstants() {}

    // --- Scheduler ---
    public static final String SCHEDULER_INIT = "Scheduler initialized. platformCount={}, maxConcurrency={}, executionMode={}";
    public static final String SCHEDULER_START = "Scheduler starting scheduled sync. executionMode={}, maxConcurrency={}";
    public static final String SCHEDULER_ALL_COMPLETE = "Scheduler all platforms sync completed. executionMode={}";
    public static final String SCHEDULER_PLATFORM_START = "{}: platform sync started. maxConcurrency={}, executionMode={}";
    public static final String SCHEDULER_PLATFORM_COMPLETE = "{}: platform sync completed. maxConcurrency={}, executionMode={}";

    // --- Notification ---
    public static final String NOTIF_COMPOSITE_INIT = "CompositeNotificationAdapter: Initialized with {} leaf notifiers.";
    public static final String NOTIF_KAFKA_SEND = "KafkaNotificationAdapter: Sending payload to Kafka topic='{}'";
}

