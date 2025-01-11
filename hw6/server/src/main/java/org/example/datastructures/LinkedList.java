package org.example.datastructures;

import org.example.domain.Node;
import org.example.domain.Participant;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LinkedList {
    private final Node head; // Sentinel start node
    private final Node tail; // Sentinel end node

    public LinkedList() {
        this.head = new Node(null); // Dummy head
        this.tail = new Node(null); // Dummy tail
        this.head.next = this.tail;
    }

    public synchronized List<Participant> getItemsAsList() {
        List<Participant> list = new ArrayList<>();

        Node current = head.next;

        while (current != tail) {
            list.add(current.getParticipant());
            current = current.next;
        }

        return list;
    }

    public void add(Participant participant) {
        Node prev = head, current = head.next;
        prev.lock.lock();
        try {
            current.lock.lock();
            try {
                while (current != tail) {
                    prev.lock.unlock();
                    prev = current;
                    current = current.next;
                    current.lock.lock();
                }

                Node newNode = new Node(participant);
                newNode.lock.lock();
                try {
                    newNode.next = current;
                    prev.next = newNode;
                } finally {
                    newNode.lock.unlock();
                }
            } finally {
                current.lock.unlock();
            }
        } finally {
            prev.lock.unlock();
        }
    }

    // Add/update a node in fine-grained synchronized list
    public void addOrUpdate(Participant participant) {
        Node prev = head, current = head.next;
        prev.lock.lock();
        try {
            current.lock.lock();
            try {
                while (current != tail) {
                    if (current.getParticipant().getId() == participant.getId()) {
                        if (participant.getScore() == -1) {
                            remove(prev, current);
                        } else {
                            current.getParticipant().setScore(current.getParticipant().getScore() + participant.getScore());
                        }
                        return;
                    }
                    prev.lock.unlock();
                    prev = current;
                    current = current.next;
                    current.lock.lock();
                }

                if (participant.getScore() != -1) {
                    Node newNode = new Node(participant);
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
        BufferedWriter writer = new BufferedWriter(new FileWriter("server/src/main/resources/output/" + fileName));
        Node current = head.next;

        while (current != tail) {
            writer.write("(" + current.getParticipant().getId() + ", " + current.getParticipant().getScore() + ", " + current.getParticipant().getCountry() + ")");
            writer.newLine();
            current = current.next;
        }

        writer.close();
    }
}