package org.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class LinkedList {
    private Node head;

    public LinkedList() {
        this.head = null;
    }

    // Add to list (ordered descending)
    public synchronized void add(int id, int score) {
        if (score == -1) { // Fraud elimination
            remove(id);
            return;
        }

        // Find node by id
        Node nodeWithGivenId = findById(id);

        if (nodeWithGivenId != null) {
            // Node found, update the score
            int currentScore = nodeWithGivenId.score + score;

            // Remove the old node
            remove(id);

            // Create a new node with the updated score
            nodeWithGivenId.score = currentScore;

            // Insert the updated node in the correct position
            insertInOrder(nodeWithGivenId);
        } else {
            // If the node doesn't exist, create a new one and insert it in the correct position
            Node newNode = new Node(id, score);
            insertInOrder(newNode);
        }
    }

    // Find a node by ID
    private synchronized Node findById(int id) {
        Node current = head;
        while (current != null) {
            if (current.id == id) {
                return current;
            }
            current = current.next;
        }
        return null; // Return null if the node with the given ID is not found
    }

    // Remove from list
    public synchronized void remove(int id) {
        if (head == null) return;

        if (head.id == id) {
            head = head.next;
            return;
        }

        Node current = head;
        while (current.next != null) {
            if (current.next.id == id) {
                current.next = current.next.next;
                return;
            }
            current = current.next;
        }
    }

    // Insert a node in the correct position (ordered by score descending)
    private synchronized void insertInOrder(Node newNode) {
        if (head == null || head.score < newNode.score) {
            newNode.next = head;
            head = newNode;
            return;
        }

        Node current = head;
        while (current.next != null && current.next.score >= newNode.score) {
            current = current.next;
        }

        newNode.next = current.next;
        current.next = newNode;
    }

    // Write the ranking in a file
    public synchronized void writeToFile(String fileName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        Node current = head;
        while (current != null) {
            writer.write("(" + current.id + ", " + current.score + ")");
            writer.newLine();
            current = current.next;
        }
        writer.close();
    }
}