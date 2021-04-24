package simpledb.tx;

import simpledb.buffer.BufferMgr;
import simpledb.file.*;
import simpledb.log.LogMgr;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;
import simpledb.tx.concurrency.*;

import java.io.File;
import java.io.IOException;

public class WaitDieTest {
   private static FileMgr fm;
   private static LogMgr lm;
   private static BufferMgr bm;

   public static void delete(File f) throws IOException {
      if (!f.exists())
         return;
      if (f.isDirectory()) {
         for (File ff : f.listFiles())
            delete(ff);
      }
      if (!f.delete()) {
         System.out.println("not deleted");
         throw new IOException();
      }
   }

   public static void main(String[] args) throws IOException {
      //initialize the database system
      String filename = "waitdietest";
      delete(new File(filename));
      SimpleDB db = new SimpleDB(filename, 400, 8);

      // change this to the implementation to be tested
      ConcurrencyMgr.locktbl = new LockTableWaitDie();

      fm = db.fileMgr();
      lm = db.logMgr();
      bm = db.bufferMgr();
      A a = new A(); new Thread(a).start();
      B b = new B(); new Thread(b).start();
      C c = new C(); new Thread(c).start();
   }

   static class A implements Runnable { 
      public void run() {
         Transaction txA = null;
         try {
            txA = new Transaction(fm, lm, bm);
            System.out.printf("Transaction A(%d) starts\n", txA.txnum);
            BlockId blk1 = new BlockId("testfile", 1);
            BlockId blk2 = new BlockId("testfile", 2);
            txA.pin(blk1);
            txA.pin(blk2);
            System.out.println("Tx A: request slock block 1");
            txA.getInt(blk1, 0);
            System.out.println("Tx A: receive slock block 1");
            Thread.sleep(1000);
            System.out.println("Tx A: request slock block 2");
            txA.getInt(blk2, 0);
            System.out.println("Tx A: receive slock block 2");
            txA.commit();
            System.out.println("Transaction A commits");
         }
         catch(InterruptedException e) {}
         catch(LockAbortException e) {
            System.out.println("Transaction A aborts");
            txA.rollback();
         }
      }
   }

   static class B implements Runnable {
      public void run() {
         Transaction txB = null;
         try {
            Thread.sleep(300);
            txB = new Transaction(fm, lm, bm);
            System.out.printf("Transaction B(%d) starts\n", txB.txnum);
            BlockId blk1 = new BlockId("testfile", 1);
            BlockId blk2 = new BlockId("testfile", 2);
            txB.pin(blk1);
            txB.pin(blk2);
            System.out.println("Tx B: request xlock block 2");
            txB.setInt(blk2, 0, 0, false);
            System.out.println("Tx B: receive xlock block 2");
            Thread.sleep(1000);
            System.out.println("Tx B: request slock block 1");
            txB.getInt(blk1, 0);
            System.out.println("Tx B: receive slock block 1");
            txB.commit();
            System.out.println("Transaction B commits");
         }
         catch(InterruptedException | LockAbortException e) {
            System.out.println("Transaction B aborts");
            txB.rollback();
         }
      }
   }

   static class C implements Runnable {
      public void run() {
         Transaction txC = null;
         try {
            Thread.sleep(600);
            txC = new Transaction(fm, lm, bm);
            System.out.printf("Transaction C(%d) starts\n", txC.txnum);
            BlockId blk1 = new BlockId("testfile", 1);
            BlockId blk2 = new BlockId("testfile", 2);
            txC.pin(blk1);
            txC.pin(blk2);
            System.out.println("Tx C: request xlock block 1");
            // Under wait-die, txC gets aborted because it must wait for
            // txB to release its write lock on block 1. Uncomment
            // the following statement and txC will not get aborted.
            // Thread.sleep(2000);
            txC.setInt(blk1, 0, 0, false);
            System.out.println("Tx C: receive xlock block 1");
            Thread.sleep(1000);
            System.out.println("Tx C: request slock block 2");
            txC.getInt(blk2, 0);
            System.out.println("Tx C: receive slock block 2");
            txC.commit();
            System.out.println("Transaction C commits");
         }
         catch(InterruptedException | LockAbortException e) {
            System.out.println("Transaction C aborts");
            txC.rollback();
         }
      }
   }
}
