package org.dsqrwym.langora.infrastructure.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Container level errors forward and bypass @RestControllerAdvice
 */
@RestController
public class JsonErrorController implements ErrorController {
    @RequestMapping("/error")
    public ResponseEntity<ApiResponse<Void>> onError(HttpServletRequest request) {
        Object attr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int raw = (attr instanceof Integer i) ? i : HttpStatus.INTERNAL_SERVER_ERROR.value();

        HttpStatus status = HttpStatus.resolve(raw);

        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

        return ResponseEntity.status(status).body(
                ApiResponse.error(status, "HTTP_" + status.value(), ApiResponse.MSG_REQUEST_FAILED, null));
    }
}
