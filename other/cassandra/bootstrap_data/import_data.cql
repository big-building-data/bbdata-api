use bbdata2;

copy raw_values from 'raw_values.csv' with header = true;

copy aggregations ("minutes", "object_id", "timestamp", "date", "last", "last_ts", "max", "sum", "mean", "count")
from 'aggregations_simple.csv' with header = true;

copy aggregations ("minutes", "object_id", "timestamp", "date", "last", "last_ts", "max", "sum", "mean", "count", "min", "std")
from 'aggregations_full.csv' with header = true;

exit;