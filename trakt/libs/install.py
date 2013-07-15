import os

os.system('mvn install:install-file -Dfile=android-support-v4.jar -DgroupId=com.google.android -DartifactId=support-v4 -Dversion=r13 -Dpackaging=jar')
os.system('mvn install:install-file -Dfile=dashclock-api-r2.0.jar -DgroupId=com.google.android.apps.dashclock -DartifactId=dashclock-api -Dversion=2.0 -Dpackaging=jar')
