package org.example.response;

import java.io.Serializable;
import java.util.List;

public class Response implements Serializable {
    List<CountryResult> countryResults;
    ResponseType responseType;

    public Response(ResponseType responseType, List<CountryResult> countryResults) {
        this.responseType = responseType;
        this.countryResults = countryResults;
    }

    public List<CountryResult> getCountryResults() {
        return countryResults;
    }

    public ResponseType getResponseType() {
        return responseType;
    }
}
