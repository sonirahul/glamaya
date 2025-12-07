package com.glamaya.sync.core.common;

public final class LoggerConstants {
    private LoggerConstants() {}

    // --- Orchestrator ---
    public static final String ORCH_SYNC = "{}: triggering sync. processorCount={}, concurrency={}";
    public static final String ORCH_REGISTERED = "{}: registered for orchestration.";
    public static final String ORCH_START_EXEC = "{}: starting executeSync.";
    public static final String ORCH_COMPLETE_EXEC = "{}: sync completed.";
    public static final String ORCH_EXEC_INVOKED = "{}: executeSync invoked.";
    public static final String ORCH_SYNC_DISABLED = "{}: sync disabled.";
    public static final String ORCH_SYNC_COMPLETED = "{}: sync completed. totalItems={}";
    public static final String ORCH_FETCH_PAGE = "{}: fetching data. page={}, pageSize={}";
    public static final String ORCH_NOT_CONFIGURED = "{}: not configured for sync.";
}
