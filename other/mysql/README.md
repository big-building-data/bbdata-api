# Dev setup

Given a running mysql instance in localhost (with no root password), here are the steps to follow.

## Create user

```mysql
create user 'bbdata-admin'@'%' identified by 'bbdata';
grant all privileges on bbdata2.* to 'bbdata-admin'@'%';
flush privileges;
```

## Create schema & add test data

```bash
# create schema
mysql -u root < bbdata-structure.sql
# insert data
mysql -u root bbdata2 < test-data.sql
```