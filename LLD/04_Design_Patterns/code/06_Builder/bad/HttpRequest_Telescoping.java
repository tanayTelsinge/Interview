package builder.bad;

/**
 * VIOLATION: Telescoping constructor anti-pattern.
 *
 * Problems:
 * 1. Caller can't tell which argument is which without an IDE tooltip.
 * 2. Must pass null/defaults for optional params — error-prone.
 * 3. Adding a new optional field requires a new constructor overload.
 * 4. All these constructors are maintenance nightmares.
 *
 * Try reading: new HttpRequest_Telescoping("GET", "http://api.com", 30, 3, true, null, "Bearer xyz")
 * Which position is timeout? Which is retries? Is position 6 the body or a header?
 */
public class HttpRequest_Telescoping {

    private final String method;
    private final String url;
    private final int timeoutSeconds;
    private final int retries;
    private final boolean followRedirects;
    private final String body;
    private final String authToken;

    // Minimal constructor
    public HttpRequest_Telescoping(String method, String url) {
        this(method, url, 30, 0, true, null, null);
    }

    // With timeout
    public HttpRequest_Telescoping(String method, String url, int timeoutSeconds) {
        this(method, url, timeoutSeconds, 0, true, null, null);
    }

    // With timeout + retries
    public HttpRequest_Telescoping(String method, String url, int timeoutSeconds, int retries) {
        this(method, url, timeoutSeconds, retries, true, null, null);
    }

    // All params — impossible to read at call site
    public HttpRequest_Telescoping(String method, String url, int timeoutSeconds,
                                    int retries, boolean followRedirects,
                                    String body, String authToken) {
        this.method = method;
        this.url = url;
        this.timeoutSeconds = timeoutSeconds;
        this.retries = retries;
        this.followRedirects = followRedirects;
        this.body = body;
        this.authToken = authToken;
    }

    // BAD usage:
    // HttpRequest_Telescoping r = new HttpRequest_Telescoping("POST", "http://api.com", 30, 3, false, "{}", "Bearer xyz");
    // Which arg is timeout? retries? No clue without checking constructor signature.
}
