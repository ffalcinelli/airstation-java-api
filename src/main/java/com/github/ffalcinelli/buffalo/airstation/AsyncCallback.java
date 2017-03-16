package com.github.ffalcinelli.buffalo.airstation;

/**
 * Interface to invoke some action on success or failure events.
 * <p>
 * Created by fabio on 10/03/17.
 */
public interface AsyncCallback<T extends Object> {

    void onFailure(Throwable t);

    void onSuccess(T data);
}
