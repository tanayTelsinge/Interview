# Performance & Common Interview Questions

## Performance Techniques

### 1. OnPush Change Detection
Biggest performance win in Angular. Skips CD check unless @Input reference changes or async pipe emits.

```typescript
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserCardComponent {
  @Input() user: User;
}
```

**Rule:** use OnPush on all "dumb" / presentational components — they only receive inputs and emit outputs.

---

### 2. trackBy in *ngFor

Without trackBy, Angular re-creates ALL DOM nodes on every array change.
With trackBy, Angular identifies each item by ID and only re-renders changed items.

```html
<!-- Without trackBy — recreates entire list on every update -->
<li *ngFor="let user of users">{{ user.name }}</li>

<!-- With trackBy — only re-renders changed/added/removed items -->
<li *ngFor="let user of users; trackBy: trackByUserId">{{ user.name }}</li>
```

```typescript
trackByUserId(index: number, user: User): number {
  return user.id;
}
```

---

### 3. Lazy Loading Modules / Routes

Split your app into chunks — load admin, dashboard, settings only when user navigates there.

```typescript
{
  path: 'admin',
  loadChildren: () => import('./admin/admin.routes').then(r => r.adminRoutes)
}
```

---

### 4. Avoid Function Calls in Templates

Functions in templates re-execute on every CD cycle:

```html
<!-- Bad — getUserDisplayName() called on every CD cycle -->
<span>{{ getUserDisplayName(user) }}</span>

<!-- Good — computed once, only changes when user changes -->
<span>{{ user.firstName + ' ' + user.lastName }}</span>

<!-- Best for complex logic — use a pipe (pure pipes are memoized) -->
<span>{{ user | displayName }}</span>
```

---

### 5. Pure Pipes vs Impure Pipes

| | Pure (default) | Impure |
|---|---|---|
| Re-executes when | Input reference changes | Every CD cycle |
| Performance | Good | Expensive |
| Use for | Transformations | Time, random, side-effect transforms |

```typescript
@Pipe({ name: 'filter', pure: true })  // only recalculates when array reference changes
export class FilterPipe implements PipeTransform {
  transform(items: Item[], search: string): Item[] {
    return items.filter(i => i.name.includes(search));
  }
}
```

---

### 6. async Pipe for Subscriptions

Avoid manual subscription management — async pipe unsubscribes automatically:

```typescript
// Avoid
ngOnInit() { this.users$.subscribe(u => this.users = u); }  // must manage subscription

// Prefer
users$ = this.userService.users$;  // let async pipe handle it
```

---

### 7. Preloading Strategies

```typescript
provideRouter(routes, withPreloading(PreloadAllModules))
// Downloads all lazy modules in background after app loads
// Trade-off: faster subsequent navigation, slightly more initial bandwidth

// Custom strategy: preload only routes with data: { preload: true }
```

---

## Common Interview Questions

**Q: What is Angular's compilation — JIT vs AOT?**
→ JIT (Just-In-Time): compiles in browser at runtime — used in development (`ng serve`). AOT (Ahead-of-Time): compiles at build time — used in production (`ng build`). AOT catches template errors at build time, smaller bundle, faster startup.

---

**Q: What is tree-shaking?**
→ Bundler (webpack/esbuild) removes unused code from the final bundle. Angular's standalone components enable better tree-shaking — each component imports exactly what it uses, so unused directives/pipes are eliminated.

---

**Q: What is the difference between `*ngIf` and hidden attribute?**

```html
<div *ngIf="show">Content</div>     <!-- removes from DOM entirely -->
<div [hidden]="!show">Content</div>  <!-- stays in DOM, CSS display:none -->
```

`*ngIf` destroys component (runs ngOnDestroy, frees memory). `[hidden]` keeps component alive (preserves state, no teardown cost). Use `*ngIf` for conditional logic, `[hidden]` when you need to preserve component state across toggles.

---

**Q: What is a pure component / presentational component in Angular?**
→ A component that only receives data via @Input and emits events via @Output. No service injection, no direct store access. Benefits: easy to test, easy to reuse, naturally works with OnPush CD.

---

**Q: @Component vs @Directive — difference?**
→ @Component = @Directive + template. Components have a view. Directives add behavior to existing elements.

```typescript
@Directive({ selector: '[appHighlight]' })  // no template — extends existing elements
export class HighlightDirective {
  @HostListener('mouseenter') onEnter() { this.el.nativeElement.style.background = 'yellow'; }
}
```

---

**Q: What is the difference between `[attr.disabled]` and `[disabled]`?**

```html
<button [disabled]="isDisabled">         <!-- property binding — works correctly -->
<button [attr.disabled]="isDisabled">    <!-- attribute binding — sets string "false", still disabled! -->
```

Property binding sets the DOM property. Attribute binding sets the HTML attribute. For boolean attributes, always use property binding.

---

**Q: What is ng-container?**
→ A logical wrapper that renders nothing in the DOM — useful for applying structural directives without adding extra elements.

```html
<!-- Want ngIf + ngFor without a wrapping div -->
<ng-container *ngIf="show">
  <li *ngFor="let item of items">{{ item }}</li>
</ng-container>
```

---

**Q: What is ViewEncapsulation?**

```typescript
encapsulation: ViewEncapsulation.Emulated  // default — Angular scopes CSS to component
encapsulation: ViewEncapsulation.None       // global CSS — styles leak out
encapsulation: ViewEncapsulation.ShadowDom  // native Shadow DOM isolation
```

---

**Q: What is a `forwardRef` and when is it needed?**
→ Used when you need to reference a class before it's declared (circular references):

```typescript
providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => MyInputComponent) }]
```

---

**Q: What is Content Security and why avoid `innerHTML`?**
→ Setting `innerHTML` directly is an XSS risk. Angular sanitizes `[innerHTML]` bindings but it's still risky for user-provided HTML. Prefer Angular interpolation `{{ }}` which auto-escapes, or `DomSanitizer.bypassSecurityTrustHtml()` only when you fully trust the content.

---

## Angular-Specific Pitfalls

| Pitfall | Fix |
|---|---|
| Mutating @Input object — OnPush doesn't update | Spread into new object before passing |
| Forgetting to unsubscribe | Use async pipe or takeUntilDestroyed |
| Multiple subscriptions in ngOnInit | Compose with combineLatest / forkJoin |
| Function calls in templates | Use pure pipes or computed signals |
| Forgetting `trackBy` in large lists | Always add trackBy for lists that change |
| ngOnChanges not firing | You mutated the object reference — create new object |
| Circular DI (A depends on B depends on A) | Refactor to extract shared service C |
| Zone.js not detecting changes | Use zone-aware APIs, or manually call markForCheck() |
