# JPA Auto-REST Example

This example shows how to configure a JPA based Spring Boot application for deployment on Cloud Foundry. The application will use an in memory H2 database when not on cloud foundry. This application uses Spring data REST support to create a default REST service with no coding.

Note that this project uses an unreleased version of SpringFox. The released version does not support Spring Data rest version 2.

When running on cloud foundry (the cloud profile), the `application-cloud.properties` file disables recreation of the database schema

If you want to recreate the database on Cloud Foundry, the application is known to work with a MySQL instance initialized with this table definition:

```sql
CREATE TABLE user (
  id int(11) unsigned NOT NULL AUTO_INCREMENT,
  first_name varchar(64) DEFAULT NULL,
  last_name varchar(64) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```
