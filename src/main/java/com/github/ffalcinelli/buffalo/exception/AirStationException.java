package com.github.ffalcinelli.buffalo.exception;

import java.io.IOException;

/**
 * A generic exception class to handle special cases in AirStation communication.
 *
 * Created by fabio on 24/02/17.
 */
public class AirStationException extends IOException {

    public AirStationException(String message) {
        super(message);
    }

    public AirStationException(String message, Throwable cause) {
        super(message, cause);
    }
}
