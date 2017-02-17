import pandas as pd
import numpy as np
import random
from datetime import datetime, timedelta as td

region_latlon_base = range(10, 60, 10)
index = """,RateCodeID,RatecodeID,VendorID,dropoff_latitude,dropoff_longitude,extra,fare_amount,improvement_surcharge,mta_tax,passenger_count,payment_type,pickup_latitude,pickup_longitude,store_and_fwd_flag,tip_amount,tolls_amount,total_amount,tpep_dropoff_datetime,tpep_pickup_datetime,trip_distance""".split(",")
reverse_idx = {v: k for k, v in dict(enumerate(index)).iteritems()}
print reverse_idx

def generate_latlon(base):
    dec_lat = random.random() / 10
    dec_lon = random.random() / 10
    base = region_latlon_base[base]
    return ('%.15f' % (base + dec_lat)), ('%.15f' % (base + dec_lon))

def two_rand(s):
    c1 = random.randint(0, s-1)
    c2 = random.randint(0, s-1)
    if c1 != c2:
        return c1, c2
    else: return two_rand(s)

def generate_record(pickup_time, latlon_base):
    template = """9827511,1.0,,1,40.7645454407,-73.9659423828,0.0,6.0,0.0,0.5,1,1,40.7550506592,-73.96534729,N,1.35,0.0,8.15,2015-01-03 13:56:32,2015-01-03 13:50:31,0.8""".split(",")
    delta = td(minutes=random.randint(0, 3600 * 24 - 30))

    pickup_time = pickup_time + delta

    template[reverse_idx['tpep_pickup_datetime']] = pickup_time.strftime('%Y-%m-%d %H:%M:%S')
    pickup_lat, pickup_lon = generate_latlon(latlon_base)
    template[reverse_idx['pickup_longitude']] = pickup_lon
    template[reverse_idx['pickup_latitude']] = pickup_lat
    return template

"""
Assuming:
    1. Each day d has 2d rides random uniformly, each d rides classified in one cluster
    2. Only contains data from the year of 2015
"""

d = datetime(2015, 1, 1, 0, 0, 0)
series = []

while d < datetime(2016, 1, 1, 0, 0, 0):
    day = d.day
    c1, c2 = two_rand(len(region_latlon_base))
    for dd in range(day):
        series.append(generate_record(d, c1))
        series.append(generate_record(d, c2))

    d = d + td(days=1)
data = np.array(series)
data = pd.DataFrame(data=data, columns=index)
data.to_csv('data/fake.csv', index=False)
