package org.example.request;

import java.io.Serializable;
import java.util.List;

public class Request implements Serializable {
    RequestType requestType;
    List<Result> results;
    String country;

    public Request(RequestType requestType, List<Result> results, String country) {
        this.requestType = requestType;
        this.results = results;
        this.country = country;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public List<Result> getResults() {
        return results;
    }

    public String getCountry() {
        return country;
    }
}
