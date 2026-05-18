# 06 - Routing

## Setup
```typescript
// app-routing.module.ts
const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'about', component: AboutComponent },
  { path: 'user/:id', component: UserComponent },
  { path: '**', component: NotFoundComponent }  // wildcard
];
```

## Template
```html
<!-- navigation -->
<a routerLink="/">Home</a>
<a routerLink="/about">About</a>
<a [routerLink]="['/user', userId]">User</a>

<!-- renders the matched component here -->
<router-outlet></router-outlet>
```

## Reading Route Params
```typescript
constructor(private route: ActivatedRoute) {}

ngOnInit() {
  const id = this.route.snapshot.paramMap.get('id');
  // or subscribe for dynamic changes:
  this.route.paramMap.subscribe(params => {
    const id = params.get('id');
  });
}
```

## Programmatic Navigation
```typescript
constructor(private router: Router) {}

goToHome() {
  this.router.navigate(['/']);
}
```

## Route Guards
```typescript
@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  canActivate(): boolean {
    return isLoggedIn ? true : false;
  }
}
// in routes:
{ path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard] }
```

## Lazy Loading
```typescript
{ path: 'admin', loadChildren: () => import('./admin/admin.module').then(m => m.AdminModule) }
```

## Practice
- [ ] Set up 3 routes with navigation
- [ ] Pass and read route params
- [ ] Add a wildcard 404 page
- [ ] Protect a route with a guard
