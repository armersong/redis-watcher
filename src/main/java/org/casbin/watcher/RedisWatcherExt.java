package org.casbin.watcher;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.WatcherEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class RedisWatcherExt extends RedisWatcher implements WatcherEx {
    private static final int EXPIRE_TIME = 120;
    private static final Logger logger = LoggerFactory.getLogger(RedisWatcherExt.class);
    private SyncCoordinator coordinator;

    public RedisWatcherExt(String redisIp, int redisPort, String redisChannelName, int timeout, String password) {
        super(redisIp, redisPort, redisChannelName, timeout, password);
    }

    public void SetSyncCoordinator(SyncCoordinator coordinator) {
        SetSyncCoordinator(coordinator, null);
    }

    public void SetSyncCoordinator(SyncCoordinator coordinator, Consumer<String> comsumer) {
        this.coordinator = coordinator;
        Consumer<String> com = comsumer;
        if(com == null) {
            com = new Consumer<String>(){
                @Override
                public void accept(String s) {
                    handleMessage(s);
                }
            };
        }
        super.setUpdateCallback(com);
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
                Enforcer enforcer = coordinator.getEnforcer(msg.getModelId());
                if(enforcer != null){
                    if(msg.getSection().indexOf("p") == 0) {
                        switch(msg.getOp()) {
                            case SyncMessage.OP_ADD_POLICY: {
                                if(!enforcer.hasPolicy(msg.getContents())) {
                                    logger.info("model {} add: {}", msg.getModelId(), msg);
                                    enforcer.addPolicy(msg.getContents());
                                }
                                break;
                            }
                            case SyncMessage.OP_REMOVE_POLICY: {
                                if(enforcer.hasPolicy(msg.getContents())) {
                                    logger.info("model {} remove: {}", msg.getModelId(), msg);
                                    enforcer.removePolicy(msg.getContents());
                                }
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

                    } else if(msg.getSection().indexOf("g") == 0) {
                        switch(msg.getOp()) {
                            case SyncMessage.OP_ADD_POLICY: {
                                if(msg.getContents().size() >=2) {
                                    if(!enforcer.hasRoleForUser(msg.getContents().get(0), msg.getContents().get(1))) {
                                        logger.info("model {} add: {}", msg.getModelId(), msg);
                                        enforcer.addRoleForUser(msg.getContents().get(0), msg.getContents().get(1));
                                    }
                                } else {
                                    logger.error("section {} ptype {} content size {} too small", msg.getSection(), msg.getPtype(), msg.getContents().size());
                                }
                                break;
                            }
                            case SyncMessage.OP_REMOVE_POLICY: {
                                if(msg.getContents().size() >=2) {
                                    if(enforcer.hasRoleForUser(msg.getContents().get(0), msg.getContents().get(1))) {
                                        logger.info("model {} remove: {}", msg.getModelId(), msg);
                                        enforcer.deleteRoleForUser(msg.getContents().get(0), msg.getContents().get(1));
                                    }
                                } else {
                                    logger.error("section {} ptype {} content size {} too small", msg.getSection(), msg.getPtype(), msg.getContents().size());
                                }
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

                    } else {
                        logger.error("@TODO: section {} ptype {}",msg.getSection(), msg.getPtype());
                    }
                    return;
                }
                logger.error("section {} ptype {} not handled", msg.getSection(), msg.getPtype());
            }
        } catch(Exception e) {
            logger.error(String.format("invalid message:%s", s), e);
        }
    }

    @Override
    public void updateForAddPolicy(String modelId, String sec, String ptype, String... paras) {
        List<String> params = new ArrayList();
        params.add(SyncMessage.OP_ADD_POLICY);
        params.add(modelId);
        params.add(sec);
        params.add(ptype);
        params.addAll(Arrays.asList(paras));
        if(!needBroadcast(params)) {
            return;
        }
        notify(new SyncMessage(this.localId, modelId, SyncMessage.OP_ADD_POLICY, sec, ptype, Arrays.asList(paras)));
    }

    @Override
    public void updateForRemovePolicy(String modelId, String sec, String ptype, String... paras) {
        List<String> params = new ArrayList();
        params.add(SyncMessage.OP_REMOVE_POLICY);
        params.add(modelId);
        params.add(sec);
        params.add(ptype);
        params.addAll(Arrays.asList(paras));
        if(!needBroadcast(params)) {
            return;
        }
        notify(new SyncMessage(this.localId, modelId, SyncMessage.OP_REMOVE_POLICY, sec, ptype, Arrays.asList(paras)));
    }

    @Override
    public void updateForRemoveFilteredPolicy(String modelId, String sec, String ptype, int fieldIndex, String... fieldValues) {
        List<String> params = new ArrayList();
        params.add(SyncMessage.OP_REMOVE_POLICY);
        params.add(modelId);
        params.add(sec);
        params.add(ptype);
        params.add(fieldIndex+"");
        params.addAll(Arrays.asList(fieldValues));
        if(!needBroadcast(params)) {
            return;
        }
        SyncMessage msg = new SyncMessage(this.localId, modelId, SyncMessage.OP_REMOVE_POLICY, sec, ptype, Arrays.asList(fieldValues));
        msg.setFilterIndex(fieldIndex);
        notify(msg);
    }

    @Override
    public void updateForSavePolicy(String modelId, Model model) {
        List<String> params = new ArrayList();
        params.add(SyncMessage.OP_SAVE_POLICY);
        params.add(modelId);
        if(!needBroadcast(params)) {
            return;
        }
        notify(new SyncMessage(this.localId, modelId, SyncMessage.OP_SAVE_POLICY, "", "", new ArrayList<>()));
    }

    private boolean needBroadcast(List<String> params) {
        String s = String.join("/", params);
        String h = DigestUtils.md5Hex(s);
        String key = "casbin/cache/" + h;
        try (Jedis jedis = jedisPool.getResource()) {
            if(jedis.exists(key)) {
                logger.info("redis exist {}, not need to broadcast", key);
                return false;
            }
            jedis.set(key, s);
            jedis.expire(key, EXPIRE_TIME);
            logger.info("to prevent broadcast storm, generate redis key {} content {} expired {}", key, s, EXPIRE_TIME);
        } catch (Exception e) {
            logger.error(String.format("get redis failed: %s",e.getMessage()), e);
        }
        return true;
    }
}
