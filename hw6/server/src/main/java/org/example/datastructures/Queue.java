package org.example.datastructures;

import org.example.domain.Node;
import org.example.domain.Participant;

import java.util.concurrent.atomic.AtomicInteger;

public class Queue {
    private Node front;
    private Node rear;
    private int size;
    private final int capacity;
    private boolean finished;
    private final AtomicInteger readersLeft;

    public Queue(AtomicInteger readersLeft) {
        this.front = null;
        this.rear = null;
        this.size = 0;
        this.capacity = 100;
        this.finished = false;
        this.readersLeft = readersLeft;
    }

    public synchronized void enqueue(Participant participant) {
        while (size >= capacity) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Node newNode = new Node(participant);

        if (rear == null) {
            front = newNode;
        } else {
            rear.next = newNode;
        }

        rear = newNode;
        size++;

        notifyAll();
    }

    public synchronized Participant dequeue() {
        while (isEmpty() && !finished) {
            try {
                if (readersLeft.get() == 0) {
                    return null;
                }

                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (isEmpty()) {
            return null;
        }

        Participant pair = front.getParticipant();
        front = front.next;
        size--;

        if (front == null) {
            rear = null;
        }

        notifyAll();
        return pair;
    }

    public synchronized void setFinished() {
        finished = true;
        notifyAll();
    }

    public synchronized boolean isFinished() {
        return finished && size == 0;
    }

    public synchronized boolean isEmpty() {
        return size == 0;
    }
}
