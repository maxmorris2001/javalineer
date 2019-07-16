package edu.lsu.cct.javalin;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class Cond {
    final AtomicReference<CondState> state = new AtomicReference<>(CondState.ready);
    volatile Consumer<Future<Boolean>> task;
    public String toString() {
        return task.toString()+":"+state.get();
    }
}
