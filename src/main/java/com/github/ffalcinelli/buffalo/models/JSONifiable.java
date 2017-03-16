package com.github.ffalcinelli.buffalo.models;

import org.json.JSONObject;

/**
 * Created by fabio on 15/03/17.
 */
public interface JSONifiable {

    JSONObject toJSONObject();

    void fromJSONObject(JSONObject jsonObject);
}
