# Components & Lifecycle

## Component Anatomy

```typescript
@Component({
  selector: 'app-user',            // used as <app-user> in HTML
  templateUrl: './user.component.html',
  styleUrls: ['./user.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,                // Angular 17+ — no NgModule needed
  imports: [CommonModule]          // import directives/pipes directly
})
export class UserComponent implements OnInit, OnDestroy {
  @Input() userId: number;
  @Output() selected = new EventEmitter<User>();
}
```

---

## Lifecycle Hooks (in execution order)

```
CREATION                      UPDATES                      DESTRUCTION
────────                      ───────                      ───────────
constructor()                 ngOnChanges()                ngOnDestroy()
ngOnChanges() ← first call    ngDoCheck()
ngOnInit()                    ngAfterContentChecked()
ngAfterContentInit()          ngAfterViewChecked()
ngAfterContentChecked()
ngAfterViewInit()
ngAfterViewChecked()
```

| Hook | When | Use for |
|---|---|---|
| `constructor` | Class instantiated | DI injection only — no DOM, no inputs yet |
| `ngOnChanges` | Before ngOnInit, then whenever @Input changes | React to input changes with previous/current values |
| `ngOnInit` | After first ngOnChanges | Init logic, API calls, setup |
| `ngAfterContentInit` | After `<ng-content>` projected | Access ContentChild for first time |
| `ngAfterViewInit` | After template + child components rendered | Access ViewChild for first time |
| `ngOnDestroy` | Before component removed | Unsubscribe, cleanup timers |

---

## constructor vs ngOnInit — Interview Answer

```typescript
constructor(private userService: UserService) {
  // ✓ DI injection ready
  // ✗ @Input() not set yet — don't use this.userId here
  // ✗ DOM not available
}

ngOnInit() {
  // ✓ @Input() values available
  // ✓ Safe to call APIs
  this.userService.getUser(this.userId).subscribe(...);
}
```

> Rule: constructor = wiring (DI only). ngOnInit = initialization logic.
> Java analogy: constructor = Spring bean instantiation. ngOnInit = `@PostConstruct`.

---

## @Input — Receiving Data

```typescript
// Child
@Input() title = '';
@Input({ required: true }) userId!: number;  // Angular 16+ required input

// Parent template
<app-user title="Hello" [userId]="selectedId" />
```

**ngOnChanges — detecting input changes:**
```typescript
ngOnChanges(changes: SimpleChanges) {
  if (changes['userId']) {
    const prev = changes['userId'].previousValue;
    const curr = changes['userId'].currentValue;
    this.loadUser(curr);
  }
}
```

---

## @Output — Sending Events Up

```typescript
// Child
@Output() userSelected = new EventEmitter<User>();

selectUser(user: User) {
  this.userSelected.emit(user);
}

// Parent template
<app-user (userSelected)="onUserSelected($event)" />

// Parent component
onUserSelected(user: User) {
  this.currentUser = user;
}
```

---

## Content Projection — ng-content

Pass HTML from parent into a child's template (like React `children`):

```html
<!-- parent -->
<app-card>
  <h2>Title</h2>
  <p>Content goes here</p>
</app-card>

<!-- app-card template -->
<div class="card">
  <ng-content></ng-content>
</div>
```

**Multi-slot projection:**
```html
<ng-content select="[header]"></ng-content>
<ng-content select="[body]"></ng-content>

<!-- usage -->
<app-card>
  <h2 header>Title</h2>
  <p body>Content</p>
</app-card>
```

---

## ViewChild vs ContentChild

| | ViewChild | ContentChild |
|---|---|---|
| What it accesses | Elements inside this component's own template | Elements projected via `<ng-content>` |
| Available in | `ngAfterViewInit` | `ngAfterContentInit` |

```typescript
@ViewChild('myInput') inputRef: ElementRef;        // <input #myInput>
@ContentChild('headerSlot') header: ElementRef;     // projected content

ngAfterViewInit() {
  this.inputRef.nativeElement.focus();
}
```

---

## Memory Leak Pattern — Always Unsubscribe

```typescript
export class UserComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  ngOnInit() {
    this.userService.users$
      .pipe(takeUntil(this.destroy$))  // auto-unsubscribes on destroy
      .subscribe(users => this.users = users);
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
```

---

## Follow-up Questions

**Q: What's the difference between ngOnInit and constructor?**
→ Constructor only has DI available — @Input values aren't set yet. ngOnInit is called after the first ngOnChanges, so all inputs are ready. Always put init logic in ngOnInit.

**Q: When does ngOnChanges NOT fire?**
→ If the @Input value is an object/array and you mutate it (same reference). Object identity check — Angular only sees a change if the reference changes.

**Q: What is ngDoCheck?**
→ Runs on every change detection cycle. Very expensive — avoid unless you need custom dirty-checking. ngOnChanges is preferred for @Input changes.

**Q: How do you avoid memory leaks with subscriptions?**
→ Three approaches: `takeUntil(destroy$)`, `async pipe` (auto-unsubscribes), or `takeUntilDestroyed()` from `@angular/core` (Angular 16+).

**Q: Can a child component communicate directly with its parent without @Output?**
→ Yes via shared service, or via template reference variable (`@ViewChild` on parent). But @Output is the idiomatic Angular way for direct parent-child communication.
