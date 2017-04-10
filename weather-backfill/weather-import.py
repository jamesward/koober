#!/usr/bin/python

import requests
import csv
import time
import datetime
import sys
import getopt

cache = {}
def compareObs(ob1, ob2):
    if int(ob1['date']['hour']) > int(ob2['date']['hour']) or int(ob1['date']['hour']) == int(ob2['date']['hour']) and int(ob1['date']['min']) > int(ob2['date']['min']):
        return 1;
    elif int(ob1['date']['hour']) == int(ob2['date']['hour']) and int(ob1['date']['min']) == int(ob2['date']['min']):
        return 0;
    else:
        return -1;

def cmp_to_key(mycmp):
    'Convert a cmp= function into a key= function'
    class K(object):
        def __init__(self, obj, *args):
            self.obj = obj
        def __lt__(self, other):
            return mycmp(self.obj, other.obj) < 0
        def __gt__(self, other):
            return mycmp(self.obj, other.obj) > 0
        def __eq__(self, other):
            return mycmp(self.obj, other.obj) == 0
        def __le__(self, other):
            return mycmp(self.obj, other.obj) <= 0  
        def __ge__(self, other):
            return mycmp(self.obj, other.obj) >= 0
        def __ne__(self, other):
            return mycmp(self.obj, other.obj) != 0
    return K

def getWeatherData(pickup_hour, observations):
    for observation in observations:
        if observation['date']['hour'] == pickup_hour:
            fog = observation['fog']
            rain = observation['rain']
            snow = observation['snow']
            hail = observation['hail']
            thunder = observation['thunder']
            tornado = observation['tornado']
            heat = 0 if observation['heatindexm'] == '-9999' else observation['heatindexm']
            windchill = 0 if observation['windchillm'] == '-999' else observation['windchillm']
            precipitation = 0 if observation['precipm'] == '-9999.00' else observation['precipm']
            clear = '0'
            if fog == '0' and rain == '0' and snow == '0' and hail == '0' and thunder == '0' and tornado == '0':
                clear = '1'
            return {'temp': observation['tempm'], 'clear':clear, 'fog':fog, 'rain':rain, 'snow':snow, 'hail':hail, 'thunder':thunder, 'tornado':tornado, 
            'heat':heat, 'windchill':windchill, 'precipitation':precipitation}

def import_weather_data(inputfile, outputfile):
    with open(inputfile, newline='') as csvfile:
        datareader = csv.DictReader(csvfile)
        # row_count = sum(1 for row in datareader)
        headers = datareader.fieldnames
        headers.extend(['temp', 'clear', 'fog', 'rain', 'snow', 'hail', 'thunder', 'tornado', 'heat', 'windchill', 'precipitation'])
        with open(outputfile, 'w') as csvfile:
            outputfilewriter = csv.DictWriter(csvfile, delimiter=',', quotechar='|', fieldnames=headers)
            # print(headers)
            outputfilewriter.writeheader()
            for row in datareader:
                if datareader.line_num % 10000 == 0:
                    print('line number: ' + str(datareader.line_num))
                pickup_datetime = datetime.datetime.strptime(row['tpep_pickup_datetime'], "%Y-%m-%d %H:%M:%S");
                pickup_date = pickup_datetime.strftime('%Y%m%d')
                pickup_hour = pickup_datetime.strftime('%H')
                # print(pickup_date)
                if pickup_date not in cache:
                    url = "http://api.wunderground.com/api/c05d3c35081ff3d9/history_" + pickup_date + "/q/NY/New_York.json"
                    # print(url)
                    r = requests.get(url)
                    response_json = r.json()
                    observations = response_json['history']['observations']
                    cache[pickup_date] = sorted(observations, key=cmp_to_key(compareObs))
                weather_data = getWeatherData(pickup_hour, cache[pickup_date])
                row.update(weather_data)
                # print(row)
                outputfilewriter.writerow(row)

def main(argv):
   inputfile = ''
   outputfile = ''
   try:
      opts, args = getopt.getopt(argv,"hi:o:",["ifile=","ofile="])
   except getopt.GetoptError:
      print('WeatherImport.py -i <inputfile> -o <outputfile>')
      sys.exit(2)
   for opt, arg in opts:
      if opt == '-h':
         print('WeatherImport.py -i <inputfile> -o <outputfile>')
         sys.exit()
      elif opt in ("-i", "--ifile"):
         inputfile = arg
      elif opt in ("-o", "--ofile"):
         outputfile = arg
   print('Input file is "', inputfile)
   print('Output file is "', outputfile)
   import_weather_data(inputfile, outputfile)

main(sys.argv[1:])