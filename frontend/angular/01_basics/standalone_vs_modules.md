# Standalone Components vs app.module.ts

## Project Structure Comparison

**Angular < 17 (with modules):**
```
src/app/
  app.component.ts      ← root component
  app.component.html
  app.component.css
  app.module.ts         ← had to register every component here
```

**Angular 17+ (standalone — current):**
```
src/app/
  app.ts                ← root component (self-contained)
  app.html
  app.css
  app.routes.ts         ← routing config
  // NO app.module.ts
```

## Problems with Modules

- Forgot to declare component → `Error: 'app-user' is not a known element` — confusing error
- One `app.module.ts` grew to 100+ lines in large apps — hard to maintain, constant merge conflicts
- Lazy loading required a whole separate module file just for one page
- Entire app shared one module — hard to tree-shake unused code

## What Standalone Solved

- Each component imports only what it needs directly — no central module file
- Better tree-shaking → smaller bundle size
- Easier lazy loading per component

## Code Comparison

```typescript
// OLD — had to register in app.module.ts
@NgModule({
  declarations: [AppComponent, UserComponent],
  imports: [BrowserModule, HttpClientModule]
})

// NEW — component is self-contained
@Component({
  imports: [RouterOutlet, HttpClientModule]  // imports right here
})
```

## Tree-shaking
= removing unused code from final bundle before shipping to browser
- Each component declares exactly what it uses → Angular cuts everything else
- Smaller bundle = faster page load
- Old modules made it hard to tell what was used where → standalone makes it precise
- Java analogy: Maven bundles entire library even if you use one class. Tree-shaking only packages what you actually call.

## One-liner for interview
> "Standalone components make each component self-sufficient — no module registration needed, better tree-shaking, less boilerplate. Solved the god-module problem in large Angular apps."

> Like Spring Boot — earlier registered every bean in XML, now `@Component` + `@Autowired` handles it directly.
