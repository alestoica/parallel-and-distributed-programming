package org.example.domain;

public class Participant {
    private final int id;
    private int score;
    private final String country;

    public Participant(int id, int score, String country) {
        this.id = id;
        this.score = score;
        this.country = country;
    }

    public int getId() {
        return id;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getCountry() {
        return country;
    }
}
