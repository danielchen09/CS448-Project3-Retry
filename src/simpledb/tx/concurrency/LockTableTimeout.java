package simpledb.tx.concurrency;

import simpledb.file.BlockId;
import simpledb.tx.Transaction;

public class LockTableTimeout extends LockTable {
    @Override
    public synchronized void sLock(Transaction transaction, BlockId blk)  {
        try {
            long timestamp = System.currentTimeMillis();
            while (hasXlock(blk) && !waitingTooLong(timestamp))
                wait(MAX_TIME);
            if (hasXlock(blk))
                throw new LockAbortException();
            grantLock(transaction, blk, 1);
        }
        catch(InterruptedException e) {
            throw new LockAbortException();
        }
    }

    @Override
    public synchronized void xLock(Transaction transaction, BlockId blk) {
        try {
            long timestamp = System.currentTimeMillis();
            while (hasOtherSLocks(blk) && !waitingTooLong(timestamp))
                wait(MAX_TIME);
            if (hasOtherSLocks(blk))
                throw new LockAbortException();
            grantLock(transaction, blk, -1);
        }
        catch(InterruptedException e) {
            throw new LockAbortException();
        }
    }

    private boolean waitingTooLong(long starttime) {
        return System.currentTimeMillis() - starttime > MAX_TIME;
    }
}
