Program: v2_updated_since.py

This program downloads the locations updated since a provided date/time into
a CSV file that can be easily consumed by other programs. It requires a
client ID, signing key and the 'updated since' date/time. All other
parameters are optional.

As the program finishes, it outputs a line including the appropriate date/time
to use in the next run of the program. Capture that date/time and save it to
get only the new updates on the next call.

This program requires Python version 2.6 or 2.7 and runs on Linux, OS X or
Windows.

For usage:

  python v2_updated_since.py -h


Basic usage with output redirected to a file:

  python v2_updated_since.py -c cqgqdiwm4861uty3kbolywc1h -s uF13lWeVxPZafai9D5i_nEOpbScUhTUfu426Yk7qXrA -l -u "2013-01-08" > output.csv


Basic usage with an output file defined in the command parameters:

  python v2_updated_since.py -c YOUR_CLIENT_ID -s YOUR_SIGNATURE_KEY -l -u "2012-01-08" -o output.csv


Limiting changes to a set of zip codes:

  python v2_updated_since.py -c cqgqdiwm4861uty3kbolywc1h -s uF13lWeVxPZafai9D5i_nEOpbScUhTUfu426Yk7qXrA -l -u "2012-01-08" -q "94618" > output.csv

Limiting changes to a set of two-letter state abbreviations:

python v2_updated_since.py -c cqgqdiwm4861uty3kbolywc1h -s uF13lWeVxPZafai9D5i_nEOpbScUhTUfu426Yk7qXrA -l -u "2012-01-01" -q "CA" > output.csv
  
linked_status column: indicates whether or not the location is Owner Verified. True = Owner Verified.
businessType column: type of business
publishedAt column: the date the location's menu was published in the SP system. For those publishers with real-time integrations that are looking only for new location matches need only to match against those locations that were published since the date this script was last run.
