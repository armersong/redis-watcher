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
    private SyncCoordinator coordinator;

    public RedisWatcherExt(String redisIp, int redisPort, String redisChannelName, int timeout, String password) {
        super(redisIp, redisPort, redisChannelName, timeout, password);
    }

    public void SetSyncCoordinator(SyncCoordinator coordinator) {
        this.coordinator = coordinator;
        super.setUpdateCallback(new Consumer<String>(){
            @Override
            public void accept(String s) {
                handleMessage(s);
            }
        });
    }

    @Override
    public void setUpdateCallback(Runnable runnable) {
        logger.warn("setUpdateCallback is deprecated");
    }

    @Override
    public void setUpdateCallback(Consumer<String> consumer) {
        super.setUpdateCallback(consumer);
    }

    protected void handleMessage(String s) {
        try {
            logger.debug("got sync message: {}",s);
            SyncMessage msg = JSONObject.parseObject(s, SyncMessage.class);
            if(msg.getInstanceId().equals(this.localId)) {
                logger.debug("ignore self message: {}", s);
                return;
            }
            if(coordinator != null) {
                Model m = coordinator.getModel(msg.getModelId());
                if(m == null) {
                    logger.warn("model {} not exist or reloaded", msg.getModelId());
                    return;
                }
                switch(msg.getOp()) {
                    case SyncMessage.OP_ADD_POLICY: {
                        logger.info("model {} add: {}", msg.getModelId(), msg);
                        m.addPolicy(msg.getSection(), msg.getPtype(), msg.getContents());
                        break;
                    }
                    case SyncMessage.OP_REMOVE_POLICY: {
                        logger.info("model {} remove: {}", msg.getModelId(), msg);
                        m.removePolicy(msg.getSection(), msg.getPtype(), msg.getContents());
                        break;
                    }
                    case SyncMessage.OP_ADD_FILTERED_POLICY:
                    case SyncMessage.OP_SAVE_POLICY: {
                        logger.error("@TODO op: {}: {}", msg.getOp(), msg);
                        break;
                    }
                    default: {
                        logger.error("unknown op: {}: {}", msg.getOp(), msg);
                    }
                }
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
