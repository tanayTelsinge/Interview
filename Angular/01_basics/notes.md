# 01 - Angular Basics

## What is Angular?
- A TypeScript-based frontend framework by Google
- Component-based architecture
- Uses modules to organize code

## Setup
```bash
npm install -g @angular/cli
ng new my-app
cd my-app
ng serve
```
- What is Angular CLI - CLI tool to create + manage Angular project via terminal.
- Without it, we have to manually create folder structure, ts configs, add boilerplate code.
- with CLI, ng new my-app, ng g component, ng serve, ng build etc.
- Spring Initializer similar but for Angular.

## Project Structure

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

## Standalone Components (Angular 17+)
→ See [standalone_vs_modules.md](standalone_vs_modules.md)

## Key Concepts
- **Module** → groups related components/services (`@NgModule`)
- **Component** → building block of UI (`@Component`)
- **Template** → HTML with Angular syntax
- **Decorator** → metadata attached to a class (`@Component`, `@NgModule`)

## Angular vs React — Interview

| | Angular | React |
|---|---|---|
| Made by | Google | Meta |
| Type | Full Framework | UI Library |
| Language | TypeScript (mandatory) | JavaScript / TypeScript |
| Learning curve | Steep | Moderate |
| Data binding | Two-way ([(ngModel)]) | One-way |
| DOM | Real DOM | Virtual DOM |
| State management | NgRx / Services | Redux / Zustand / Context |
| Routing | Built-in (@angular/router) | External (react-router) |
| Forms | Built-in (Reactive + Template) | External (react-hook-form) |
| HTTP | Built-in (HttpClient) | External (axios / fetch) |
| Architecture | Opinionated (MVC-like) | Flexible (you decide) |

## When to use which?

**Use Angular when:**
- Large enterprise app with big team — strict structure prevents chaos across devs
- Need everything built-in (routing, forms, HTTP, DI) — no need to pick external libraries
- Building one unified SaaS portal consuming many microservices — module system mirrors service boundaries, built-in HTTP interceptors handle JWT across all service calls cleanly
- Internal tools, admin panels, enterprise SaaS (Jira, CRM, ERP, banking portals)

**Use React when:**
- Smaller team or startup — React is just a UI library, start small and add only what you need (routing? add react-router, state? add zustand, else skip)
- Customer-facing SaaS product — needs SEO (Next.js SSR), fast page load (React ~40KB vs Angular ~130KB+), frequent UI changes based on user feedback
- UI-heavy apps with frequent re-renders (real-time feeds, live charts) — Virtual DOM batches updates efficiently vs Angular rendering whole component tree
- Micro-frontends — each microservice can own its own React UI independently

## One-liner for interview
> "Angular is a complete framework — opinionated, batteries included, best for large enterprise SaaS portals consuming microservices.
> React is a UI library — flexible and lightweight, best for customer-facing products where SEO, fast load, and quick iterations matter."

## Steps:
 ```
 npm @angular/cli 
 ng new learn-angular
 cd learn-angular
 ng serve
 ```
