"""
Import sample data for classification engine
"""

import argparse
import csv

import predictionio


def import_events(clt, file_name):
    print "Importing data..."
    with open(file_name) as csvFile:
        reader = csv.DictReader(csvFile)
        reader.fieldnames[0] = 'timestamp'

        count = 0
        for row in reader:
            timestamp = row[reader.fieldnames[0]]
            for field in reader.fieldnames[1:]:
                if row[field] != '' and float(row[field]) >= 0:
                    clt.create_event(
                        event="$set",
                        entity_type="energy_consumption",
                        entity_id=str(count),
                        properties={
                            "circuit_id": int(field),
                            "timestamp": int(timestamp),
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
