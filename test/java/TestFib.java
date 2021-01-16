import edu.lsu.cct.javalineer.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFib {
    static int fibc(int n) {
        if(n < 2) return n;
        return fibc(n-1) + fibc(n-2);
    }

    static int fib_sync(int n) {
        if(n < 2)
            return n;
        else
            return fib_sync(n-1)+fib_sync(n-2);
    }

    static CompletableFuture<Integer> fib(int n) {
        if (n < 2)
            return CompletableFuture.completedFuture(n);
        if (n < 20)
            return CompletableFuture.completedFuture(fib_sync(n));

        CompletableFuture<Integer> f1 = Pool.supply(() -> fib(n - 1));
        CompletableFuture<Integer> f2 = fib(n - 2);

        return f1.thenCombine(f2, Integer::sum);
    }

    @Test
    @DisplayName("Fib")
    public void testFib() {
        for(int i = 5; i < 40; i++) {
            final int f = i;
            fib(f).thenAccept(n -> {
                System.out.printf("fib(%d)=%d%n", f, n);
                assertEquals(n, fibc(f));
            });
        }

        Pool.await();
    }
}
