package searchengine.services.components;

import org.springframework.stereotype.Component;

import java.util.concurrent.ForkJoinPool;
@Component
public class FjpComponent {
    private static  volatile ForkJoinPool forkJoinPool;

    private FjpComponent() {
    }
    public static ForkJoinPool getInstance(){
        if (forkJoinPool == null){
            forkJoinPool = new ForkJoinPool();
        }
        return forkJoinPool;
    }
}
