package org.casbin.watcher;

public interface SyncListener {
    void handleMessage(SyncMessage msg);
}
