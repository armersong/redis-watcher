package org.casbin.watcher;

import org.casbin.jcasbin.model.Model;

public interface SyncCoordinator {
    Model getModel(String modelId);
}
