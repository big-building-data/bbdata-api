# JMeter stress test

## Running the test

1. download jmeter: https://jmeter.apache.org/download_jmeter.cgi
2. create the configuration file (see below)
3. run the test using the command below

```bash
jmeter -n -t BBData-api-input-stress-test -p local.properties -l results.jtl  -j meter.log
```

## Configuring the test

The test consists of creating `theads` threads, each associated with a different object id, and make a POST `/input/values`
request every `period` second `loops` times.

With BBData, this means we need some objects and tokens. Here is an example on how to proceed.

First, have a look at the MySQL database and find a range of object ids that is free. For example, 10-100.
Use the `gen_test_inserts.py` file to generate the INSERT statements for the test objects and tokens, which will have the 
form `[01234567890123456789a*<object_id>]`:
```bash
./gen_test_inserts.py -N0 10 -N1 100
```
Copy the output of the script and paste it into a mysql prompt (with the bbdata database selected of course).

Now, copy `local.properties` and set the properties as needed. For the example above, 
the *POST /objects/values properties* section will look like:
```properties
id_inc=9
# beginning of the token (fixed)
token_start=01234567890123456789
# padding: repeated n times between token_start and object_id so that the total length of the token is 32
token_pad=a
``` 
and we can configure up to 90 threads (object ids 10 to 100).

*Note*: `local.properties` is configured to work with this development version of BBData, running locally with test data.

## JMeter usage

Open jmeter in GUI in order to debug the test. It includes default values by defaut, in the *Test Plan* panel.
Those are all the values that can be overriden through a property file.

Once it works in the GUI (for debug only), we can run the test in non-GUI mode (option `-n`) and save the results
in a file (option `-l`, for listener).

After the test, relaunch jmeter in GUI mode, add some listeners and load the results file to see the results.
You can also generate a web report using:
```bash
jmeter -g results.jtl -o some_writable_dir
open some_writable_dir/index.html
```

