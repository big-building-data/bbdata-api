copy raw_values from '/bootstrap_data/raw_values.csv' with header = true;

copy aggregations ("minutes", "object_id", "timestamp", "date", "last", "last_ts", "max", "sum", "mean", "count")
from '/bootstrap_data/aggregations_simple.csv' with header = true;

copy aggregations ("minutes", "object_id", "timestamp", "date", "last", "last_ts", "max", "sum", "mean", "count", "min", "std")
from '/bootstrap_data/aggregations_full.csv' with header = true;


