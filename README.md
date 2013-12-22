Cathode
=======

An app for managing your Trakt library.


Building
--------

Some initialization is required before building this application.

Log in at http://trakt.tv/api-docs/authentication and find your Trakt API key.

To perform the initialization execute the following:
```
python init.py <insert Trakt API key>
```

Then just build by calling:
```
./gradlew clean assembleDebug
```
