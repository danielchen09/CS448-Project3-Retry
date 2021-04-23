package simpledb.tx;

import simpledb.plan.Plan;
import simpledb.plan.Planner;
import simpledb.query.Scan;
import simpledb.server.SimpleDB;

public class QueryTest {
    public static SimpleDB db;

    public static int RECORDS = 400;
    public static int RESTART_DELAY = 10; // ms

    public static void main(String[] args) throws InterruptedException {
        db = new SimpleDB("querytest");

        Transaction tx = db.newTx();

        Planner planner = db.planner();
        System.out.println("create table");
        String cmd = "create table T1(A int, B varchar(9))";
        planner.executeUpdate(cmd, tx);
        for (int i = 0; i < RECORDS; i++) {
            int a = i + 1;
            String b = "b" + a;
            cmd = String.format("insert into T1(A,B) values(%d,'%s')", a, b);
            planner.executeUpdate(cmd, tx);
        }
        tx.commit();

        Thread[] threads = new Thread[RECORDS + 1];
        threads[0] = new Thread(new T1());
        for (int i = 1; i < RECORDS + 1; i++) {
            threads[i] = new Thread(new T2((int)(Math.random() * RECORDS + 1)));
        }
        for (int i = 0; i < RECORDS + 1; i++) {
            threads[i].start();
        }
        for (int i = 0; i < RECORDS + 1; i++) {
            threads[i].join();
        }
        System.out.println("done");
    }

    static class T1 implements Runnable {
        @Override
        public void run() {
            Transaction tx1 = db.newTx();
            try {
                String qry = "select B from T1";
                Plan p = db.planner().createQueryPlan(qry, tx1);
                Scan s = p.open();
                while (s.next()) {
                    System.out.println(s.getString("b"));
                }
                s.close();
                tx1.commit();
            } catch (Exception ex) {
                System.out.println("restarting " + ex);
                tx1.release();
                try {
                    Thread.sleep(RESTART_DELAY);
                    Thread child = new Thread(new T1());
                    child.start();
                    child.join();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class T2 implements Runnable {
        private int a;

        public T2(int a) {
            this.a = a;
        }

        @Override
        public void run() {
            Transaction tx2 = db.newTx();
            try {
                String upd = "update T1 set A=1 where A=" + a;
                db.planner().executeUpdate(upd, tx2);
                tx2.commit();
            } catch (Exception ex) {
                System.out.println("restarting " + ex);
                tx2.release();
                try {
                    Thread.sleep(RESTART_DELAY);
                    Thread child = new Thread(new T2(a));
                    child.start();
                    child.join();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
