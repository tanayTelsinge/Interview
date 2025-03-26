
OPTMISTIC_LOCKING - throws exceptions if @Version is modified.
PESSIMISTIC_LOCKING - waits till lock is removed.

LockModeTypes in java:
OPTIMISTIC - reads version (a get call), if during save (version is modified), throws exception. (updated version only in modified).
OPTIMISTIC_FORCE_INCREMENT - increments version even if entity is not modified.
PESSIMISTIC_READ - second tx can read, only wait for modify.
PESSIMISTIC_WRITE - second tx needs to wait for read and modify.

When to use what?
Contention - degree of competition between multiple threads or transactions for the same resource.
Low contention - low concurrent updates, mainly concurrent reads -> use optimistic locking.
E-commerce product catalog:
Thousands of users browsing products but only a few updating stock or prices.
User profile management:
Many users reading their profile, but only occasionally making changes.

High contention - high concurrent updates -> use pessimistic locking.
Online banking system:
Multiple users transferring money from the same account simultaneously.
Real-time stock trading platform:
Many buy/sell orders for the same stock.
Hotel booking system:
Multiple users trying to book the same room simultaneously.
