package builder.good;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * GOOD: Builder pattern solves the telescoping constructor problem.
 *
 * HttpRequest is immutable — all fields are final.
 * The only way to construct it is through HttpRequest.builder().
 *
 * Advantages:
 * 1. Self-documenting: method("GET").url("...").timeoutSeconds(30) is readable
 * 2. Optional parameters handled cleanly (don't need to pass nulls)
 * 3. Immutable object — thread-safe by default
 * 4. Build-time validation in build() method
 * 5. Easy to add new optional fields without breaking existing callers
 *
 * In production Java, use Lombok @Builder annotation to generate this automatically.
 * In interviews, implement manually to demonstrate understanding.
 */
public class HttpRequest {

    // All fields immutable
    private final String method;
    private final String url;
    private final int timeoutSeconds;
    private final int retries;
    private final boolean followRedirects;
    private final String body;
    private final String authToken;
    private final Map<String, String> headers;

    // Private constructor — only Builder can call this
    private HttpRequest(Builder builder) {
        this.method = builder.method;
        this.url = builder.url;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.retries = builder.retries;
        this.followRedirects = builder.followRedirects;
        this.body = builder.body;
        this.authToken = builder.authToken;
        this.headers = Collections.unmodifiableMap(new HashMap<>(builder.headers));
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getMethod()         { return method; }
    public String getUrl()            { return url; }
    public int getTimeoutSeconds()    { return timeoutSeconds; }
    public int getRetries()           { return retries; }
    public boolean isFollowRedirects(){ return followRedirects; }
    public String getBody()           { return body; }
    public String getAuthToken()      { return authToken; }
    public Map<String, String> getHeaders() { return headers; }

    @Override
    public String toString() {
        return String.format("HttpRequest{method='%s', url='%s', timeout=%ds, retries=%d, followRedirects=%s, body=%s, auth=%s}",
            method, url, timeoutSeconds, retries, followRedirects,
            body != null ? "present" : "null",
            authToken != null ? "present" : "null");
    }

    // --- Static inner Builder class ---

    public static class Builder {
        // Required
        private String method;
        private String url;

        // Optional with sensible defaults
        private int timeoutSeconds = 30;
        private int retries = 0;
        private boolean followRedirects = true;
        private String body = null;
        private String authToken = null;
        private Map<String, String> headers = new HashMap<>();

        private Builder() {}

        public Builder method(String method) {
            this.method = method;
            return this;  // Fluent interface — enables method chaining
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder timeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Builder retries(int retries) {
            this.retries = retries;
            return this;
        }

        public Builder followRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder authToken(String authToken) {
            this.authToken = authToken;
            return this;
        }

        public Builder header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        /**
         * Validate and build the immutable HttpRequest.
         * Business validation goes here — NOT in setters.
         */
        public HttpRequest build() {
            // Validate required fields
            if (method == null || method.isBlank()) {
                throw new IllegalStateException("HTTP method is required");
            }
            if (url == null || url.isBlank()) {
                throw new IllegalStateException("URL is required");
            }

            // Validate values
            if (timeoutSeconds <= 0) {
                throw new IllegalStateException("Timeout must be positive");
            }
            if (retries < 0) {
                throw new IllegalStateException("Retries cannot be negative");
            }

            return new HttpRequest(this);
        }
    }
}
