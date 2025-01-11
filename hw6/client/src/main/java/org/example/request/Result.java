package org.example.request;

import java.io.Serializable;

public class Result implements Serializable {
    int id;
    int score;
//    String country;

    public Result(int id, int score) {
        this.id = id;
        this.score = score;
//        this.country = country;
    }

    public int getId() {
        return id;
    }

    public int getScore() {
        return score;
    }

//    public String getCountry() {
//        return country;
//    }
}

