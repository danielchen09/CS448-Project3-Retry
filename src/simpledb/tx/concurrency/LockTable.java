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
   public enum LockTableType {
      WAIT_DIE,
      WOUND_WAIT,
      GRAPH,
      TIMEOUT
   }
   public static LockTable getLockTable(LockTable.LockTableType type) {
      switch (type) {
         case WAIT_DIE:
            return new LockTableWaitDie();
         case WOUND_WAIT:
            return new LockTableWoundWait();
         case GRAPH:
            return new LockTableGraph();
         case TIMEOUT:
            return new LockTableTimeout();
      }
      return null;
   }

   public static long MAX_TIME = 3000; // 10 seconds
   
   protected Map<BlockId, List<Transaction>> locks = new HashMap<>();
   protected Map<BlockId, Integer> locktype = new HashMap<>(); // 0: no lock, 1: s locks, -1: x lock

   public abstract void sLock(Transaction transaction, BlockId blk);
   public abstract void xLock(Transaction transaction, BlockId blk);

   protected synchronized void grantLock(Transaction transaction, BlockId blk, int type) {
      if (!locks.containsKey(blk))
         locks.put(blk, new ArrayList<>());
      locks.get(blk).add(transaction);
      locktype.put(blk, type);
   }

   synchronized void unlock(Transaction transaction, BlockId blk) {
//      System.out.println(transaction.txnum + " unlock " + blk);
      unlockHandler(transaction, blk);
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
         locktype.put(blk, 0);
         notifyAll();
      }
   }

   protected void unlockHandler(Transaction transaction, BlockId blk) {

   }

   protected List<Transaction> getYounger(Transaction transaction, BlockId blk) {
      if (locks.get(blk) == null)
         return null;
      List<Transaction> younger = new ArrayList<>();
      for (Transaction t : locks.get(blk)) {
         if (t.txnum > transaction.txnum)
            younger.add(t);
      }
      return younger;
   }

   protected boolean isHolding(Transaction transaction, BlockId blk) {
      if (locks.get(blk) == null)
         return false;
      for (Transaction t : locks.get(blk))
         if (t.txnum == transaction.txnum)
            return true;
      return false;
   }

   protected boolean hasOlder(Transaction transaction, BlockId blk) {
      if (locks.get(blk) == null)
         return false;
      for (Transaction t : locks.get(blk))
         if (t.txnum < transaction.txnum)
            return true;
      return false;
   }

   protected boolean hasLock(BlockId blk) {
      return locktype.containsKey(blk) && locktype.get(blk) != 0;
   }

   protected boolean hasXlock(BlockId blk) {
      return locktype.containsKey(blk) && locktype.get(blk) == -1;
   }
   
   protected boolean hasOtherSLocks(BlockId blk) {
      return locktype.containsKey(blk) && locktype.get(blk) == 1;
   }
}
