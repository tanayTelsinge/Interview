# 08 - HTTP Client

## Setup
```typescript
// app.module.ts
import { HttpClientModule } from '@angular/common/http';
imports: [HttpClientModule]
```

## Basic CRUD in Service
```typescript
@Injectable({ providedIn: 'root' })
export class ApiService {
  private url = 'https://jsonplaceholder.typicode.com/posts';

  constructor(private http: HttpClient) {}

  getPosts() {
    return this.http.get(this.url);              // GET
  }
  createPost(data: any) {
    return this.http.post(this.url, data);       // POST
  }
  updatePost(id: number, data: any) {
    return this.http.put(`${this.url}/${id}`, data);  // PUT
  }
  deletePost(id: number) {
    return this.http.delete(`${this.url}/${id}`);     // DELETE
  }
}
```

## Using in Component
```typescript
ngOnInit() {
  this.apiService.getPosts().subscribe({
    next: (data) => this.posts = data,
    error: (err) => console.error(err)
  });
}
```

## HTTP Interceptor (add token to every request)
```typescript
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler) {
    const cloned = req.clone({
      headers: req.headers.set('Authorization', 'Bearer ' + token)
    });
    return next.handle(cloned);
  }
}
```

## Practice
- [ ] Fetch posts from jsonplaceholder API
- [ ] Display in template
- [ ] Handle loading and error states
- [ ] Create an interceptor that adds a header
