package org.example;

public class Queue {
    private Node front;
    private Node rear;
    private boolean finished = false;

    public Queue() {
        this.front = null;
        this.rear = null;
    }

    public synchronized void enqueue(int id, int score) {
        Node newNode = new Node(id, score);

        if (rear == null) {
            front = newNode;
        } else {
            rear.next = newNode;
        }
        rear = newNode;

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

        Pair pair = new Pair(front.id, front.score);
        front = front.next;

        if (front == null) {
            rear = null;
        }

        return pair;
    }

    public synchronized void setFinished() {
        finished = true;
        notifyAll();
    }

    public synchronized boolean isFinished() {
        return finished && front == null;
    }

    public synchronized boolean isEmpty() {
        return front == null;
    }
}