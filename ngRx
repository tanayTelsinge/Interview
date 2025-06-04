# 📚 NgRx Notes for Angular

NgRx is a reactive state management library for Angular, inspired by Redux.

---

## ⚙️ Core Concepts

| Concept      | Description                                                                 |
|--------------|-----------------------------------------------------------------------------|
| **Store**    | Holds the application state as a single immutable object                    |
| **Action**   | Describes an event (e.g. user clicks, API call started/succeeded/failed)    |
| **Reducer**  | Pure function to update state based on an action                            |
| **Selector** | Function to query a slice of state                                          |
| **Effect**   | Handles side effects like HTTP requests, uses `Actions` stream              |

---

## 📦 Packages

```bash
ng add @ngrx/store
ng add @ngrx/effects
ng add @ngrx/store-devtools

### NgRx flow

Component
   ⬇ dispatch(action)
Effect (optional side effects)
   ⬇
Reducer
   ⬇
Store (state updated)
   ⬇
Component (subscribes via select)


### Example

When the component loads, it:

Dispatches loadBill({ reservationId: 101 })

Effect calls /api/reservations/101/bill

On success, dispatches loadBillSuccess → reducer updates store

Component selects and displays bill info



```
src/
├── app/
│   ├── reservation/
│   │   ├── bill.model.ts
│   │   ├── bill.actions.ts
│   │   ├── bill.reducer.ts
│   │   ├── bill.effects.ts
│   │   ├── bill.service.ts
│   │   └── bill.component.ts

```

## bill.model.ts

``` ts
export interface ReservationBill {
  reservationId: number;
  customerName: string;
  amount: number;
  status: 'PAID' | 'PENDING' | 'CANCELLED';
}

```

## bill.actions.ts

```
import { createAction, props } from '@ngrx/store';
import { ReservationBill } from './bill.model';

export const loadBill = createAction(
  '[Reservation] Load Bill',
  props<{ reservationId: number }>()
);

export const loadBillSuccess = createAction(
  '[Reservation] Load Bill Success',
  props<{ bill: ReservationBill }>()
);

export const loadBillFailure = createAction(
  '[Reservation] Load Bill Failure',
  props<{ error: any }>()
);

```


## bill.reducer.ts
```
import { createReducer, on } from '@ngrx/store';
import * as BillActions from './bill.actions';
import { ReservationBill } from './bill.model';

export interface BillState {
  bill: ReservationBill | null;
  loading: boolean;
  error: any;
}

export const initialState: BillState = {
  bill: null,
  loading: false,
  error: null
};

export const billReducer = createReducer(
  initialState,
  on(BillActions.loadBill, state => ({ ...state, loading: true })),
  on(BillActions.loadBillSuccess, (state, { bill }) => ({ ...state, bill, loading: false })),
  on(BillActions.loadBillFailure, (state, { error }) => ({ ...state, error, loading: false }))
);


```

## bill.effects.ts

```
import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { ReservationService } from './bill.service';
import * as BillActions from './bill.actions';
import { catchError, map, mergeMap } from 'rxjs/operators';
import { of } from 'rxjs';

@Injectable()
export class BillEffects {
  constructor(private actions$: Actions, private reservationService: ReservationService) {}

  loadBill$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BillActions.loadBill),
      mergeMap(action =>
        this.reservationService.getBill(action.reservationId).pipe(
          map(bill => BillActions.loadBillSuccess({ bill })),
          catchError(error => of(BillActions.loadBillFailure({ error })))
        )
      )
    )
  );
}


```

## bill.service.ts

```

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ReservationBill } from './bill.model';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ReservationService {
  constructor(private http: HttpClient) {}

  getBill(reservationId: number): Observable<ReservationBill> {
    return this.http.get<ReservationBill>(`/api/reservations/${reservationId}/bill`);
  }
}

```

## bill.component.ts
```

import { Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import * as BillActions from './bill.actions';
import { Observable } from 'rxjs';
import { ReservationBill } from './bill.model';

@Component({
  selector: 'app-bill',
  template: `
    <div *ngIf="loading$ | async">Loading bill...</div>
    <div *ngIf="bill$ | async as bill">
      <h3>Reservation Bill #{{ bill.reservationId }}</h3>
      <p>Customer: {{ bill.customerName }}</p>
      <p>Amount: ₹{{ bill.amount }}</p>
      <p>Status: {{ bill.status }}</p>
    </div>
  `
})
export class BillComponent implements OnInit {
  bill$: Observable<ReservationBill | null> = this.store.select(state => state.bill.bill);
  loading$: Observable<boolean> = this.store.select(state => state.bill.loading);

  constructor(private store: Store) {}

  ngOnInit() {
    this.store.dispatch(BillActions.loadBill({ reservationId: 101 }));
  }
}


```

## Register in app module

```
StoreModule.forRoot({ bill: billReducer }),
EffectsModule.forRoot([BillEffects])

```

RxJS and NgRx: The Connection
NgRx is built on RxJS Observables — the state store, actions, and effects all use streams of data (Observables).

Components subscribe to Observables to get state updates reactively.

Actions and side-effects are processed as streams, allowing powerful operators to transform, filter, and handle asynchronous data.

Key RxJS Concepts Used in NgRx
RxJS Concept	Role in NgRx	Example Operators Used
Observable	Stream of values (actions, state)	store.select() returns an Observable
Subject	Special Observable to emit events	Actions stream (actions$) in Effects
Operators	Transform/handle streams	map, mergeMap, switchMap, catchError
Subscription	Listening to an Observable	Components subscribe to store.select()

How RxJS Powers NgRx Effects
Effects listen to the actions$ Observable stream.

When a specific action is dispatched (ofType operator filters them), Effects trigger side effects like HTTP calls.

The result is mapped to success/failure actions.

The whole chain uses RxJS operators like:

ts
Copy
Edit
this.actions$.pipe(
  ofType(loadUsers),
  mergeMap(() => http.get('/api/users').pipe(
    map(users => loadUsersSuccess({ users })),
    catchError(error => of(loadUsersFailure({ error })))
  ))
);
Example RxJS Operators in NgRx
ofType() — filters action stream for specific actions

mergeMap() — maps to inner Observable (e.g., HTTP call), flattens the result

switchMap() — switches to new Observable, cancels previous

map() — transforms emitted values

catchError() — handles errors and returns fallback Observable

Why RxJS?
Asynchronous handling: easy to handle async events like HTTP calls, user inputs.

Composability: complex data flows can be created with simple operators.

Reactive UI updates: components react automatically to state changes.


