package com.glamaya.sync.core.domain.model;

public enum ExecutionMode {
    /**
     * Run platforms sequentially; within each platform, run processors sequentially.
     */
    SEQUENTIAL("sequential"),
    /**
     * Run platforms sequentially; within each platform, run processors in parallel with a cap.
     */
    PLATFORM_PARALLEL("platform-parallel"),
    /**
     * Run all processors across platforms in parallel with a cap.
     */
    PARALLEL("parallel");

    private final String value;

    ExecutionMode(String value) {
        this.value = value;
    }

    public static ExecutionMode fromString(String value) {
        for (ExecutionMode mode : values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Invalid execution mode: " + value);
    }
}

