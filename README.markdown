# High Speed Dirt

Sometimes you just want to zip through a large JDBC result set, one record at a time, with as little marshalling overhead as possible.

HSD provides a programmer-friendly, type-safe way to do this whilst minimising data copying and object creation/garbage collection.

HSD is *opinionated software*. Among the opinions it promotes are the following:

  * Query/command separation is a good idea.
  * Objects representing retrieved data should be *immutable* in most cases.
  * Use cursors where possible for batch processing of queries, instead of bulk-loading data into a collection of "persistable" objects.
  * In the batch processing case, reduce object creation / garbage collection by proxying a single adapter object to the underlying ResultSet 

Downloads
---------
HSD is distributed via maven central, and can be downloaded [here](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.youdevise%22%20a%3A%22hsd%22).

```xml
<dependency>
    <groupId>com.youdevise</groupId>
    <artifactId>hsd</artifactId>
    <version>0.3</version>
</dependency>
```

