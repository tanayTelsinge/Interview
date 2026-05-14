# Forms

## Two Approaches

| | Template-Driven | Reactive |
|---|---|---|
| Logic lives in | HTML template | TypeScript class |
| Module needed | `FormsModule` | `ReactiveFormsModule` |
| Validation | HTML attributes + directives | Validators array in TS |
| Testing | Harder (needs DOM) | Easy (pure TS) |
| Dynamic fields | Awkward | Easy (FormArray) |
| Use for | Simple forms (login, contact) | Complex forms (multi-step, dynamic) |

---

## Template-Driven Forms

```typescript
// app.component.ts
import { FormsModule } from '@angular/forms';
@Component({ imports: [FormsModule] })
```

```html
<form #loginForm="ngForm" (ngSubmit)="onSubmit(loginForm.value)">
  <input
    name="email"
    ngModel
    required
    email
    #emailField="ngModel"
  >
  <span *ngIf="emailField.invalid && emailField.touched">
    Invalid email
  </span>

  <input name="password" type="password" ngModel required minlength="6">

  <button type="submit" [disabled]="loginForm.invalid">Login</button>
</form>
```

```typescript
onSubmit(value: any) {
  console.log(value);  // { email: '...', password: '...' }
}
```

---

## Reactive Forms

```typescript
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

@Component({ imports: [ReactiveFormsModule] })
export class LoginComponent {
  private fb = inject(FormBuilder);

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  onSubmit() {
    if (this.form.valid) {
      console.log(this.form.value);
    }
  }
}
```

```html
<form [formGroup]="form" (ngSubmit)="onSubmit()">
  <input formControlName="email">
  <span *ngIf="form.get('email')?.invalid && form.get('email')?.touched">
    {{ getEmailError() }}
  </span>

  <input formControlName="password" type="password">

  <button type="submit" [disabled]="form.invalid">Login</button>
</form>
```

---

## Accessing Form Control State

```typescript
const emailCtrl = this.form.get('email');

emailCtrl.value        // current value
emailCtrl.valid        // passes all validators
emailCtrl.invalid      // fails at least one
emailCtrl.errors       // { required: true } or { email: true } etc
emailCtrl.touched      // user clicked in then out
emailCtrl.dirty        // user typed something
emailCtrl.pristine     // untouched by user
```

---

## Built-in Validators

```typescript
Validators.required
Validators.email
Validators.minLength(6)
Validators.maxLength(100)
Validators.pattern(/^[a-z]+$/)
Validators.min(0)
Validators.max(100)
```

---

## Custom Validator

```typescript
// Sync custom validator
function noSpacesValidator(control: AbstractControl): ValidationErrors | null {
  if (control.value && control.value.includes(' ')) {
    return { noSpaces: true };  // error key
  }
  return null;  // valid
}

// Async custom validator (check username availability)
function usernameAvailableValidator(apiService: ApiService): AsyncValidatorFn {
  return (control: AbstractControl): Observable<ValidationErrors | null> => {
    return apiService.checkUsername(control.value).pipe(
      map(available => available ? null : { usernameTaken: true }),
      catchError(() => of(null))
    );
  };
}

// Usage
username: ['', [Validators.required], [usernameAvailableValidator(this.apiService)]]
//          value  sync validators      async validators
```

---

## Cross-field Validation (Form-level)

```typescript
function passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return password === confirm ? null : { passwordMismatch: true };
}

form = this.fb.group({
  password: ['', Validators.required],
  confirmPassword: ['', Validators.required]
}, { validators: passwordMatchValidator });  // validator at group level
```

```html
<span *ngIf="form.errors?.['passwordMismatch']">Passwords do not match</span>
```

---

## FormArray — Dynamic Fields

```typescript
form = this.fb.group({
  name: [''],
  phones: this.fb.array([
    this.fb.control('')  // initial entry
  ])
});

get phones() { return this.form.get('phones') as FormArray; }

addPhone() {
  this.phones.push(this.fb.control(''));
}

removePhone(i: number) {
  this.phones.removeAt(i);
}
```

```html
<div formArrayName="phones">
  <div *ngFor="let phone of phones.controls; let i = index">
    <input [formControlName]="i">
    <button (click)="removePhone(i)">Remove</button>
  </div>
</div>
<button (click)="addPhone()">Add Phone</button>
```

---

## setValue vs patchValue

```typescript
// setValue — must set ALL fields
this.form.setValue({ email: 'a@b.com', password: '123456' });

// patchValue — set only some fields (partial update)
this.form.patchValue({ email: 'a@b.com' });  // password unchanged
```

---

## Follow-up Questions

**Q: Template-driven vs Reactive — which do you prefer and why?**
→ Reactive for almost everything. Logic lives in TypeScript (testable, type-safe, IDE support). Dynamic fields with FormArray are trivial. Template-driven is fine for a simple 2-field login form.

**Q: How do you reset a form after submit?**
→ `this.form.reset()` — clears all values and resets validation state (touched, dirty). `this.form.reset({ email: '' })` to reset with default values.

**Q: When does a validator fire?**
→ By default on every value change (`updateOn: 'change'`). Can configure `updateOn: 'blur'` (on field leave) or `updateOn: 'submit'` for the group.

**Q: How do you show error messages only after user interaction?**
→ Check `touched` (user focused then left) or `dirty` (user typed). Showing errors immediately on load is bad UX — gate with `touched`.

**Q: What is the difference between sync and async validators?**
→ Sync validators return `ValidationErrors | null` immediately. Async validators return `Observable<ValidationErrors | null>` — used for server-side checks like username availability. Async validators only run if all sync validators pass.
