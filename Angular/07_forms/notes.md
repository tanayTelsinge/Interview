# 07 - Forms

## Two Approaches

### 1. Template-Driven Forms (simple, uses HTML)
```typescript
// import FormsModule in app.module.ts
```
```html
<form #myForm="ngForm" (ngSubmit)="onSubmit(myForm)">
  <input name="email" ngModel required email>
  <span *ngIf="myForm.controls['email']?.invalid">Invalid email</span>
  <button type="submit" [disabled]="myForm.invalid">Submit</button>
</form>
```

### 2. Reactive Forms (complex, uses TypeScript — preferred)
```typescript
// import ReactiveFormsModule in app.module.ts

form = new FormGroup({
  email: new FormControl('', [Validators.required, Validators.email]),
  password: new FormControl('', [Validators.minLength(6)])
});

onSubmit() {
  console.log(this.form.value);
}
```
```html
<form [formGroup]="form" (ngSubmit)="onSubmit()">
  <input formControlName="email">
  <span *ngIf="form.get('email')?.invalid">Invalid</span>
  <button [disabled]="form.invalid">Submit</button>
</form>
```

## Key Validators
```typescript
Validators.required
Validators.email
Validators.minLength(6)
Validators.maxLength(20)
Validators.pattern('^[a-z]+$')
```

## When to use which?
| | Template-driven | Reactive |
|---|---|---|
| Complexity | Simple forms | Complex forms |
| Control | Less | More |
| Testing | Harder | Easier |

## Practice
- [ ] Build a login form with template-driven approach
- [ ] Rebuild same with reactive forms
- [ ] Add validation messages
