# JPA Example

This example shows how to configure a JPA based Spring Boot application for deployment on Cloud Foundry. The application will use an in memory H2 database when not on cloud foundry.

The main differences are as follows:

1. The `CloudConfig` class is the magic connector to Cloud Foundry - this class will connect MyBatis to a Cloud Foundry DB service
2. When running locally, the `application-default.properties` file configures a local H2 database

If you want to recreate the database on Cloud Foundry, the application is known to work with a MySQL instance initialized with this table definition:

```sql
CREATE TABLE users (
  id int(11) unsigned NOT NULL AUTO_INCREMENT,
  first_name varchar(64) DEFAULT NULL,
  last_name varchar(64) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8;
```
