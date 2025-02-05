package org.casbin.watcher;

import com.alibaba.fastjson.JSONObject;
import org.casbin.jcasbin.persist.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.UUID;
import java.util.function.Consumer;

public class RedisWatcher implements Watcher {
    private static final Logger logger = LoggerFactory.getLogger(RedisWatcher.class);

    protected Runnable updateCallback;
    protected final JedisPool jedisPool;
    protected final String localId;
    protected final String redisChannelName;
    protected SubThread subThread;

    public RedisWatcher(String redisIp, int redisPort, String redisChannelName, int timeout, String password) {
        this.jedisPool = new JedisPool(new JedisPoolConfig(), redisIp, redisPort, timeout, password);
        this.localId = UUID.randomUUID().toString();
        this.redisChannelName = redisChannelName;
        startSub();
    }

    public RedisWatcher(JedisPoolConfig config, String redisIp, int redisPort, String redisChannelName, int timeout, String password) {
        this.jedisPool = new JedisPool(config, redisIp, redisPort, timeout, password);
        this.localId = UUID.randomUUID().toString();
        this.redisChannelName = redisChannelName;
        startSub();
    }

    public RedisWatcher(String redisIp, int redisPort, String redisChannelName) {
        this(redisIp, redisPort, redisChannelName, 2000, (String)null);
    }

    @Override
    public void setUpdateCallback(Runnable runnable) {
        this.updateCallback=runnable;
        subThread.setUpdateCallback(runnable);
    }

    @Override
    public void setUpdateCallback(Consumer<String> consumer) {
        subThread.setUpdateCallback(consumer);
    }

    @Override
    public void update() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(redisChannelName, "Casbin policy has a new version from redis watcher: "+localId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startSub(){
        subThread = new SubThread(jedisPool,redisChannelName,updateCallback);
        subThread.start();
    }


    protected void notify(SyncMessage message) {
        synchronized (this) {
            String content = JSONObject.toJSONString(message);
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(redisChannelName, content);
            } catch (Exception e) {
                logger.error(String.format("notify %s failed:%s", message, e.getMessage()), e);
            }
        }
    }
}
