package org.dsqrwym.langora.infrastructure.web.dto;

import org.dsqrwym.langora.infrastructure.web.ErrorCode;

public record ParamErrorDetail(String param, ErrorCode code) {
}
