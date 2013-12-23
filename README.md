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

License
=======

    Copyright 2013 Simon Vig Therkildsen

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
