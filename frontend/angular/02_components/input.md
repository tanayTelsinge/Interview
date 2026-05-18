# @Input — Passing Data from Parent to Child

## What is @Input?
Decorator that lets a parent component pass data down to a child component.

## Syntax

**Child component (user-input.ts):**
```typescript
import { Component, Input } from '@angular/core';

@Component({ selector: 'app-user-input', ... })
export class UserInput {
  @Input() name = '';
  @Input() age = 0;
  @Input() role = '';
}
```

**Parent template (app.html):**
```html
<app-user-input name="Jane Doe" [age]="25" role="Frontend Developer" />
```

## Why [] for some properties and not others?

| Syntax | Type passed | Example |
|---|---|---|
| `name="Jane"` | String — no evaluation needed | `name="Jane Doe"` |
| `[age]="25"` | Number, boolean, object, array, variable | `[age]="25"` |

- Without `[]` → value treated as plain string
- With `[]` → value evaluated as TypeScript expression

```html
<app-user age="25" />    <!-- age = "25" (string) ❌ -->
<app-user [age]="25" />  <!-- age = 25  (number) ✓  -->
```

**Rule:** passing a string literal → no `[]`. Everything else → use `[]`.

## Interpolation types (practiced in user component)

| Type | Example | Output |
|---|---|---|
| Simple variable | `{{ name }}` | John Doe |
| Expression | `{{ age + 5 }}` | 33 |
| String method | `{{ name.toUpperCase() }}` | JOHN DOE |
| Ternary | `{{ age >= 18 ? 'Adult' : 'Minor' }}` | Adult |
| Object property | `{{ address.city }}` | Mumbai |

**Cannot use in interpolation:** variable declarations, if statements, assignments — expression only.
