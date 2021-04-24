package simpledb.tx.concurrency;

import simpledb.file.BlockId;
import simpledb.tx.Transaction;

import java.util.*;

public class LockTableGraph extends LockTable {
    class WaitForGraph {
        class Vertex {
            Transaction data;
            boolean visited = false;
            boolean inCycle = false;
            List<Vertex> neighbors = new ArrayList<>();

            Vertex(Transaction data) {
                this.data = data;
            }

            void reset() {
                visited = false;
                inCycle = false;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Vertex vertex = (Vertex) o;
                return Objects.equals(data, vertex.data);
            }

            @Override
            public int hashCode() {
                return Objects.hash(data);
            }
        }

        HashMap<Transaction, Vertex> adjacencyList = new HashMap<>();

        synchronized void waitFor(Transaction waiting, List<Transaction> holding) {
            if (!adjacencyList.containsKey(waiting))
                adjacencyList.put(waiting, new Vertex(waiting));
            for (Transaction holdingEntry : holding) {
                if (!adjacencyList.containsKey(holdingEntry))
                    adjacencyList.put(holdingEntry, new Vertex(holdingEntry));
                adjacencyList.get(waiting).neighbors.add(adjacencyList.get(holdingEntry));
            }
        }

        synchronized boolean inCycle(Vertex source) {
            source.inCycle = true;
            for (Vertex neighbor : source.neighbors) {
                if (neighbor.inCycle)
                    return true;
                else if (!neighbor.visited && inCycle(neighbor))
                    return true;
            }

            source.inCycle = false;
            source.visited = true;
            return false;
        }

        synchronized boolean hasCycle() {
            for (Vertex vertex : adjacencyList.values())
                vertex.reset();
            for (Vertex vertex : adjacencyList.values())
                if (!vertex.visited && inCycle(vertex))
                    return true;
            return false;
        }

        synchronized void remove(Transaction data) {
            Vertex target = adjacencyList.get(data);

            Iterator<Vertex> vertices = adjacencyList.values().iterator();
            while (vertices.hasNext()) {
                Vertex vertex = vertices.next();
                Iterator<Vertex> neighbors = vertex.neighbors.iterator();
                while (neighbors.hasNext()) {
                    neighbors.next();
                    if (neighbors.equals(target))
                        neighbors.remove();
                }
            }

            adjacencyList.remove(data);
        }
    }

    WaitForGraph waitForGraph = new WaitForGraph();

    @Override
    public synchronized void sLock(Transaction transaction, BlockId blk) {
        try {
//            System.out.println(transaction.txnum + " request S " + blk);
            if (!hasLock(blk) || hasOtherSLocks(blk)) {
                grantLock(transaction, blk, 1);
//                System.out.println(transaction.txnum + " got S " + blk);
                return;
            }
            if (isHolding(transaction, blk)) {
//                System.out.println("downgrade");
                locktype.put(blk, 1);
                return;
            }
            waitForGraph.waitFor(transaction, locks.get(blk));
            if (waitForGraph.hasCycle()) {
                throw new LockAbortException();
            }
            wait();
            sLock(transaction, blk);
        } catch (InterruptedException e) {
            throw new LockAbortException();
        }
    }

    @Override
    public synchronized void xLock(Transaction transaction, BlockId blk) {
        try {
//            System.out.println(transaction.txnum + " request X " + blk);
            if (!hasLock(blk)) {
                grantLock(transaction, blk, -1);
//                System.out.println(transaction.txnum + " got X " + blk);
                return;
            }
            if (isHolding(transaction, blk)) {
//                System.out.println("upgrade");
                locktype.put(blk, -1);
                return;
            }
            waitForGraph.waitFor(transaction, locks.get(blk));
            if (waitForGraph.hasCycle()) {
                throw new LockAbortException();
            }
            wait();
            xLock(transaction, blk);
        } catch (InterruptedException e) {
            throw new LockAbortException();
        }
    }

    @Override
    protected void unlockHandler(Transaction transaction, BlockId blk) {
        waitForGraph.remove(transaction);
    }
}
