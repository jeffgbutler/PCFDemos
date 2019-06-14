# MyBatis Example

This example shows how to configure a MyBatis based Spring Boot application for deployment on Cloud Foundry. The application will use an in memory H2 database when not on cloud foundry.

When running locally, the `application-default.properties` file enables the H2 platform so that the `schema-h2.sql` and `data-h2.sql` files will be executed on startup

If you want to recreate the database on Cloud Foundry, the application is known to work with a MySQL instance initialized with this table definition:

```sql
CREATE TABLE user (
  id int(11) unsigned NOT NULL AUTO_INCREMENT,
  first_name varchar(64) DEFAULT NULL,
  last_name varchar(64) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```
