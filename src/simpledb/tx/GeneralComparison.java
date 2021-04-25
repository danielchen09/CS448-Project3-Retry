package simpledb.tx;

import simpledb.plan.Plan;
import simpledb.plan.Planner;
import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.concurrency.ConcurrencyMgr;
import simpledb.tx.concurrency.LockTable;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GeneralComparison {

    public static SimpleDB db;
    public static int RECORDS = 2000;
    public static int N_READS = 1;
    public static int N_WRITES = 1;
    public static boolean SHUFFLE = true;

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

    public static void main(String[] args) throws IOException, InterruptedException {
        Transaction.VERBOSE = false;

        LockTable.MAX_TIME = 1;
        test4();
    }

    public static void test1() throws IOException, InterruptedException {
        String filename = "general-out/out1.txt";

        PrintWriter pw = new PrintWriter(new FileOutputStream(filename, false));
        for (LockTable.LockTableType type : LockTable.LockTableType.values()) {
            for (int i = 100; i <= RECORDS; i+=100) {
                System.out.println(i);
                N_READS = i;
                int psum = 0;
                for (int j = 0; j < 10; j++) {
                    psum += runTest(type);
                }
                pw.println(i + " " + (psum / 10.0));
            }
        }
        pw.flush();
    }

    public static void test2() throws IOException, InterruptedException {
        String filename = "general-out/out2.txt";

        PrintWriter pw = new PrintWriter(new FileOutputStream(filename, false));
        for (LockTable.LockTableType type : LockTable.LockTableType.values()) {
            for (int i = 100; i <= RECORDS; i+=200) {
                System.out.println(i);
                N_WRITES = i;
                int psum = 0;
                for (int j = 0; j < 1; j++) {
                    psum += runTest(type);
                }
                pw.println(i + " " + (psum / 1.0));
            }
        }
        pw.flush();
    }

    public static void test3() throws IOException, InterruptedException {
        String filename = "general-out/out3.txt";

        RECORDS = 2000;

        PrintWriter pw = new PrintWriter(new FileOutputStream(filename, false));
        for (LockTable.LockTableType type : LockTable.LockTableType.values()) {
            for (int i = 5; i <= 500; i+=50) {
                System.out.println(i);
                N_WRITES = i;
                int psum = 0;
                for (int j = 0; j < 1; j++) {
                    psum += runTest(type);
                }
                pw.println(i + " " + (psum / 1.0));
            }
        }
        pw.flush();
    }

    public static void test4() throws IOException, InterruptedException {
        String filename = "general-out/out4.txt";

        PrintWriter pw = new PrintWriter(new FileOutputStream(filename, false));
        for (LockTable.LockTableType type : LockTable.LockTableType.values()) {
            for (int i = 5; i <= 2000; i+=50) {
                System.out.println(i);
                RECORDS = i;
                N_WRITES = RECORDS;
                int psum = 0;
                for (int j = 0; j < 1; j++) {
                    psum += runTest(type);
                }
                pw.println(i + " " + (psum / 1.0));
            }
        }
        pw.flush();
    }
    
    public static long runTest(LockTable.LockTableType type) throws IOException, InterruptedException {
        ConcurrencyMgr.locktbl = LockTable.getLockTable(type);
        String filename = "generaltest";
        delete(new File(filename));
        db = new SimpleDB(filename);

        Transaction tx = db.newTx();
        Planner planner = db.planner();
        planner.executeUpdate("create table T1(A int, B varchar(16))", tx);
        for (int i = 0; i < RECORDS; i++) {
            planner.executeUpdate(String.format("insert into T1(A,B) values(%d, '%s')", i, "B" + i), tx);
        }
        tx.commit();

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < N_READS; i++) {
            threads.add(new Thread(new T1()));
        }
        for (int i = 0; i < N_WRITES; i++) {
            threads.add(new Thread(new T2((int)(Math.random() * (RECORDS + 1)))));
        }
        if (SHUFFLE)
            Collections.shuffle(threads);

        long timer = System.currentTimeMillis();
        for (int i = 0; i < N_WRITES + N_READS; i++) {
            Thread.sleep(1);
            threads.get(i).start();
        }

        for (int i = 0; i < N_WRITES + N_READS; i++) {
            threads.get(i).join();
        }

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
                Plan p = db.planner().createQueryPlan("select A,B from T1", tx1);
                Scan s = p.open();
                while (s.next())
                    s.getString("b");
                tx1.commit();
            } catch (Exception ex) {
//                System.out.println("restarting " + ex);
                tx1.release();
                while (true) {
                    try {
                        Thread child = new Thread(new T1(tx1.txnum));
                        Thread.sleep(10);
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
                db.planner().executeUpdate("update T1 set B='CHANGED' where A=1", tx2);
                tx2.commit();
            } catch (Exception ex) {
//                System.out.println("restarting " + ex);
                tx2.release();
                while (true) {
                    try {
                        Thread child = new Thread(new T2(tx2.txnum));
                        Thread.sleep(10);
                        child.start();
                        child.join();
                        break;
                    } catch (Exception e) {}
                }
            }
        }
    }
}
