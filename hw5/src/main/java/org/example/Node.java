package org.example;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Node {
    int id, score;
    String country;
    Node next;
    final Lock lock = new ReentrantLock();

    Node(int id, int score, String country) {
        this.id = id;
        this.score = score;
        this.country = country;
        this.next = null;
    }
}

