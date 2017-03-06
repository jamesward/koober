import pandas as pd
import numpy as np
import deepdish as dd

DATA_PATH_TEMPLATE = 'data/yellow_tripdata_2015-%02d.csv'
OUTPUT_PATH_TEMPLATE = 'data/yellow_tripdata_2015-%02d.pkl'
SAMPLE_PATH_TEMPLATE = 'data/yellow_tripdata_2015-%02d_sample.pkl'
CONCATTED_PATH = 'data/yellow_tripdata_2015_all_sample.pkl'
CONCATTED_PATH_CSV = 'data/yellow_tripdata_2015_all_sample.csv'
FURTHER_CONCAT_CSV = 'data/yellow_tripdata_2015_further_sample.csv'

def convert_data():
    for i in range(1, 13):
        print "Reading %d" % i
        data = pd.read_csv(DATA_PATH_TEMPLATE % (i), engine='c')
        # import pdb; pdb.set_trace()
        print type(data)
        dd.io.save(OUTPUT_PATH_TEMPLATE % (i), data)

def sample_data(data_id, percentage = 0.001):
    print "Loading data %d" % data_id
    data = dd.io.load(OUTPUT_PATH_TEMPLATE % (data_id))
    print "Sampling data %d" % data_id
    sample = data.sample(int(data.size * percentage))
    print "Saving data %d" % data_id
    dd.io.save(SAMPLE_PATH_TEMPLATE % (data_id), sample)

def combine_samples():
    datas = [dd.io.load(SAMPLE_PATH_TEMPLATE % (i)) for i in range(1, 13)]
    for i in datas:
        print i.size
    result = pd.concat(datas)
    print result.size
    dd.io.save(CONCATTED_PATH, result)

if __name__ == "__main__":
    # convert_data()
    # for i in range(1, 13):
    #     sample_data(i, 0.0001)
    # combine_samples()
    data = dd.io.load(CONCATTED_PATH)
    sample = data.sample(10000)
    sample.to_csv(FURTHER_CONCAT_CSV)
