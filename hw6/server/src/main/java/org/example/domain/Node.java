package org.example.domain;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Node {
    public final Lock lock = new ReentrantLock();
    private Participant participant;
    public Node next;

    public Node(Participant participant) {
        this.participant = participant;
        this.next = null;
    }

    public Lock getLock() {
        return lock;
    }

    public Participant getParticipant() {
        return participant;
    }

    public void setParticipant(Participant participant) {
        this.participant = participant;
    }

    public Node getNext() {
        return next;
    }
}
