# Authentication Interview Cheat Sheet

---

## Authentication Types Overview

```
External B2B API    → API Key
Normal User         → JWT + OAuth2 + OpenID Connect (Keycloak)
Machine to Machine  → OAuth2 Client Credentials
Legacy (Autodesk)   → OAuth1 → migrated to OAuth2
```

---

## 1. API Key Authentication

### What is it?
Long-lived key issued to external clients for B2B API access.

### Flow
```
External System
        ↓ X-API-Key: abc123xyz in header
API Gateway
        ↓
Extract X-API-Key
        ↓
Validate against Redis cache → DB
        ↓
Valid   → inject X-Client-Id header → forward
Invalid → 401 Unauthorized
```

### Gateway Filter
```java
@Component
public class ApiKeyFilter implements GlobalFilter {

    private final ApiKeyRepository apiKeyRepo;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();

        if (!path.startsWith("/api/external/")) {
            return chain.filter(exchange);
        }

        String apiKey = exchange.getRequest()
            .getHeaders()
            .getFirst("X-API-Key");

        if (apiKey == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Redis cache first — avoid DB hit every request
        String cachedClient = redisTemplate.opsForValue().get("apikey:" + apiKey);

        if (cachedClient != null) {
            return forwardWithClientId(exchange, chain, cachedClient);
        }

        // DB lookup
        ApiKeyEntity entity = apiKeyRepo.findByKey(apiKey);

        if (entity == null || !entity.isActive()) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Cache for 5 minutes
        redisTemplate.opsForValue().set(
            "apikey:" + apiKey,
            entity.getClientId(),
            Duration.ofMinutes(5)
        );

        return forwardWithClientId(exchange, chain, entity.getClientId());
    }

    private Mono<Void> forwardWithClientId(ServerWebExchange exchange,
            GatewayFilterChain chain, String clientId) {
        ServerHttpRequest mutated = exchange.getRequest()
            .mutate()
            .header("X-Client-Id", clientId)
            .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }
}
```

### API Key Entity
```java
@Entity
public class ApiKeyEntity {
    private String keyHash;          // hashed key — never store plain
    private String clientId;         // e.g. "partner-bank-abc"
    private String clientName;
    private boolean active;
    private LocalDate expiryDate;
    private List<String> allowedPaths;
}
```

### Why Redis Cache
```
Every request → DB lookup = slow + expensive
Every request → Redis = sub-millisecond

Cache TTL = 5 mins
Key revoked → wait max 5 mins
Immediate revocation → delete Redis key on revoke
```

### Interview Drill-down Q&A

**Q: How do you revoke an API key immediately?**
> "Delete the Redis cache entry for that key — `redisTemplate.delete("apikey:" + key)`. Next request will hit DB, find key inactive, return 401. Without cache deletion, revocation takes up to TTL duration (5 minutes)."

**Q: How do you store API keys securely?**
> "Never store plain text API keys. Store a hashed version (SHA-256) in DB. When validating, hash the incoming key and compare with stored hash — same as password hashing."

**Q: How do you handle API key rotation?**
> "Issue new key while keeping old key active for a grace period. Client migrates to new key, then old key is deactivated. This avoids breaking existing integrations."

---

## 2. JWT Authentication

### What is JWT?
Self-contained token — carries user info inside, no DB lookup needed for validation.

### JWT Structure
```
Header.Payload.Signature

Header:  { "alg": "RS256", "typ": "JWT" }

Payload: {
  "sub": "user-uuid-123",          // user ID
  "preferred_username": "tanay",
  "email": "tanay@maxxton.com",
  "roles": ["FINANCE_ANALYST"],
  "iat": 1700000000,               // issued at
  "exp": 1700003600                // expires (1 hour)
}

Signature: RSA-SHA256(base64(header) + "." + base64(payload), privateKey)
```

### JWT Validation at Gateway
```java
@Component
public class JwtAuthFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest()
            .getHeaders()
            .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(publicKey)  // validate with public key
                .build()
                .parseClaimsJws(token)
                .getBody();

            // Inject user info for downstream
            ServerHttpRequest mutated = exchange.getRequest()
                .mutate()
                .header("X-User-Id", claims.getSubject())
                .header("X-User-Role", claims.get("role", String.class))
                .header("X-Tenant-Id", claims.get("tenantId", String.class))
                .build();

            return chain.filter(exchange.mutate().request(mutated).build());

        } catch (ExpiredJwtException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        } catch (JwtException e) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
    }
}
```

### JWT vs Session Token

| | JWT | Session Token |
|--|-----|--------------|
| Storage | Client side | Server side (DB/Redis) |
| Stateless | Yes — no DB lookup | No — needs session store |
| Scalability | High | Lower |
| Revocation | Hard — needs blacklist | Easy — delete session |
| Size | Larger | Small (random ID) |

### Interview Drill-down Q&A

**Q: How do you handle JWT revocation?**
> "JWT is stateless — once issued, valid until expiry. For revocation we maintain a token blacklist in Redis — store revoked token IDs (jti claim) with TTL matching token expiry. Gateway checks blacklist on every request. Alternatively, use short expiry (15 mins) with refresh tokens — revoke refresh token to prevent new access tokens."

**Q: Why use RS256 instead of HS256 for signing?**
> "HS256 uses a shared secret — both issuer and validator need the same secret. If Gateway has the secret, it could also issue tokens — security risk. RS256 uses asymmetric keys — Auth Service signs with private key, Gateway validates with public key. Gateway can't forge tokens. Better for distributed systems."

**Q: What if JWT secret is compromised?**
> "Rotate the signing key immediately — all existing tokens become invalid (signed with old key). Issue new tokens signed with new key. Users need to re-login. This is why key rotation strategy is important — JWKS endpoint allows publishing multiple valid keys during rotation."

---

## 3. OAuth2

### What is OAuth2?
Authorization framework — allows third party apps to access resources on behalf of user without sharing credentials.

### OAuth2 Grant Types

| Grant Type | Use case | Flow |
|------------|---------|------|
| **Authorization Code** | Web/mobile apps (users) | Most secure — code exchange |
| **Client Credentials** | Machine to machine | No user involved |
| **Password** | Trusted first-party apps | Deprecated — avoid |
| **Implicit** | SPAs (legacy) | Deprecated — avoid |
| **Refresh Token** | Get new access token | Used with Authorization Code |

### Authorization Code Flow (your use case)
```
1. User clicks Login
        ↓
2. App redirects to Authorization Server (Keycloak)
   GET /auth?response_type=code&client_id=finance-app&redirect_uri=...
        ↓
3. User logs in on Keycloak
        ↓
4. Keycloak redirects back with Authorization Code
   GET /callback?code=abc123 (short lived, single use)
        ↓
5. App exchanges code for tokens (server side — secure)
   POST /token
   grant_type=authorization_code&code=abc123&client_secret=...
        ↓
6. Keycloak returns:
   - Access Token  (JWT, short lived — 15 mins)
   - Refresh Token (long lived — days)
   - ID Token      (OIDC — user identity)
        ↓
7. App uses Access Token for API calls
8. When expired → use Refresh Token to get new Access Token
```

### Client Credentials Flow (M2M)
```
Service A needs to call Service B (no user involved)
        ↓
Service A → POST /token
            grant_type=client_credentials
            client_id=finance-service
            client_secret=secret
        ↓
Keycloak returns Access Token
        ↓
Service A calls Service B with Access Token
```

---

## 4. OpenID Connect (OIDC)

### What is OIDC?
Authentication layer on top of OAuth2. OAuth2 = authorization (what can you do). OIDC = authentication (who are you).

```
OAuth2 alone:  Access Token (can call APIs)
OIDC adds:     ID Token    (who is the user)
```

### Tokens in OIDC

| Token | Purpose | Contains |
|-------|---------|----------|
| Access Token | API authorization | Roles, scopes, expiry |
| ID Token | User identity (OIDC) | Name, email, profile |
| Refresh Token | Get new access token | Nothing meaningful |

### ID Token
```json
{
  "sub": "user-uuid-123",
  "name": "Tanay Telsinge",
  "email": "tanay@maxxton.com",
  "picture": "https://keycloak/photo",
  "email_verified": true,
  "iat": 1700000000,
  "exp": 1700003600,
  "iss": "http://keycloak/realms/citi"   // issuer
}
```

### OIDC Endpoints
```
Discovery: /.well-known/openid-configuration
           → returns all endpoint URLs

Authorization: /auth
Token:         /token
UserInfo:      /userinfo      → get user profile
JWKS:          /certs         → public keys for JWT validation
Logout:        /logout
```

---

## 5. Keycloak

### What is Keycloak?
Open source Identity Provider (IdP) implementing OAuth2 + OIDC. Replaces custom Auth Service.

### Key Concepts

| Concept | Description |
|---------|-------------|
| Realm | Isolated tenant — `citi-realm` |
| Client | Registered application — `finance-app` |
| User | Person who authenticates |
| Role | Permission — `FINANCE_ANALYST` |
| Group | Collection of users |
| Scope | What access token grants access to |
| JWKS | Public keys endpoint — Gateway uses to verify JWT |

### Keycloak in Your Architecture
```
┌─────────────────────────────────────────┐
│              Keycloak                   │
│  Realm: maxxton                         │
│  Clients: finance-app, owner-portal     │
│  Roles: FINANCE_ANALYST, OWNER, ADMIN  │
│  Issues: Access Token, ID Token,        │
│          Refresh Token                  │
└─────────────────────────────────────────┘
         ↑ login          ↓ JWKS public keys
┌──────────────┐    ┌──────────────────────┐
│   Browser    │    │    API Gateway       │
│   Mobile App │    │  validates JWT       │
│              │    │  locally via JWKS    │
└──────────────┘    └──────────────────────┘
                           ↓ X-User-Id, X-User-Role
                    ┌──────────────────────┐
                    │  Finance Service     │
                    │  Settlement Service  │
                    │  (trusts headers)    │
                    └──────────────────────┘
```

### Gateway Config with Keycloak
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://keycloak:8080/auth/realms/maxxton/protocol/openid-connect/certs
          issuer-uri: http://keycloak:8080/auth/realms/maxxton
```

```java
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/api/auth/**", "/actuator/health").permitAll()
                .pathMatchers("/api/external/**").hasRole("API_CLIENT")
                .pathMatchers("/api/finance/**").hasRole("FINANCE_ANALYST")
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwkSetUri("http://keycloak:8080/realms/maxxton/protocol/openid-connect/certs")
                )
            )
            .csrf(csrf -> csrf.disable())
            .build();
    }
}
```

### Keycloak Interview Drill-down Q&A

**Q: How does Gateway validate JWT without calling Keycloak on every request?**
> "Gateway downloads Keycloak's public keys from the JWKS endpoint on startup and caches them locally. JWT validation is done locally — verify signature using public key, check expiry, check issuer. No network call to Keycloak per request. Keys are refreshed periodically or when a new key ID (kid) is seen in a token."

**Q: What is a Realm in Keycloak?**
> "A Realm is an isolated tenant in Keycloak — has its own users, roles, clients, and configuration. We had a separate realm per environment — dev-realm, staging-realm, prod-realm. This ensures complete isolation — user in dev realm can't access prod."

**Q: How do you handle Keycloak being down?**
> "Already issued tokens are valid until expiry — Gateway validates locally, no Keycloak call needed. New logins fail — users can't authenticate. For resilience, run Keycloak in HA mode — multiple instances with shared DB (PostgreSQL). Keycloak supports clustering out of the box."

**Q: How do you implement role-based access control with Keycloak?**
> "Keycloak assigns roles to users — FINANCE_ANALYST, OWNER, ADMIN. Roles are included in JWT claims. Gateway enforces coarse-grained access — `hasRole('FINANCE_ANALYST')` for /api/finance/**. Downstream services enforce fine-grained access — check specific permissions from X-User-Role header."

---

## 6. OAuth1 to OAuth2 Migration (Autodesk)

### What is OAuth1?
Older authorization protocol — predecessor to OAuth2.

### OAuth1 vs OAuth2

| | OAuth1 | OAuth2 |
|--|--------|--------|
| Signature | Every request signed with HMAC-SHA1 | Bearer token only |
| Tokens | Request Token + Access Token (2 step) | Access Token + Refresh Token |
| Token Secret | Required for signing | Not needed |
| Complexity | High — request signing complex | Simple — just send Bearer token |
| HTTPS | Optional (signing provides security) | Mandatory |
| Mobile support | Poor | Good |
| Token expiry | No standard expiry | Standard expiry + refresh |
| Stateless | No | Yes (JWT) |

### OAuth1 Flow (old)
```
1. App requests Request Token from server
   POST /oauth/request_token
   Authorization: OAuth oauth_consumer_key=..., oauth_signature=...
        ↓
2. User redirected to authorize
        ↓
3. Server returns Verifier code
        ↓
4. App exchanges Request Token + Verifier for Access Token
   POST /oauth/access_token (signed request)
        ↓
5. Every API call signed with Access Token + Token Secret
   Authorization: OAuth oauth_token=..., oauth_signature=...
```

### OAuth2 Flow (new)
```
1. User redirected to Authorization Server
        ↓
2. User logs in → Authorization Code returned
        ↓
3. App exchanges code for Access Token + Refresh Token
        ↓
4. Every API call → Bearer token in header
   Authorization: Bearer <access_token>
        ↓
5. Token expires → use Refresh Token for new Access Token
```

### Why Migration was Needed
```
OAuth1 problems at Autodesk:
- Complex request signing — every request needs HMAC signature
- Client library complexity — different signing logic per platform
- No token expiry standard — tokens lived forever
- Poor mobile support — signing on mobile was error-prone
- Third party integrations difficult — every partner had to implement signing

OAuth2 benefits:
- Simple Bearer token — just send in header
- Standard token expiry + refresh flow
- Better mobile support
- Wide ecosystem support — every platform has OAuth2 libraries
- OIDC support — user identity built in
```

### Migration Approach
```
Phase 1 — Run both in parallel
├── OAuth1 endpoints kept active
├── OAuth2 endpoints introduced
└── New clients use OAuth2, old clients still on OAuth1

Phase 2 — Migrate clients
├── Notify existing OAuth1 clients
├── Provide OAuth2 migration guide
└── Set deprecation date for OAuth1

Phase 3 — Deprecate OAuth1
├── Monitor OAuth1 usage → zero
└── Remove OAuth1 endpoints
```

### Technical Migration
```java
// OAuth1 — request signing required
OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.autodesk.com/data");
oauthService.signRequest(accessToken, request);  // complex signing
Response response = oauthService.execute(request);

// OAuth2 — simple Bearer token
HttpHeaders headers = new HttpHeaders();
headers.setBearerAuth(accessToken);
ResponseEntity<String> response = restTemplate.exchange(url, GET, 
    new HttpEntity<>(headers), String.class);
```

### Interview Answer — OAuth1 to OAuth2
> "At Autodesk, Fusion 360 was using OAuth1 for API authentication. OAuth1 required every request to be signed with HMAC-SHA1 using the access token and token secret — complex to implement correctly across different platforms and error-prone on mobile. We migrated to OAuth2 which uses simple Bearer tokens — much easier for clients to implement. The migration ran OAuth1 and OAuth2 in parallel during a transition period — existing integrations stayed on OAuth1 while new clients adopted OAuth2. Once OAuth1 usage dropped to zero, we deprecated those endpoints. OAuth2 also gave us the foundation to add OIDC for user identity and standard token expiry with refresh tokens."

---

## Complete Auth Architecture — Both Orgs

### Maxxton (Current)
```
Normal Users → Keycloak (OAuth2 + OIDC) → JWT → Gateway
External B2B → API Key → Gateway → Redis Cache → DB
```

### Autodesk (Previous)
```
OAuth1 (legacy) → migrated to → OAuth2
Fusion 360 API → OAuth2 Bearer Token
```

---

## Interview Answer — Full Auth Picture

> "We handled two types of authentication at the API Gateway. For normal users, we used Keycloak as our identity provider implementing OAuth2 Authorization Code flow with OIDC. User logs in on Keycloak, receives Access Token and ID Token. Gateway validates JWT locally using Keycloak's JWKS public keys — no Keycloak call per request. User ID and roles injected as headers for downstream services.
>
> For external B2B clients, we used API Key authentication — X-API-Key header validated against Redis cache backed by DB. Redis prevents DB hit on every request, with immediate revocation by deleting the cache entry.
>
> At Autodesk, I was part of migrating from OAuth1 to OAuth2 — OAuth1's complex request signing was a pain point for clients. OAuth2's Bearer token model is much simpler and has better ecosystem support. We ran both in parallel during migration and deprecated OAuth1 once all clients migrated."

---

## Common Interview Questions

**Q: Difference between Authentication and Authorization?**
> "Authentication = who are you (login). Authorization = what can you do (permissions). OAuth2 handles authorization, OIDC adds authentication."

**Q: What is the difference between OAuth2 and OIDC?**
> "OAuth2 is an authorization framework — issues Access Token for resource access. OIDC is an authentication layer on top of OAuth2 — adds ID Token containing user identity. OIDC = OAuth2 + who is the user."

**Q: Why use Authorization Code flow instead of Implicit?**
> "Implicit flow returns Access Token directly in URL fragment — visible in browser history, logs. Authorization Code flow returns a short-lived code, exchanged server-side for tokens — tokens never exposed in URL. Implicit is deprecated in OAuth2.1."

**Q: What is PKCE?**
> "Proof Key for Code Exchange — extension to Authorization Code flow for public clients (mobile apps, SPAs) that can't keep a client secret. App generates a code verifier and code challenge — prevents authorization code interception attacks."

**Q: How do you handle token refresh?**
> "Access Token expires (15 mins). App uses Refresh Token to silently get new Access Token — no user interaction needed. If Refresh Token also expired, user must re-login. Refresh tokens are long-lived (days) but stored server-side in Keycloak — can be revoked immediately."