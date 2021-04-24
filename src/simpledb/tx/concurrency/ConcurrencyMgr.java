package simpledb.tx.concurrency;

import java.util.*;
import simpledb.file.BlockId;
import simpledb.tx.Transaction;

/**
 * The concurrency manager for the transaction.
 * Each transaction has its own concurrency manager. 
 * The concurrency manager keeps track of which locks the 
 * transaction currently has, and interacts with the
 * global lock table as needed. 
 * @author Edward Sciore
 */
public class ConcurrencyMgr {

   /**
    * The global lock table. This variable is static because 
    * all transactions share the same table.
    */
   private static LockTable locktbl = new LockTableGraph();
   private Map<BlockId,String> locks  = new HashMap<BlockId,String>();

   /**
    * Obtain an SLock on the block, if necessary.
    * The method will ask the lock table for an SLock
    * if the transaction currently has no locks on that block.
    * @param blk a reference to the disk block
    */
   public void sLock(Transaction transaction, BlockId blk) {
      if (!hasLock(blk, "S") && !hasLock(blk, "X")) {
//         System.out.println(transaction.txnum + " request S lock " + blk);
         locktbl.sLock(transaction, blk);
         locks.put(blk, "S");
      }
   }

   /**
    * Obtain an XLock on the block, if necessary.
    * If the transaction does not have an XLock on that block,
    * then the method first gets an SLock on that block
    * (if necessary), and then upgrades it to an XLock.
    * @param blk a reference to the disk block
    */
   public void xLock(Transaction transaction, BlockId blk) {
      if (!hasLock(blk, "X")) {
//         System.out.println(transaction.txnum + " request X lock " + blk);
         locktbl.xLock(transaction, blk);
         locks.put(blk, "X");
      }
   }

   /**
    * Release all locks by asking the lock table to
    * unlock each one.
    */
   public void release(Transaction transaction) {
      for (BlockId blk : locks.keySet()) 
         locktbl.unlock(transaction, blk);
      locks.clear();
   }

   private boolean hasLock(BlockId blk, String type) {
      String locktype = locks.get(blk);
      return locktype != null && locktype.equals(type);
   }
}
