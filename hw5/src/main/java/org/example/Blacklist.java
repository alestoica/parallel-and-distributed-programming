package org.example;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class BlacklistNode {
    int id;
    BlacklistNode next;
    final Lock lock = new ReentrantLock();

    BlacklistNode(int id) {
        this.id = id;
        this.next = null;
    }
}

public class Blacklist {

    private final BlacklistNode head;
    private final BlacklistNode tail;

    public Blacklist() {
        head = new BlacklistNode(-1);
        tail = new BlacklistNode(-1);
        head.next = tail;
    }

    public void add(int id) {
        BlacklistNode newNode = new BlacklistNode(id);
        head.lock.lock();
        BlacklistNode prev = head;
        try {
            BlacklistNode curr = head.next;
            curr.lock.lock();
            try {
                while (curr != tail && curr.id < id) {
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
                    if (curr.id == id) {
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
            System.out.print(current.id + " ");
            current = current.next;
            size++;
        }
        System.out.println();
        System.out.println("Blacklist size: " + size);
    }
}

//class BlacklistNode {
//    int id;
//    BlacklistNode next;
//
//    BlacklistNode(int id) {
//        this.id = id;
//        this.next = null;
//    }
//}
//
//public class Blacklist {
//
//    private final BlacklistNode head;
//
//    public Blacklist() {
//        head = new BlacklistNode(-1);
//    }
//
//    public synchronized void add(int id) {
//        if (!contains(id)) {
//            BlacklistNode newNode = new BlacklistNode(id);
//            newNode.next = head.next;
//            head.next = newNode;
//        }
//    }
//
//    public synchronized boolean contains(int id) {
//        BlacklistNode current = head.next;
//        while (current != null) {
//            if (current.id == id) {
//                return true;
//            }
//            current = current.next;
//        }
//        return false;
//    }
//
//    public void print() {
//        BlacklistNode current = head.next;
//        int size = 0;
//        while (current != null) {
//            System.out.print(current.id + " ");
//            current = current.next;
//            size++;
//        }
//        System.out.println();
//        System.out.println("Blacklist length: " + size);
//    }
//}