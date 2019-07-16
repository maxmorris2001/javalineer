package edu.lsu.cct.javalin.test;

import edu.lsu.cct.javalin.*;
import java.util.*;

class ProdCon {

    static int idSeq = 0;
    final int id = idSeq++;

    private volatile boolean readyToProduce = false;
    private volatile boolean readyToConsume = true;
    private volatile boolean alwaysReady = false;
    private volatile int val = 0;

    public ProdCon(boolean a) {
        alwaysReady = a;
    }

    boolean isReadyToProduce(int n) {
        if(alwaysReady)
            return true;
        if (val != n) {
            //System.out.printf("not ready to produce: %d != %d: %d%n",val,n,id);
            return false;
        }
        return alwaysReady || readyToProduce;
    }

    boolean isReadyToConsume(int n) {
        if(alwaysReady)
            return true;
        if(val != n) {
            //System.out.printf("not ready to consume: %d != %d: %d%n",val,n,id);
            return false;
        }
        return alwaysReady || readyToConsume;
    }

    void produce(int n) {
        if(alwaysReady)
            return;
        //System.out.println("produce: "+n+","+id);
        readyToProduce = false;
        readyToConsume = true;
        assert n >= val: ""+n+" >= "+val;
        val = n;
    }

    void consume(int n) {
        if(alwaysReady)
            return;
        //System.out.println("consume: "+n+","+id);
        readyToProduce = true;
        readyToConsume = false;
    }
}

class Segment {
    final boolean all_ = true;

    final int id;

    GuardVar< ProdCon> left = new GuardVar< ProdCon>(new ProdCon(false));
    GuardVar< ProdCon> right = new GuardVar< ProdCon>(new ProdCon(false));
    GuardVar< ProdCon> neighborLeft, neighborRight;
    public String toString() {
        return ""+id+"{"+
            left.getGuard().id+","+
            right.getGuard().id+","+
            neighborLeft.getGuard().id+","+
            neighborRight.getGuard().id+
            "}";
    }

    Segment(int id) { this.id = id; }

    void runStep(final int step) {
        //Here.println("id="+id+" step="+step);
        if(step == 10) {
            System.out.println("Complete!");
            return;
        }
        if(id == 0)
            System.out.println("Running step: " + step+" id="+id);
        //condition with(left => l, right => r)
        Guard.runCondition(left, right, (l_,r_,f)->
        {
            ProdCon l = l_.get();
            ProdCon r = r_.get();
            //assert this guardedby left.getGuard();
            //assert this guardedby right.getGuard();
            if (!l.isReadyToConsume(step)) {
                f.set(false);
                return;
            }
            if (!r.isReadyToConsume(step)) {
                f.set(false);
                return;
            }
            //System.out.printf("Stage1: step=%d, id=%d%n",step,id);
            l.consume(step);
            r.consume(step);
            assert r.id != l.id;
            if(all_) {
                left.signalAll();
                right.signalAll();
            } else {
                left.signal();
                right.signal();
            }
            //ignore { Thread.sleep(100); }
            //condition with(neighborLeft => nl, neighborRight => nr)
            Guard.runCondition(neighborLeft, neighborRight, (nl_, nr_, ff)->
            {
                ProdCon nl = nl_.get();
                ProdCon nr = nr_.get();
                //assert this guardedby neighborLeft.getGuard();
                //assert this guardedby neighborRight.getGuard();
                if (!nl.isReadyToProduce(step)) {
                    ff.set(false);
                    return;
                }
                if (!nr.isReadyToProduce(step)) {
                    ff.set(false);
                    return;
                }
                //System.out.printf("Stage2: step=%d, id=%d%n",step,id);
                nl.produce(step + 1);
                nr.produce(step + 1);
                assert nl.id != nr.id: "on seg: "+id;
                if(all_) {
                    neighborLeft.signalAll();
                    neighborRight.signalAll();
                } else {
                    neighborLeft.signal();
                    neighborRight.signal();
                }
                runStep(step + 1);

                ff.set(true);
            });
            f.set(true);
        });
    }
}

public class March {

    public static void main(String[] args) {
        final int N = 20;
        System.out.println("N=" + N);
        final List< Segment> segs = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            segs.add(new Segment(i));
        }

        for (int i = 0; i < N; i++) {
            Segment seg = segs.get(i);
            if (i == 0) {
                seg.left = seg.neighborLeft = new GuardVar< ProdCon>(new ProdCon(true));
            } else {
                seg.neighborLeft = segs.get(i - 1).right;
            }

            if (i + 1 == N) {
                seg.right = seg.neighborRight = new GuardVar< ProdCon>(new ProdCon(true));
            } else {
                seg.neighborRight = segs.get(i + 1).left;
            }

            System.out.println("seg: "+seg);
        }

        for (int i = 0; i < N; i++) {
            final Segment seg = segs.get(i);
            seg.runStep(0);
        }
        Pool.await();
    }
}
