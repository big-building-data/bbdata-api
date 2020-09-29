# Monitoring with Prometheus + Grafana

This folder contains a docker compose to monitor BBData API instances using Prometheus + Grafana.

**IMPORTANT**: for this to work, the BBData API instances must be launched with the prometheus endpoint enabled (see [prerequisites](#prerequisites)).

- [Prerequisites](#prerequisites)
- [Setup and Run](#setup-and-run)
- [prometheus](#prometheus)
- [Grafana](#grafana)
    * [First use](#first-use)
    * [Tips](#tips)
- [Resources](#resources)


## Prerequisites

The BBData-API must expose all the metrics, as well as the `/prometheus` endpoint.
The best way to do so is to set the following properties:
```properties
# IMPORTANT: actuators may be dangerous, so ensure you run it on another port, that is only available to administrators !
# (in this case, the regular API runs on port 8080 and port 8111 is NOT available from the outside)

# enable ALL actuators and metrics
management.endpoints.web.exposure.include=*
management.endpoint.health.show-components=always
# run management interface on a different port
management.server.port=8111
# remove the default /actuator path prefix
management.endpoints.web.base-path=/
```

## Setup and Run

First, edit the file `config/prometheus.yml`:
* set the different target instances, that is the list of `<HOST|IP>:<PORT>` where your applications are running 
  (when development on Mac, use `host.docker.internal` to access bbdata-api running on localhost)
* change the scraping interval, if you want to

Once done, simply run from this directory the command:
```bash
docker-compose up -d
```

If you get an error of type **permission denied**, such as
```text
You may have issues with file permissions, [...]
mkdir: cannot create directory '/var/lib/grafana/plugins': Permission denied
```
look at the permissions of the `data` folder (chown if needed), and use the `user: 'sid'` directive in `docker-compose.yml`.
On linux, you can get you user sid by running `id -u`.

## prometheus

Prometheus can be accessed on port `9090`.


## Grafana

Grafana can be accessed on port `3000`.

### First use

* use default username/password: *admin/admin* (change it to admin/bbdata or whatever)
* add prometheus data source: *Configuration* > *Add data source* > *Prometheus*, and set URL=`http://prometheus:9090`
* add an open-source Spring Boot dashboard: *"+"* > *Import*: https://grafana.com/grafana/dashboards/10280
* add the custom BBData dashboard using the same import wizard and load (or copy-paste) the JSON file `bbdata-dashboard-grafana.json`

### Tips

For all metrics of type *summary* (such as `http_server_requests_seconds_*`), we have three sub counters at our disposal:
* `_count`: the total number of records
   For http requests, this means the total number of requests made to each endpoints;
* `_sum`: the sum of all the records made during a time window.
    For http requests, it means the total duration of every request for each endpoint;

We can work with those summaries in Prometheus/Graffana using common approaches:

* Average latency: `rate(timer_sum[10s])/rate(timer_count[10s])`
* Throughput (requests per second): `rate(timer_count[10s])`
* Count `increase(timer_count)`

For http_server_requests, we also have a `_max`: (gauge, optional) the maximum request during a time window. 
The value resets to 0 when a new time window starts. 

## Resources

* [SpringDoc: Prometheus](https://docs.spring.io/spring-metrics/docs/current/public/prometheus)
* [How does a Prometheus Counter work?](https://www.robustperception.io/how-does-a-prometheus-counter-work)
* [Spring Boot default metrics](https://tomgregory.com/spring-boot-default-metrics/)