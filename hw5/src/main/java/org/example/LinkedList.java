package org.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class LinkedList {
    private final Node head; // Sentinel start node
    private final Node tail; // Sentinel end node

    public LinkedList() {
        this.head = new Node(-1, Integer.MAX_VALUE, null); // Dummy head
        this.tail = new Node(-1, Integer.MIN_VALUE, null); // Dummy tail
        this.head.next = this.tail;
    }

    // Add/update a node in fine-grained synchronized list
    public void addOrUpdate(int id, int score, String country) {
        Node prev = head, current = head.next;
        prev.lock.lock();
        try {
            current.lock.lock();
            try {
                while (current != tail) {
                    if (current.id == id) {
                        if (score == -1) {
                            remove(prev, current);
                        } else {
                            current.score += score;
                        }
                        return;
                    }
                    prev.lock.unlock();
                    prev = current;
                    current = current.next;
                    current.lock.lock();
                }

                if (score != -1) {
                    Node newNode = new Node(id, score, country);
                    newNode.lock.lock();
                    try {
                        newNode.next = current;
                        prev.next = newNode;
                    } finally {
                        newNode.lock.unlock();
                    }
                }
            } finally {
                current.lock.unlock();
            }
        } finally {
            prev.lock.unlock();
        }
    }

    private void remove(Node prev, Node current) {
        prev.lock.lock();
        try {
            current.lock.lock();
            try {
                prev.next = current.next;
            } finally {
                current.lock.unlock();
            }
        } finally {
            prev.lock.unlock();
        }
    }


    // Write to file
    public synchronized void writeToFile(String fileName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        Node current = head.next;

        while (current != tail) {
            writer.write("(" + current.id + ", " + current.score + ", " + current.country + ")");
            writer.newLine();
            current = current.next;
        }

        writer.close();
    }
}