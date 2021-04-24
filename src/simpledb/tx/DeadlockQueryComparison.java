package simpledb.tx;

import simpledb.plan.Plan;
import simpledb.plan.Planner;
import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.concurrency.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class DeadlockQueryComparison {
    public static SimpleDB db;

    public static int RESTART_DELAY = 10; // ms

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

    public static void main(String[] args) throws InterruptedException, IOException {
        Transaction.VERBOSE = false;
        SimpleDB.VERBOSE = false;
        String filename = "deadlockquerycomparison-out/out.txt";

        PrintWriter pw = new PrintWriter(new FileOutputStream(filename, false));

        long psum;

        System.out.println("start wait die");
        ConcurrencyMgr.locktbl = new LockTableWaitDie();
        psum = 0;
        for (int i = 0; i < 10; i++) {
            System.out.println(i);
            psum += runTest();
            Thread.sleep(100);
        }
        pw.println("waitdie " + psum / 10.0);
        System.out.println("done wait die");

        System.out.println("start wound wait");
        ConcurrencyMgr.locktbl = new LockTableWoundWait();
        psum = 0;
        for (int i = 0; i < 10; i++) {
            System.out.println(i);
            psum += runTest();
            Thread.sleep(100);
        }
        pw.println("woundwait " + psum / 10.0);
        System.out.println("done wound wait");

        System.out.println("start graph");
        ConcurrencyMgr.locktbl = new LockTableGraph();
        psum = 0;
        for (int i = 0; i < 10; i++) {
            System.out.println(i);
            psum += runTest();
        }
        pw.println("graph " + psum / 10.0);
        System.out.println("done graph");

        ConcurrencyMgr.locktbl = new LockTableTimeout();
        for (int i = 1; i <= 300; i+=10) {
            System.out.println(i);
            LockTable.MAX_TIME = i;
            pw.println(i + " " + runTest());
            Thread.sleep(100);
        }

        pw.flush();
    }

    public static long runTest() throws InterruptedException, IOException {
        String filename = "deadlockquerytest";
        delete(new File(filename));
        db = new SimpleDB(filename);

        Transaction tx = db.newTx();

        Planner planner = db.planner();
        planner.executeUpdate("create table T1(A int, B varchar(16), C varchar(16))", tx);
        planner.executeUpdate("insert into T1(A,B,C) values(1,'B1','C1')", tx);
        planner.executeUpdate("insert into T1(A,B,C) values(2,'B2','C2')", tx);
        planner.executeUpdate("create table T2(X int, Y varchar(16), Z varchar(16))", tx);
        planner.executeUpdate("insert into T2(X,Y,Z) values(1, 'Y1', 'Z1')", tx);
        planner.executeUpdate("insert into T2(X,Y,Z) values(2, 'Y2', 'Z2')", tx);
        tx.commit();

        Thread thr1 = new Thread(new T1());
        Thread thr2 = new Thread(new T2());

        long timer = System.currentTimeMillis();
        thr1.start();
        Thread.sleep(5);
        thr2.start();

        thr1.join();
        thr2.join();
        long time = System.currentTimeMillis() - timer;
        db.fileMgr().closeAll();
        return time;
    }

    static class T1 implements Runnable {
        private int txnum;

        public T1() {
            txnum = -1;
        }

        public T1(int txnum) {
            this.txnum = txnum;
        }

        @Override
        public void run() {
            Transaction tx1 = db.newTx();
            if (txnum != -1)
                tx1.setTxNum(txnum);
            try {
                db.planner().executeUpdate("update T1 set B='B11' where A=1", tx1);
                Thread.sleep(10);
                db.planner().executeUpdate("update T2 set Z='Z11' where X=1", tx1);
                tx1.commit();
            } catch (Exception ex) {
//                System.out.println("restarting " + ex);
                tx1.release();
                while (true) {
                    try {
                        Thread child = new Thread(new T1(tx1.txnum));
                        Thread.sleep(RESTART_DELAY + (int)(Math.random() + 1));
                        child.start();
                        child.join();
                        break;
                    } catch (Exception e) {}
                }
            }
        }
    }

    static class T2 implements Runnable {
        private int a;
        private int txnum;

        public T2() {
            txnum = -1;
        }

        public T2(int txnum) {
            this.txnum = txnum;
        }

        @Override
        public void run() {
            Transaction tx2 = db.newTx();
            if (txnum != -1)
                tx2.setTxNum(txnum);
            try {
                db.planner().executeUpdate("update T2 set Z='Z21' where X=1", tx2);
                Thread.sleep(5);
                db.planner().executeUpdate("update T1 set C='C21' where A=1", tx2);
                tx2.commit();
            } catch (Exception ex) {
//                System.out.println("restarting " + ex);
                tx2.release();
                while (true) {
                    try {
                        Thread child = new Thread(new T2(tx2.txnum));
                        Thread.sleep(RESTART_DELAY + (int)(Math.random() + 1));
                        child.start();
                        child.join();
                        break;
                    } catch (Exception e) {}
                }
            }
        }
    }
}
