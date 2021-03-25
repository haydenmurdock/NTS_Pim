package com.example.nts_pim.utilities.here_maps;

public class HereConsts {
    static final String API_KEY = "020141f3-9758-43f0-9838-5420a2609e0e";

    // Parsing constants shared by multiple APIs (unique constants for an API will be inside the API's class).
    static final String JSON_STATUS = "status";
    static final String STATUS_OK = "OK";
    static final String STATUS_INVALID_API_KEY = "API_KEY_NOT_VALID";
    static final String STATUS_INVALID_ORIG_ADDR = "START_ADDR_NOT_VALID";
    static final String STATUS_INVALID_DEST_ADDR = "DEST_ADDR_NOT_VALID";
    static final String STATUS_SYSTEM_ERROR = "SYSTEM_ERROR";

    static final String JSON_SYSTEM_ERROR = "error";

    static final String JSON_TIME = "time";
    static final String JSON_LAT = "lat";
    static final String JSON_LNG = "lng";
    static final String JSON_TITLE = "title";

    public enum HereResultStatus
    {
        UNKNOWN, OK, INVALID_API_KEY, INVALID_ORIGIN_ADDR, INVALID_DEST_ADDR, SYSTEM_ERROR
    }
}


