package org.example.datastructures;

import org.example.domain.BlacklistNode;
import org.example.domain.Node;
import org.example.domain.Participant;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Blacklist {
    private final BlacklistNode head;
    private final BlacklistNode tail;

    public Blacklist() {
        head = new BlacklistNode(-1, "");
        tail = new BlacklistNode(-1, "");
        head.next = tail;
    }

    public synchronized List<Participant> getItemsAsList() {
        List<Participant> list = new ArrayList<>();

        BlacklistNode current = head.next;

        while (current != tail) {
            list.add(new Participant(current.getId(), -1, current.getCountry()));
            current = current.next;
        }

        return list;
    }

    public void add(int id, String country) {
        BlacklistNode newNode = new BlacklistNode(id, country);
        head.lock.lock();
        BlacklistNode prev = head;
        try {
            BlacklistNode curr = head.next;
            curr.lock.lock();
            try {
                while (curr != tail && curr.getId() < id) {
                    prev.lock.unlock();
                    prev = curr;
                    curr = curr.next;
                    curr.lock.lock();
                }
                newNode.next = curr;
                prev.next = newNode;
            } finally {
                curr.lock.unlock();
            }
        } finally {
            prev.lock.unlock();
        }
    }

    public boolean contains(int id) {
        head.lock.lock();
        BlacklistNode curr = head;
        try {
            curr = curr.next;
            curr.lock.lock();
            try {
                while (curr != tail) {
                    if (curr.getId() == id) {
                        return true;
                    }
                    BlacklistNode next = curr.next;
                    next.lock.lock(); // Lock the next node
                    curr.lock.unlock(); // Unlock the current node
                    curr = next;
                }
                return false;
            } finally {
                curr.lock.unlock(); // Potential issue: Unlocking already unlocked or mismatched node
            }
        } finally {
            head.lock.unlock();
        }
    }

    public void print() {
        BlacklistNode current = head.next;
        int size = 0;
        while (current != tail) {
            System.out.print(current.getId() + " ");
            current = current.next;
            size++;
        }
        System.out.println();
        System.out.println();
        System.out.println("Blacklist size: " + size);
    }
}