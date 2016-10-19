# kixi.data-acquisition

A microservice which helps facilitate user data acquisition via strategies such as invites and requests

## Environment

There is a `docker-compose.yml` file included in the repo which will set up the environment required for running development and testing:

``` bash
docker-compose up -d
```

Additionally, for InfluxDB you will need to create the database:

``` bash
curl -G http://localhost:8086/query --data-urlencode "q=CREATE DATABASE metrics
```

## License

Copyright © 2016 Mastodon C Ltd

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
