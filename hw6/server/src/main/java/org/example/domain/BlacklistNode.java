package org.example.domain;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BlacklistNode {
    int id;
    String country;
    public BlacklistNode next;
    public final Lock lock = new ReentrantLock();

    public BlacklistNode(int id, String country) {
        this.id = id;
        this.country = country;
        this.next = null;
    }

    public int getId() {
        return id;
    }

    public String getCountry() {
        return country;
    }
}
