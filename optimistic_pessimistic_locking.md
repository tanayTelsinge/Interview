## OPTIMISTIC_LOCKING
- Throws exceptions if `@Version` is modified.

## PESSIMISTIC_LOCKING
- Waits till lock is removed.

## LockModeTypes in Java

### OPTIMISTIC
- Reads version (a get call), if during save (version is modified), throws exception. (updated version only if modified).

### OPTIMISTIC_FORCE_INCREMENT
- Increments version even if entity is not modified.

### PESSIMISTIC_READ
- Second transaction can read, only wait for modify.

### PESSIMISTIC_WRITE
- Second transaction needs to wait for read and modify.

## When to Use What?

### Contention
- Degree of competition between multiple threads or transactions for the same resource.

### Low Contention
- Low concurrent updates, mainly concurrent reads -> use optimistic locking.
  - **E-commerce product catalog**:
    - Thousands of users browsing products but only a few updating stock or prices.
  - **User profile management**:
    - Many users reading their profile, but only occasionally making changes.

### High Contention
- High concurrent updates -> use pessimistic locking.
  - **Online banking system**:
    - Multiple users transferring money from the same account simultaneously.
  - **Real-time stock trading platform**:
    - Many buy/sell orders for the same stock.
  - **Hotel booking system**:
    - Multiple users trying to book the same room simultaneously.
