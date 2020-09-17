# JMeter stress test

* [Requirements](#requirements)
* [Overview](#overview)
* [Setup test objects](#setup-test-objects)
* [Configure the test](#configure-the-test)
* [Run the test & view the results](#run-the-test---view-the-results)
* [Change the test plan](#change-the-test-plan)

## Requirements

* jmeter: https://jmeter.apache.org/download_jmeter.cgi
* access to the MySQL db of the BBData instance under test

## Overview

The jmeter *Test Plan* defined in `BBData-api-stress-test.jmx` have three thread groups that concurrently 
hit three endpoints of the API for a specified amount of time:

* *input*: submit new values to the API
* *info*: GET /info
* *get latest*: request the latest value for an object

Each thread group can have arbitrary number of threads, however each thread uses a different object_id, 
whose id and tokens are computed based on the thread id in a predefined way. This means that if you want to run
80 threads for the *input* group, you need 80 objects in BBData. See "generate test data" for more info.

## Setup test objects

In the test plan, each thread submits values for a different object_id. This is to ensure there are no clashes in
the timestamps. Hence, if you want 80 threads, you need to create 80 "test objects" and 80 tokens.

To simplify this process, use the script `gen_test_inserts.py -n0 X -n1 Y` and copy-paste its output into the MySQL repl.
`n0` is the first object_id, `n1` the last. Look at the ids of the existing object_id in your database to set `n0`.
Note that by default, this script will generate tokens in the form `[01234567890123456789a*<object_id>]`. 

## Configure the test

The configuration is done through a property file. The default values are set so the test plan can run against a local
API instance using the test data.

The file `local.properties` lists all the available properties, along with expensive comments.

If you generated the test objects using the script `gen_test_inserts.py`, ensure that the following properties are set:
```properties
id_inc=<n0 - 1>
token_start=01234567890123456789
token_pad=a
```

## Run the test & view the results

```bash
# run the test
jmeter -n -t BBData-api-stress-test -p local.properties -l results.jtl  -j meter.log
# generate the HTML report
jmeter -g results.jtl -o report
# open the report
open report/index.html
```

## Change the test plan

To inspect and/or modify the test plan, run jmeter in gui mode (`jmeter`), then File > Open > BBData-api-stress-test.jmx