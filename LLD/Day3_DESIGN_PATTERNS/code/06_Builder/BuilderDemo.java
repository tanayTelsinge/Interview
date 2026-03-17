package builder;

import builder.good.HttpRequest;

/**
 * Demo: Builder Pattern
 */
public class BuilderDemo {

    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println("   BUILDER PATTERN DEMO");
        System.out.println("========================================\n");

        // --- Minimal build (only required params) ---
        System.out.println("--- Minimal request ---");
        HttpRequest getRequest = HttpRequest.builder()
            .method("GET")
            .url("https://api.example.com/users")
            .build();
        System.out.println(getRequest);

        // --- Full build (all optional params) ---
        System.out.println("\n--- Full request ---");
        HttpRequest postRequest = HttpRequest.builder()
            .method("POST")
            .url("https://api.example.com/orders")
            .timeoutSeconds(45)
            .retries(3)
            .followRedirects(false)
            .body("{\"item\": \"book\", \"qty\": 2}")
            .authToken("Bearer eyJhbGci...")
            .header("Content-Type", "application/json")
            .header("X-Request-ID", "req-123")
            .build();
        System.out.println(postRequest);

        // --- Validation error ---
        System.out.println("\n--- Validation error ---");
        try {
            HttpRequest invalid = HttpRequest.builder()
                .method("GET")
                .timeoutSeconds(-5)  // Invalid
                .build();
        } catch (IllegalStateException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        System.out.println("\n--- Interview takeaway ---");
        System.out.println("Self-documenting construction: no positional arg confusion.");
        System.out.println("Immutable result: thread-safe, no accidental mutation.");
        System.out.println("In prod: Lombok @Builder generates this boilerplate.");
    }
}
