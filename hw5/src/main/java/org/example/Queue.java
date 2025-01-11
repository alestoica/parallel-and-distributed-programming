package org.example;

public class Queue {
    private Node front;
    private Node rear;
    private int size;
    private final int capacity;
    private boolean finished;

    public Queue(int capacity) {
        this.front = null;
        this.rear = null;
        this.size = 0;
        this.capacity = capacity;
        this.finished = false;
    }

    public synchronized void enqueue(int id, int score, String country) {
        while (size >= capacity) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Node newNode = new Node(id, score, country);

        if (rear == null) {
            front = newNode;
        } else {
            rear.next = newNode;
        }

        rear = newNode;
        size++;

        notifyAll();
    }

    public synchronized Pair dequeue() {
        while (isEmpty() && !finished) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (isEmpty()) {
            return null;
        }

        Pair pair = new Pair(front.id, front.score, front.country);
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
