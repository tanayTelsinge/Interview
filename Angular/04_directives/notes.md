# 04 - Directives

## Types
- **Structural** → change DOM structure (*ngIf, *ngFor, *ngSwitch)
- **Attribute** → change appearance/behavior (ngClass, ngStyle)
- **Custom** → your own directive

## Structural Directives
```html
<!-- ngIf -->
<p *ngIf="isLoggedIn">Welcome!</p>
<p *ngIf="isLoggedIn; else loggedOut">Welcome!</p>
<ng-template #loggedOut><p>Please login</p></ng-template>

<!-- ngFor -->
<li *ngFor="let item of items; let i = index">{{ i }}: {{ item }}</li>

<!-- ngSwitch -->
<div [ngSwitch]="color">
  <p *ngSwitchCase="'red'">Red</p>
  <p *ngSwitchCase="'blue'">Blue</p>
  <p *ngSwitchDefault>Other</p>
</div>
```

## Attribute Directives
```html
<div [ngClass]="{ 'active': isActive, 'disabled': isDisabled }">...</div>
<div [ngStyle]="{ 'color': textColor, 'font-size': fontSize + 'px' }">...</div>
```

## Custom Directive
```typescript
@Directive({ selector: '[appHighlight]' })
export class HighlightDirective {
  constructor(private el: ElementRef) {}

  @HostListener('mouseenter') onMouseEnter() {
    this.el.nativeElement.style.backgroundColor = 'yellow';
  }
  @HostListener('mouseleave') onMouseLeave() {
    this.el.nativeElement.style.backgroundColor = '';
  }
}
```
```html
<p appHighlight>Hover over me!</p>
```

## Practice
- [ ] Show/hide element with *ngIf
- [ ] Render a list with *ngFor
- [ ] Toggle class with ngClass
- [ ] Create a custom highlight directive
