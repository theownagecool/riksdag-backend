# Riksdagskollen backend

## Dependencies
- java 1.8+
- ruby & bundler gem (only for deployment)

## Development
Start the dev server listening on localhost:8080:
```shell
./sbt "~;jetty:stop;jetty:start"
```

## Building
Compile the app and all its dependencies to `target/scala-2.11/app.jar`:
```shell
./sbt assembly
```

## Deployment
Deploy a new version to the server:
```shell
bundle exec cap production deploy
```

Start/stop the app:
```shell
bundle exec cap production app:stop
bundle exec cap production app:start
```

## Endpoints
```
/person
/person/status
/person/birth-year
/person/gender
/voting
```
