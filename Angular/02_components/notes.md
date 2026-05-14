# 02 - Components

## Creating a Component
```bash
ng generate component my-component
# shorthand
ng g c my-component
```

## Key Concepts
- **Interpolation** `{{ }}` — display component data in template
- **Selector** — `selector: 'app-user'` → used as `<app-user />` in HTML
- **Standalone imports** — import child component directly in parent's `imports: []` array (no module needed)

## Component Structure
```typescript
@Component({
  selector: 'app-hello',       // used as <app-hello> in HTML
  templateUrl: './hello.component.html',
  styleUrls: ['./hello.component.css']
})
export class HelloComponent {
  title = 'Hello Angular';
}
```

## Lifecycle Hooks (in order)
| Hook | When it runs |
|------|-------------|
| ngOnChanges | when @Input changes |
| ngOnInit | after component initializes (use this like constructor) |
| ngDoCheck | every change detection cycle |
| ngAfterViewInit | after view (template) is rendered |
| ngOnDestroy | before component is removed |

## @Input and @Output
→ See [input.md](input.md) for @Input details

## ViewChild
```typescript
@ViewChild('myRef') myElement: ElementRef;

ngAfterViewInit() {
  console.log(this.myElement.nativeElement);
}
```

## Practice
- [ ] Create parent and child components
- [ ] Pass data using @Input
- [ ] Send event using @Output
- [ ] Try ngOnInit vs constructor
