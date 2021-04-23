package simpledb.tx.concurrency;

import java.util.*;
import simpledb.file.BlockId;
import simpledb.tx.Transaction;

/**
 * The lock table, which provides methods to lock and unlock blocks.
 * If a transaction requests a lock that causes a conflict with an
 * existing lock, then that transaction is placed on a wait list.
 * There is only one wait list for all blocks.
 * When the last lock on a block is unlocked, then all transactions
 * are removed from the wait list and rescheduled.
 * If one of those transactions discovers that the lock it is waiting for
 * is still locked, it will place itself back on the wait list.
 * @author Edward Sciore
 */
public abstract class LockTable {
   public static final long MAX_TIME = 10000; // 10 seconds
   
   protected Map<BlockId, List<Transaction>> locks = new HashMap<>();
   protected int locktype = 0; // 0: no lock, 1: s locks, -1: x lock

   public abstract void sLock(Transaction transaction, BlockId blk);
   public abstract void xLock(Transaction transaction, BlockId blk);

   protected synchronized void grantLock(Transaction transaction, BlockId blk, int locktype) {
      if (!locks.containsKey(blk)) {
         locks.put(blk, new ArrayList<>());
      }
      locks.get(blk).add(transaction);
      this.locktype = locktype;
   }

   synchronized void unlock(Transaction transaction, BlockId blk) {
      if (!locks.containsKey(blk) || locks.get(blk).size() == 0)
         return;

      Iterator<Transaction> it = locks.get(blk).iterator();
      while (it.hasNext()) {
         Transaction t = it.next();
         if (t.equals(transaction))
            it.remove();
      }

      if (locks.get(blk).size() == 0) {
         // no locks, others can be granted
         locks.remove(blk);
         locktype = 0;
         notifyAll();
      }
   }

   protected boolean hasXlock(BlockId blk) {
      return locktype == -1;
   }
   
   protected boolean hasOtherSLocks(BlockId blk) {
      return locktype == 1;
   }
}
