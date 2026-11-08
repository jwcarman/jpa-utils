# Property Validation

## What is checked

The first argument to `Paginator.of` is the entity the paged queries return. When the paginator is built, every
sort enum constant's property and every tiebreaker property is checked against that type, using the same
property-path rules Spring Data applies when it runs the query, nested paths and inherited properties included.

```java
Paginator.of(MemberDirectoryEntry.class, MemberSort.class, cfg -> cfg.defaultSort(MemberSort.NAME));
```

Every constant is checked, not just the ones requests happen to use, so a typo in a rarely used sort option is
found as early as one in the default.

## What a failure looks like

A mistake fails when the paginator is built, with an `IllegalArgumentException` that names the constant, the path
and the entity:

```
MemberSort.ROLE sorts by "roleRnak", which is not a property of com.example.members.MemberDirectoryEntry
(use skipPropertyValidation() if the query sorts by something else)
```

A tiebreaker the entity doesn't have fails the same way. That includes the default `id` on an entity whose
identifier is named something else:

```
tiebreaker "id", which is not a property of com.example.members.MemberDirectoryEntry (...)
```

Without validation, the same mistake surfaces only when a query runs, as a `PropertyReferenceException` from every
request that uses the bad sort, which a web application turns into a 500.

## Where you'll see it

A paginator in a `static final` field is built when its class initializes, so a bad property fails the first
time the class is used:

- **In a test or at startup**, the error arrives wrapped: `ExceptionInInitializerError`, and in Spring
  `Error creating bean with name 'memberService'`. The `IllegalArgumentException` with the message above is under
  **Caused by**.
- **After that**, any further use of the class in the same JVM throws
  `NoClassDefFoundError: Could not initialize class ...`, which no longer shows the cause. Look for the first
  error in the log, not the last.

If you'd rather see the message directly, build the paginator in an instance field of a Spring bean instead. The
failure is then a `BeanInstantiationException` whose direct cause is the `IllegalArgumentException`:

```java
@Service
public class MemberService {

    private final Paginator members = Paginator.of(MemberDirectoryEntry.class, MemberSort.class);
    // ...
}
```

Either way, any test that touches the service catches the mistake.

## Opting out

Validation assumes the query sorts by entity properties. A native query doesn't: Spring Data passes the sort keys
through as column names (`display_name`), which aren't properties, so validation would reject correct code. For
those, opt out explicitly:

```java
Paginator.of(Member.class, MemberColumnSort.class, cfg -> cfg
        .tiebreaker("member_id")
        .skipPropertyValidation());
```
