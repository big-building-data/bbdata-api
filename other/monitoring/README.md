# Monitoring with Prometheus + Grafana

This folder contains a docker compose to monitor BBData API instances using Prometheus + Grafana.

**IMPORTANT**: for this to work, the BBData API instances must be launched with the property `management.endpoint.prometheus.enabled=true`.

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
