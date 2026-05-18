# 05 - Services & Dependency Injection

## What is a Service?
- A class that holds business logic, shared data, or API calls
- Kept separate from components (single responsibility)

## Creating a Service
```bash
ng generate service user
```

```typescript
@Injectable({
  providedIn: 'root'   // singleton — one instance for whole app
})
export class UserService {
  private users = ['Alice', 'Bob'];

  getUsers() {
    return this.users;
  }
}
```

## Using in Component (Dependency Injection)
```typescript
// Angular automatically injects it
constructor(private userService: UserService) {}

ngOnInit() {
  this.users = this.userService.getUsers();
}
```

## How DI works
- Angular sees `UserService` in constructor
- Looks up its injector tree for an instance
- `providedIn: 'root'` → creates one shared instance (singleton)
- `providers: [UserService]` in a component → creates new instance per component

## Practice
- [ ] Create a UserService with a list of users
- [ ] Inject it into two different components
- [ ] Verify both components share the same data (singleton)
