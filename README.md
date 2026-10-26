# Apparat

A web application for booking shared laboratory instruments. Two people can
never hold the same instrument at the same time, even when they click at the
same moment.

Built with plain Java: Servlets and JSP, JDBC and MySQL. No framework.

## What it does

- Browse instruments, see which half-hour slots are free, and book one.
- Cancel your own bookings; download them as CSV.
- Staff manage instruments (add, edit, activate, import from CSV), schedule
  maintenance windows, and read a utilization report.
- Administrators manage user accounts.
- What you may do depends on your role (see below).

## Roles

| | Student | Faculty | Technician | Administrator |
|---|:-:|:-:|:-:|:-:|
| Browse, check availability, book, cancel own | yes | yes | yes | yes |
| Longest single booking | 2 h | 4 h | 6 h | no limit |
| Active bookings at once | 3 | 6 | 10 | no limit |
| Manage instruments, import CSV | | | yes | yes |
| Maintenance windows | | | yes | yes |
| Utilization report and CSV | | | yes | yes |
| Create users, change roles, reset passwords, deactivate | | | | yes |

Where it lives in the code:

- `model/Role.java` and `model/Permission.java`: the one table of who may do what.
- `filter/AccessRules.java` and `filter/AuthFilter.java`: which page needs which
  permission. Unlisted pages under `/manage` and `/admin` are administrator-only.
- `policy/`: the booking limits. `BookingPolicy` is abstract; each role is a small
  subclass that supplies its own numbers.
- A user's role and active flag are re-read from the database on every page
  request, so a change by an administrator applies to the user's very next click.

## Demo accounts

These are sample data for development only. They are created by `database/setup.sql`.

| Email | Password | Role |
|---|---|---|
| student@apparat.test | Student@123 | Student |
| faculty@apparat.test | Faculty@123 | Faculty |
| tech@apparat.test | Tech@123 | Technician |
| admin@apparat.test | Admin@123 | Administrator |

## Requirements

- Java 21
- Maven 3.9
- MySQL 5.7 or newer (developed on 5.7.24), by default on `127.0.0.1:3308`
- Apache Tomcat 11 (Jakarta Servlet 6)

## Set up the database

```bash
./database/setup-db.sh
```

This creates two databases and fills each with `database/setup.sql`:

- `apparat2`, the one the application uses
- `apparat2_test`, used only by the tests

`setup.sql` **drops and recreates all tables**, so the script asks before it
runs. Do not run it on a database whose data you want to keep.

MySQL connection settings for the script: `MYSQL_HOST`, `MYSQL_PORT`,
`MYSQL_USER`, `MYSQL_PWD`.

To point the application at a different database, start Tomcat with Java
system properties:

```
-Dapparat.db.url=jdbc:mysql://127.0.0.1:3308/apparat2
-Dapparat.db.user=root
-Dapparat.db.password=
```

## Run the application

In IntelliJ: create a *Tomcat Server, Local* run configuration, add the
`apparat2:war exploded` artifact, set the application context to `/apparat2`,
and use JDK 21. Then open <http://localhost:8080/apparat2/>.

Or from the command line:

```bash
mvn package
cp target/apparat2-1.0-SNAPSHOT.war "$CATALINA_HOME/webapps/apparat2.war"
"$CATALINA_HOME/bin/catalina.sh" run
```

## Run the tests

```bash
mvn test
```

The tests use the real database, so they run against `apparat2_test` (set in
`pom.xml`) and never touch `apparat2`. A guard stops any test that would run
against a database whose name does not end in `_test`.

If you run a test from the IDE instead of Maven, add this VM option to the run
configuration:

```
-Dapparat.db.url=jdbc:mysql://127.0.0.1:3308/apparat2_test
```

## How double booking is prevented

1. Every booking is split into 30-minute slots stored in `booking_slots`, whose
   primary key is `(resource_id, slot_start)`. The database refuses a second
   booking for the same slot. This is the final guarantee.
2. `createBooking` runs in one transaction and first locks the instrument's row
   (`SELECT ... FOR UPDATE`), so requests for the same instrument take turns.
3. Maintenance windows lock the same row, so a window and a booking can never
   both be saved for the same time.
4. A user's row is locked while their booking limits are checked, so one person
   cannot exceed the limit by booking several instruments at the same instant.

The tests include a 50-thread race for one slot, a stress test with
overlapping windows, a window-against-bookings race, and a limit race.

## Project layout

```
src/main/java/com/apparat/
  model/     Role, Permission, User, Resource, Booking, MaintenanceWindow, statuses
  policy/    BookingPolicy and one subclass per role
  filter/    AuthFilter, AccessRules, SessionSupport
  servlet/   one servlet per page; BaseServlet knows who is logged in
  service/   business rules and typed exceptions
  dao/       all SQL
  dto/       small records passed to the pages
  util/      password hashing, CSV
  config/    database connection
src/main/webapp/   css, js, WEB-INF/views (JSP pages), web.xml
src/test/java/     unit tests and tests against apparat2_test
database/          setup.sql and setup-db.sh
docs/design/       design guides (PDF) and the script that builds them
```

## Known limits

- No CSRF tokens on forms, and no limit on login attempts.
- Maintenance windows cover part of one day; a longer outage uses the
  instrument's INACTIVE status. A window cannot be edited, only removed.
- The "time has already passed" rule is checked in the booking page, not in the
  service.
- Booking has no approval step, and there is no registration page; an
  administrator creates accounts.
