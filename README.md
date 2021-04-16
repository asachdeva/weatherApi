# WeatherAPI

WeatherAPI challenge using [OpenWeather](https://openweathermap.org/) as source of info.
Tech Stack:
* cats-effect-2 (effect type)
* ciris (config)
* http4s (server + dsl)
* odin (logging)
* refined (typesafety)
* sttp (client)
* tapir (openApi)

## Steps to install local dev env if you use nix-shell
`nix-shell`

## Usage
* `sbt clean compile run`
* Open the SwaggerUI by navigating to [Local Swagger UI](http://localhost:8080)
* Click on the GET for /weather followed by Try it out
* Enter a Latitude and Longitude and then Execute

Alternatively make a curl call like so:

`curl "http://localhost:8080/weather?lat=19.07&lon=72.87"`

Note:  There is a CI (github actions) job enabled for this repo.
