package com.shopflow.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    private ErrorBody error;
    private Instant timestamp;
    private String path;

    public ApiErrorResponse() {
        this.timestamp = Instant.now();
    }

    public ApiErrorResponse(String code, String message, String path) {
        this.error = new ErrorBody(code, message, null);
        this.timestamp = Instant.now();
        this.path = path;
    }

    public ApiErrorResponse(String code, String message, Map<String, String> details, String path) {
        this.error = new ErrorBody(code, message, details);
        this.timestamp = Instant.now();
        this.path = path;
    }

    public ErrorBody getError() { return error; }
    public void setError(ErrorBody error) { this.error = error; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorBody {
        private String code;
        private String message;
        private Map<String, String> details;

        public ErrorBody() {}

        public ErrorBody(String code, String message, Map<String, String> details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Map<String, String> getDetails() { return details; }
        public void setDetails(Map<String, String> details) { this.details = details; }
    }
}
