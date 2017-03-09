# Buffalo AirStation Link

[![Build Status](https://travis-ci.org/ffalcinelli/airstationlink.png)](https://travis-ci.org/ffalcinelli/airstationlink)
[![Coverage Status](https://img.shields.io/codecov/c/github/ffalcinelli/airstationlink/master.svg)](https://codecov.io/github/ffalcinelli/airstationlink)

This project is a simple java wrapper to manage Buffalo AirStation without using the browser.

The only supported model is WZR-1750DHP since it's the only one I own, it may work with other models too.


## Quick start

Simple like this

```java
AirStation airStation = new AirStation("http://192.168.11.1", "admin", "password");
try {
    airStation.open();
    System.out.println(airStation.getDevice().toString(4));
} catch (Exception e) {
    e.printStackTrace();
} finally {
    try {
        airStation.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
}
```

will return a JSON like

```json
{
    "SUB_VERSION": "1.02",
    "BOOT_VERSION": "6.30.163-1.00",
    "VERSION": "2.29",
    "VENDOR": "BUFFALO INC",
    "COPYRIGHT": "Copyright &copy; 2013-2014 Buffalo Inc.",
    "MODEL": "WZR-1750DHP",
    "REGION": "EU"
}
```

## Development

Development is still in progress, right now just a basic set of functionalities have been remapped.

## API Reference Documentation

The API Reference Documentation for AirStationLink can be found [here](https://ffalcinelli.github.io/airstationlink).