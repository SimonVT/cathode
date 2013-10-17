import sys

path = './cathode/src/main/res/values/secrets.xml'
file = open(path, 'w');
file.write('<?xml version="1.0" encoding="utf-8"?>\n')
file.write('<resources>\n')
file.write('  <string name="apikey">')
file.write(sys.argv[1])
file.write('</string>\n')
file.write('  <string name="crashlytics_apikey">')
file.write(sys.argv[2])
file.write('</string>\n')
file.write('</resources>')