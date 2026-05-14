# 03 - Data Binding

## 4 Types

### 1. Interpolation (Component → Template)
```html
<h1>{{ title }}</h1>
<p>{{ 2 + 2 }}</p>
```

### 2. Property Binding (Component → Template)
```html
<img [src]="imageUrl">
<button [disabled]="isDisabled">Click</button>
```

### 3. Event Binding (Template → Component)
```html
<button (click)="onClick()">Click Me</button>
<input (keyup)="onKeyUp($event)">
```

### 4. Two-Way Binding (Both directions)
```html
<!-- needs FormsModule imported in app.module.ts -->
<input [(ngModel)]="username">
<p>{{ username }}</p>
```

## Summary
| Type | Syntax | Direction |
|------|--------|-----------|
| Interpolation | `{{ value }}` | TS → HTML |
| Property | `[property]="value"` | TS → HTML |
| Event | `(event)="handler()"` | HTML → TS |
| Two-way | `[(ngModel)]="value"` | Both |

## Practice
- [ ] Display a variable using interpolation
- [ ] Bind an image src dynamically
- [ ] Handle button click event
- [ ] Create a live preview with two-way binding
