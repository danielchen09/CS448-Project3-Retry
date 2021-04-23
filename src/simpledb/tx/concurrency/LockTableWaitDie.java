package simpledb.tx.concurrency;

import simpledb.file.BlockId;
import simpledb.tx.Transaction;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;

public class LockTableWaitDie extends LockTable {
    @Override
    public synchronized void sLock(Transaction transaction, BlockId blk) {
        try {
            // already has slock on block, or no lock at all
            if (!hasLock(blk) || hasOtherSLocks(blk)) {
                grantLock(transaction, blk, 1);
                return;
            }

            // if there are older transactions, die
            if (hasOlder(transaction, blk))
                throw new LockAbortException();

            // if the transaction already has an X lock, downgrade
            if (isHolding(transaction, blk)) {
                locktype.put(blk, 1);
                return;
            }

            // if there are younger transactions(X), wait
            if (getYounger(transaction, blk).size() > 0) {
                wait();
                // after waking up, check if the lock can be granted
                sLock(transaction, blk);
            }
        } catch (InterruptedException e) {
            throw new LockAbortException();
        }
    }

    @Override
    public synchronized void xLock(Transaction transaction, BlockId blk) {
        try {
            // if no lock, grsant lock
            if (!hasLock(blk)) {
                grantLock(transaction, blk, -1);
                return;
            }

            // any older transactions, die
            if (hasOlder(transaction, blk))
                throw new LockAbortException();

            // if transaction is itself holding, downgrade
            if (isHolding(transaction, blk) && locks.get(blk).size() == 1) {
                locktype.put(blk, -1);
                // notify so other pending S locks can be granted
                notifyAll();
                return;
            }

            // if any younger transactions, wait
            if (getYounger(transaction, blk).size() > 0) {
                wait();
                xLock(transaction, blk);
            }
        } catch (InterruptedException e) {
            throw new LockAbortException();
        }
    }
}
