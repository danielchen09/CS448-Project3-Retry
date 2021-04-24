package simpledb.tx.concurrency;

import simpledb.file.BlockId;
import simpledb.tx.Transaction;

import java.util.List;

public class LockTableWoundWait extends LockTable {

    @Override
    public synchronized void sLock(Transaction transaction, BlockId blk) {
        try {
            if (!hasLock(blk) || hasOtherSLocks(blk)) {
                grantLock(transaction, blk, 1);
                return;
            }

            if (isHolding(transaction, blk)) {
                locktype.put(blk, 1);
                return;
            }

            List<Transaction> younger = getYounger(transaction, blk);
            if (younger.size() > 0) {
                for (Transaction t : younger) {
                    t.abort();
                }
            }

            if (hasOlder(transaction, blk)) {
                wait();
                sLock(transaction, blk);
            }
        } catch (InterruptedException ex) {
            throw new LockAbortException();
        }
    }

    @Override
    public void xLock(Transaction transaction, BlockId blk) {
        try {
            if (!hasLock(blk)) {
                grantLock(transaction, blk, -1);
                return;
            }

            if (isHolding(transaction, blk)) {
                locktype.put(blk, -1);
                return;
            }

            List<Transaction> younger = getYounger(transaction, blk);
            if (younger.size() > 0) {
                for (Transaction t : younger) {
                    t.abort();
                }
            }

            if (hasOlder(transaction, blk)) {
                wait();
                xLock(transaction, blk);
            }
        } catch (InterruptedException e) {
            throw new LockAbortException();
        }
    }
}
