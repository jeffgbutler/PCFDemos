# JPA Auto-REST Example

This example shows how to configure a JPA based Spring Boot application for deployment on Cloud Foundry. The application will use an in memory H2 database when not on cloud foundry. This application uses Spring data REST support to create a default REST service with no coding.

The main differences are as follows:

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
