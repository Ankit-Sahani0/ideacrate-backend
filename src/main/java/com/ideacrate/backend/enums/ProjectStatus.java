package com.ideacrate.backend.enums;

public enum ProjectStatus {
    DRAFT,
    SUBMITTED,
    PENDING, // kept for backward compatibility with older rows
    APPROVED,
    REJECTED,
}
