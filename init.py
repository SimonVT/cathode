import sys

secretsPath = './cathode/src/main/res/values/secrets.xml'
manifestPath = './cathode/src/release/AndroidManifest.xml'

traktApiKey = sys.argv[1];

if len(sys.argv) > 2:
  crashlyticsApiKey = sys.argv[2];
else:
  crashlyticsApiKey = '0000000000000000000000000000000000000000';

file = open(secretsPath, 'w');
file.write('<?xml version="1.0" encoding="utf-8"?>\n')
file.write('<resources>\n')
file.write('  <string name="apikey">')
file.write(sys.argv[1])
file.write('</string>\n')
file.write('</resources>')
file.close();

file = open(manifestPath, 'w');
file.write('<?xml version="1.0" encoding="utf-8"?>\n');
file.write('<manifest xmlns:android="http://schemas.android.com/apk/res/android"\n');
file.write('    package="net.simonvt.cathode">\n');
file.write('  <application>\n');
file.write('    <meta-data\n');
file.write('        android:name="com.crashlytics.ApiKey"\n');
file.write('        android:value="');
file.write(crashlyticsApiKey)
file.write('"/>\n');
file.write('  </application>\n');
file.write('</manifest>\n');

