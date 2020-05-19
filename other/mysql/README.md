# Dev setup local

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
mysql -u root < bbdata2-structure.sql
# insert data
mysql -u root bbdata2 < test-data.sql
```

# Dev setup docker

__Important__: if you change the structure or test data (`*.sql`), you need to rebuild the image !

Build the image:
```bash
docker build -t bbdata-mysql .
``` 

Launch the image:
```bash
docker run -p 3306:3306 --rm --name bbsql bbdata-mysql
```

Connect:
```bash
mysql -h 127.0.0.1 -u bbdata-admin --password=bbdata bbdata2
```