package org.casbin.watcher;

import org.casbin.jcasbin.main.Enforcer;

public interface SyncCoordinator {
    Enforcer getEnforcer(String modelId);
}
