# User Admin Server

[![Actions Status](https://github.com/gridsuite/user-admin-server/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/gridsuite/user-admin-server/actions)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=org.gridsuite%3Auser-admin-server&metric=coverage)](https://sonarcloud.io/component_measures?id=org.gridsuite%3Auser-admin-server&metric=coverage)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)

## Description

The **user-admin-server** is a microservice of the [GridSuite](https://github.com/gridsuite) platform dedicated to **user management, authorization, and resource governance**.

It provides the following capabilities:

- **User lifecycle management**: create, read, update, and delete users identified by their OIDC `sub` claim (the sub claim stands for "subject" and contains a unique identifier for the authenticated user)
- **Role-based access control**: distinguishes `admin` users from regular users; all sensitive operations require admin rights.
- **User profiles**: named profiles bundling references to parameter sets (LoadFlow, Security Analysis, Sensitivity Analysis, Short Circuit, Voltage Init, etc.) and per-profile operation quotas.
- **User groups**: create and manage groups of users with many-to-many relationships.
- **Resource quotas**: per-user limits on the number of concurrent in-flight operations across 13 operation types (cases, builds, load flow, security analysis, etc.), inherited from the user's assigned profile.
- **Connection tracking**: records and deduplicates user connection attempts (accepted or refused).
- **Announcements**: admins create time-windowed system-wide announcements; a scheduler detects active announcements and broadcasts them, and cleans up expired ones.
- **User identity enrichment**: fetches first name and last name from the `user-identity-server` to augment user listings.

---

## Technical Stack

- Spring Boot (Web, Data JPA, Actuator, Cloud Stream)
- PostgreSQL
- Liquibase
- RabbitMQ via Spring Cloud Stream
- ShedLock (JDBC-backed distributed scheduling)
- API documentation: OpenAPI / Swagger (`springdoc`)
- Micrometer / Prometheus

---

## Development Scripts

Build Docker image:

```shell
mvn install -DskipTests -Dpowsybl.docker.install
```

Please read [liquibase usage](https://github.com/powsybl/powsybl-parent/#liquibase-usage) for instructions to automatically generate changesets. After you generated a changeset do not forget to add it to git and in `src/resources/db/changelog/db.changelog-master.yml`.

---

## Interactions with Other Microservices

```
┌──────────────────────┐
│  user-admin-server   │──► user-identity-server  (fetch first name / last name for users)
│                      │──► directory-server       (validate element UUIDs referenced in profiles, count cases)
└──────────────────────┘
         ▼
      RabbitMQ (announcement / cancelAnnouncement)
```

---

## Announcement Broadcasting Flow

1. A scheduler (distributed via **ShedLock**) periodically checks for announcements whose time window has started.
2. When a new active announcement is detected, a message of type `announcement` is published on RabbitMQ with the severity, remaining duration, and message text.
3. When an admin deletes a currently active announcement, a `cancelAnnouncement` message is published.
4. The scheduler also cleans up expired announcements automatically.

---

## Resource Quota Enforcement

Each user is assigned a profile that defines maximum quota values per operation type. At runtime:

- **Record** an operation (`POST /users/{sub}/quota/{operation}/{operation_id}/start`).
- **Remove the record** of a given operation (`POST /users/{sub}/quota/{operation}/{operation_id}/end`).
- The current usage is persisted in the `user_operation` table; a unique constraint on `(sub, operation_id)` prevents duplicate entries.
- Quotas can be reset by an admin via `POST /users/{sub}/quota/reset`.

---

## Micrometer Observability

Spring Boot Actuator and the Micrometer Prometheus registry expose a `/actuator/prometheus` scrape endpoint for Prometheus, as well as standard health and info endpoints. The git commit metadata is embedded at build time and surfaced through `/actuator/info`.
