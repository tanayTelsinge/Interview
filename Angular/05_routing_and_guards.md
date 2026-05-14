# Routing & Guards

## Basic Setup (Angular 17+ standalone)

```typescript
// app.routes.ts
export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'users', component: UsersComponent },
  { path: 'users/:id', component: UserDetailComponent },
  { path: '**', component: NotFoundComponent }   // wildcard — must be last
];

// main.ts
bootstrapApplication(AppComponent, {
  providers: [provideRouter(routes)]
});
```

```html
<!-- Template -->
<a routerLink="/">Home</a>
<a [routerLink]="['/users', userId]">User</a>
<router-outlet></router-outlet>
```

---

## Route Parameters

```typescript
// Snapshot — read once
constructor(private route: ActivatedRoute) {}

ngOnInit() {
  const id = this.route.snapshot.paramMap.get('id');
}

// Observable — reacts when params change without component re-creating
ngOnInit() {
  this.route.paramMap.subscribe(params => {
    const id = params.get('id');
    this.loadUser(+id);
  });
}

// Query params — /users?page=2&sort=name
const page = this.route.snapshot.queryParamMap.get('page');
```

---

## Programmatic Navigation

```typescript
constructor(private router: Router) {}

goToUser(id: number) {
  this.router.navigate(['/users', id]);
  // with query params
  this.router.navigate(['/users'], { queryParams: { page: 2 } });
}
```

---

## Lazy Loading — Critical for Performance

Splits bundle — loads feature module only when user navigates to it:

```typescript
// Old (NgModule-based)
{
  path: 'admin',
  loadChildren: () => import('./admin/admin.module').then(m => m.AdminModule)
}

// Modern (standalone components)
{
  path: 'admin',
  loadComponent: () => import('./admin/admin.component').then(c => c.AdminComponent)
}

// Lazy load entire route group
{
  path: 'admin',
  loadChildren: () => import('./admin/admin.routes').then(r => r.adminRoutes)
}
```

---

## Route Guards

### Functional Guards (Angular 14+) — preferred

```typescript
// auth.guard.ts
export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isLoggedIn()) return true;
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

// routes
{ path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] }
```

### Guard Types

| Guard | Interface | Fires when |
|---|---|---|
| `canActivate` | `CanActivateFn` | Before entering a route |
| `canActivateChild` | `CanActivateChildFn` | Before entering child routes |
| `canDeactivate` | `CanDeactivateFn` | Before leaving a route (unsaved changes warning) |
| `canLoad` | `CanLoadFn` | Before lazy-loading a module |
| `resolve` | `ResolveFn` | Prefetch data before component loads |

---

## Resolver — Preload Data Before Component

```typescript
// user.resolver.ts
export const userResolver: ResolveFn<User> = (route) => {
  const userService = inject(UserService);
  const id = route.paramMap.get('id');
  return userService.getUser(+id);
};

// routes
{
  path: 'users/:id',
  component: UserDetailComponent,
  resolve: { user: userResolver }
}

// component — data already available, no loading state needed
export class UserDetailComponent {
  user = inject(ActivatedRoute).snapshot.data['user'] as User;
}
```

---

## canDeactivate — Unsaved Changes Warning

```typescript
export const unsavedChangesGuard: CanDeactivateFn<EditComponent> = (component) => {
  if (component.hasUnsavedChanges()) {
    return confirm('You have unsaved changes. Leave anyway?');
  }
  return true;
};

{ path: 'edit', component: EditComponent, canDeactivate: [unsavedChangesGuard] }
```

---

## Nested Routes (Child Routes)

```typescript
{
  path: 'users',
  component: UsersLayoutComponent,
  children: [
    { path: '', component: UserListComponent },
    { path: ':id', component: UserDetailComponent },
    { path: ':id/edit', component: UserEditComponent }
  ]
}
```

```html
<!-- users-layout.component.html -->
<nav>...</nav>
<router-outlet></router-outlet>  <!-- child components render here -->
```

---

## Router Events — Listening to Navigation

```typescript
constructor(private router: Router) {
  router.events.pipe(
    filter(e => e instanceof NavigationEnd)
  ).subscribe(() => {
    window.scrollTo(0, 0);  // scroll to top on navigation
  });
}
```

---

## Follow-up Questions

**Q: What's the difference between snapshot and observable for route params?**
→ Snapshot reads the value once. Observable updates when params change — needed when navigating from `/users/1` to `/users/2` without the component being destroyed (Angular reuses it). Always prefer observable if the component can receive new params.

**Q: What is lazy loading and why does it matter?**
→ Splits the app into multiple JS chunks. The admin module only loads when the user navigates to `/admin`. Reduces initial bundle size → faster first load. Critical for large apps.

**Q: canActivate vs canLoad?**
→ `canActivate` fires after the module is loaded but before the component activates. `canLoad` fires before even downloading the lazy module — prevents unauthorized users from downloading admin code. Use both together for secure lazy-loaded routes.

**Q: How do you pass data from a resolver to a component?**
→ Via `route.snapshot.data['key']` or subscribe to `route.data`. The resolver runs before the component, so data is ready in `ngOnInit`.

**Q: How to handle query params vs path params?**
→ Path params (`/users/:id`) for resource identity — always required. Query params (`/users?page=2`) for optional filtering, pagination, sorting — can be omitted.
