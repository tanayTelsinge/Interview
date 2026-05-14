# Dependency Injection

## What is DI?
A design pattern where a class receives its dependencies from an external source (the injector) rather than creating them itself.

Angular has a built-in DI framework — you declare what you need, Angular provides it.

```typescript
// Without DI — tightly coupled
export class UserComponent {
  service = new UserService();  // hard-coded dependency
}

// With DI — loosely coupled
export class UserComponent {
  constructor(private userService: UserService) {}  // Angular injects it
}
```

> Java analogy: Spring's `@Autowired`. Angular's DI is the same concept — IoC container manages object lifecycle and wires dependencies.

---

## How Angular's DI Works

1. You declare a class with `@Injectable`
2. Angular's injector creates and stores an instance
3. When a component requests it via constructor, injector provides the stored instance

```typescript
@Injectable({
  providedIn: 'root'   // registered in root injector → singleton
})
export class UserService {
  getUsers() { return ['Alice', 'Bob']; }
}
```

---

## Injector Tree (Hierarchical DI)

Angular has a tree of injectors mirroring the component tree:

```
Root Injector (providedIn: 'root')
  └── Module Injector
        └── Component Injector
              └── Child Component Injector
```

Angular walks UP the tree to find the provider. First match wins.

```typescript
// Root — one instance for entire app
@Injectable({ providedIn: 'root' })

// Component-level — new instance per component
@Component({
  providers: [UserService]   // this component gets its own instance
})
```

---

## Provider Scope — Which to use

| Scope | Declaration | Instances | Use when |
|---|---|---|---|
| Root | `providedIn: 'root'` | 1 (singleton) | Shared services (auth, cart, http) |
| Module | `providers: []` in NgModule | 1 per module | Feature-specific shared services |
| Component | `providers: []` in @Component | 1 per component instance | Isolated state per component |

```typescript
// Singleton — auth state shared everywhere
@Injectable({ providedIn: 'root' })
export class AuthService {}

// Per-component — each form gets its own state
@Component({ providers: [FormStateService] })
export class FormComponent {}
```

---

## Injection Tokens (non-class dependencies)

When you need to inject a primitive or an interface (not a class), use `InjectionToken`:

```typescript
// Define token
const API_URL = new InjectionToken<string>('API_URL');

// Provide value
@NgModule({
  providers: [
    { provide: API_URL, useValue: 'https://api.example.com' }
  ]
})

// Inject
constructor(@Inject(API_URL) private apiUrl: string) {}
```

---

## Provider Types

```typescript
// useClass — provide a different class (common for mocking)
{ provide: UserService, useClass: MockUserService }

// useValue — provide a static value
{ provide: API_URL, useValue: 'https://api.example.com' }

// useFactory — compute the value at runtime
{ provide: Logger, useFactory: () => environment.prod ? new ProdLogger() : new DevLogger() }

// useExisting — alias one token to another
{ provide: NewService, useExisting: OldService }
```

---

## Modern DI (Angular 14+) — inject() function

Alternative to constructor injection, works anywhere including functions:

```typescript
// inject() can be called in constructor, field initializer, or factory
@Component({...})
export class UserComponent {
  private userService = inject(UserService);   // no constructor needed
  private router = inject(Router);
}
```

Especially useful in standalone functions and guards:

```typescript
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isLoggedIn() ? true : router.createUrlTree(['/login']);
};
```

---

## Testing — Override with Mock

```typescript
TestBed.configureTestingModule({
  providers: [
    { provide: UserService, useClass: MockUserService }
  ]
});
```

---

## Follow-up Questions

**Q: What is `providedIn: 'root'` vs declaring in `providers[]`?**
→ `providedIn: 'root'` registers with root injector → singleton, tree-shakeable (if unused, bundler removes it). `providers[]` in component/module creates a new instance per component — not tree-shakeable.

**Q: Can two components share a service instance that's NOT the root singleton?**
→ Yes. Declare `providers: [MyService]` on their common ancestor component. Both children will share that ancestor's instance.

**Q: What happens if you provide the same service at both root and component level?**
→ Component gets its own instance. Injector walks UP the tree — the nearest provider wins. Root singleton is not used by that component.

**Q: What is tree-shakeable providers and why does it matter?**
→ `providedIn: 'root'` tells the compiler which services are used. If a service is never injected, the bundler eliminates it. Declaring in NgModule `providers[]` always includes the service in the bundle even if unused.

**Q: inject() vs constructor injection — when to prefer inject()?**
→ `inject()` works in field initializers (cleaner syntax), class inheritance (no need to pass through super()), and functional constructs like guards/resolvers. Constructor injection is fine too — both are equivalent.
