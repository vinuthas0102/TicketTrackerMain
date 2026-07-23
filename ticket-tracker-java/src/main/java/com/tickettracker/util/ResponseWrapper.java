package com.tickettracker.util;

import java.util.Map;

public class ResponseWrapper<T> {
    private boolean success;
    private T data;
    private String message;
    private ErrorDetails error;

    public ResponseWrapper() {}

    public ResponseWrapper(boolean success, T data, String message, ErrorDetails error) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.error = error;
    }

    public static <T> ResponseWrapper<T> success(T data) {
        return new ResponseWrapper<>(true, data, null, null);
    }

    public static <T> ResponseWrapper<T> success(T data, String message) {
        return new ResponseWrapper<>(true, data, message, null);
    }

    public static <T> ResponseWrapper<T> success() {
        return new ResponseWrapper<>(true, null, null, null);
    }

    public static <T> ResponseWrapper<T> error(String code, String message) {
        return new ResponseWrapper<>(false, null, null, new ErrorDetails(code, message, null));
    }

    public static <T> ResponseWrapper<T> error(String code, String message, Map<String, String> validationErrors) {
        return new ResponseWrapper<>(false, null, null, new ErrorDetails(code, message, validationErrors));
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public ErrorDetails getError() { return error; }
    public void setError(ErrorDetails error) { this.error = error; }

    public static class ErrorDetails {
        private String code;
        private String message;
        private Map<String, String> validationErrors;

        public ErrorDetails() {}

        public ErrorDetails(String code, String message, Map<String, String> validationErrors) {
            this.code = code;
            this.message = message;
            this.validationErrors = validationErrors;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Map<String, String> getValidationErrors() { return validationErrors; }
        public void setValidationErrors(Map<String, String> validationErrors) { this.validationErrors = validationErrors; }
    }
}
