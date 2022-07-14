package org.casbin.watcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPubSub;
import java.util.function.Consumer;

public class Subscriber extends JedisPubSub {
    private static final Logger logger = LoggerFactory.getLogger(Subscriber.class);

    private Runnable runnable;
    private Consumer<String> consumer;

    public Subscriber(Runnable updateCallback) {
        this.runnable = updateCallback;
    }

    public void setUpdateCallback(Runnable runnable){
        this.runnable = runnable;
    }

    public void setUpdateCallback(Consumer<String> consumer) {
        this.consumer = consumer;
    }

    public void onMessage(String channel, String message) {
        logger.debug("onMessage: channel {}, message {}", channel, message);
        if(runnable != null) {
            runnable.run();
        }
        if (consumer != null) {
            consumer.accept(message);
        }
    }
}
