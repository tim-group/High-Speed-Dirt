# High Speed Dirt

Sometimes you just want to zip through a large JDBC result set, one record at a time, with as little marshalling overhead as possible.

HSD provides a programmer-friendly, type-safe way to do this whilst minimising data copying and object creation/garbage collection.

HSD is *opinionated software*. Among the opinions it promotes are the following:

  * Separate data retrieval from creation / updating / deleting.
  * Objects representing retrieved data should be *immutable*.
  * Use cursors where possible for batch processing of queries, instead of bulk-loading data into memory.
  * In the batch processing case, reduce object creation / garbage collection by using the [flyweight pattern](http://en.wikipedia.org/wiki/Flyweight_pattern).
