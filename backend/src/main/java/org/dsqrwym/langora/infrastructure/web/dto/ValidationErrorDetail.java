package org.dsqrwym.langora.infrastructure.web.dto;

import org.dsqrwym.langora.infrastructure.web.ErrorCode;

public record ValidationErrorDetail(String field, ErrorCode code, String message) {}
