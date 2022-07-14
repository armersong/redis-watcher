package org.casbin.watcher;

import com.alibaba.fastjson.JSONObject;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.WatcherEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

public class RedisWatcherExt extends RedisWatcher implements WatcherEx {
    private static final Logger logger = LoggerFactory.getLogger(RedisWatcherExt.class);
    private SyncListener listener;

    public RedisWatcherExt(String redisIp, int redisPort, String redisChannelName, int timeout, String password) {
        super(redisIp, redisPort, redisChannelName, timeout, password);
    }

    public void SetSyncListener(SyncListener listener) {
        this.listener = listener;
    }

    @Override
    public void setUpdateCallback(Runnable runnable) {
        this.updateCallback=runnable;
        subThread.setUpdateCallback(new Runnable() {
            @Override
            public void run() {
                logger.info("update callback");
                runnable.run();
            }
        });
    }

    @Override
    public void setUpdateCallback(Consumer<String> consumer) {
        super.setUpdateCallback(new Consumer<String>(){
            @Override
            public void accept(String s) {
                handleMessage(s);
            }
        });
    }

    protected void handleMessage(String s) {
        try {
            SyncMessage message = JSONObject.parseObject(s, SyncMessage.class);
            if(message.getInstanceId().equals(this.localId)) {
                logger.debug("ignore self message: %s", s);
                return;
            }
            if(listener != null) {
                listener.handleMessage(message);
            }
        } catch(Exception e) {
            logger.error(String.format("invalid message:%s", s), e);
        }
    }

    @Override
    public void updateForAddPolicy(String modelId, String sec, String ptype, String... params) {
        notify(new SyncMessage(this.localId, modelId, SyncMessage.OP_ADD_POLICY, sec, ptype, Arrays.asList(params)));
    }

    @Override
    public void updateForRemovePolicy(String modelId, String sec, String ptype, String... params) {
        notify(new SyncMessage(this.localId, modelId, SyncMessage.OP_REMOVE_POLICY, sec, ptype, Arrays.asList(params)));
    }

    @Override
    public void updateForRemoveFilteredPolicy(String modelId, String sec, String ptype, int fieldIndex, String... fieldValues) {
        SyncMessage msg = new SyncMessage(this.localId, modelId, SyncMessage.OP_REMOVE_POLICY, sec, ptype, Arrays.asList(fieldValues));
        msg.setFilterIndex(fieldIndex);
        notify(msg);
    }

    @Override
    public void updateForSavePolicy(String modelId, Model model) {
        notify(new SyncMessage(this.localId, modelId, SyncMessage.OP_SAVE_POLICY, "", "", new ArrayList<>()));
    }
}
