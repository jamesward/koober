"""
Import sample data for classification engine
"""

import argparse
import csv
import datetime as datetime

import predictionio


def to_timestamp(year, month, day, hour, epoch=datetime.datetime(1970, 1, 1)):
    dt = datetime.datetime(year, month, day, hour)
    td = dt - epoch
    return int((td.microseconds + (td.seconds + td.days * 86400) * 10**6) / 10**6)


def import_events(clt, file_name):
    print "Importing data..."
    with open(file_name) as csvFile:
        reader = csv.DictReader(csvFile)

        count = 0
        for row in reader:
            zone_id = int(row[reader.fieldnames[0]])
            year = int(row[reader.fieldnames[1]])
            month = int(row[reader.fieldnames[2]])
            day = int(row[reader.fieldnames[3]])

            for field in reader.fieldnames[4:]:
                hour = int(field[1:]) - 1
                if row[field] != '' and float(row[field]) >= 0:
                    clt.create_event(
                        event="$set",
                        entity_type="energy_consumption",
                        entity_id=str(count),
                        properties={
                            "circuit_id": zone_id,
                            "timestamp": to_timestamp(year, month, day, hour),
                            "energy_consumption": float(row[field])
                        }
                    )
                    count += 1
            print "%s events are imported." % count
    print "ALL %s events are imported." % count


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description="Import sample data for classification engine")
    parser.add_argument('--access_key', default='invald_access_key')
    parser.add_argument('--url', default="http://localhost:7070")
    parser.add_argument('--file', default="./data/data.csv")

    args = parser.parse_args()
    print args

    client = predictionio.EventClient(
        access_key=args.access_key,
        url=args.url,
        threads=5,
        qsize=500)
    import_events(client, args.file)
