# 09 - Pipes

## Built-in Pipes
```html
{{ name | uppercase }}               <!-- JOHN -->
{{ name | lowercase }}               <!-- john -->
{{ price | currency:'INR' }}         <!-- ₹1,000.00 -->
{{ today | date:'dd/MM/yyyy' }}      <!-- 25/03/2026 -->
{{ 3.14159 | number:'1.2-2' }}       <!-- 3.14 -->
{{ obj | json }}                     <!-- { "key": "value" } -->
{{ observable$ | async }}            <!-- auto subscribes/unsubscribes -->
```

## Custom Pipe
```bash
ng generate pipe truncate
```
```typescript
@Pipe({ name: 'truncate' })
export class TruncatePipe implements PipeTransform {
  transform(value: string, limit: number = 20): string {
    return value.length > limit ? value.substring(0, limit) + '...' : value;
  }
}
```
```html
{{ longText | truncate:30 }}
```

## async Pipe (important!)
- Automatically subscribes to Observable/Promise
- Automatically unsubscribes when component destroys (no memory leak)
```html
<p>{{ data$ | async }}</p>
<li *ngFor="let item of items$ | async">{{ item }}</li>
```

## Practice
- [ ] Use date and currency pipes
- [ ] Create a custom truncate pipe
- [ ] Use async pipe with an HTTP call
