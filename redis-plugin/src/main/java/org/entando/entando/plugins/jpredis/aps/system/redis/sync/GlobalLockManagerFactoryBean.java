package org.entando.entando.plugins.jpredis.aps.system.redis.sync;

import io.lettuce.core.RedisClient;
import org.entando.entando.aps.system.services.cache.RedisEnvironmentVariables;
import org.entando.entando.aps.system.services.sync.IGlobalLockManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;

public class GlobalLockManagerFactoryBean implements FactoryBean<IGlobalLockManager>, BeanFactoryAware {

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public IGlobalLockManager getObject() {
        if (RedisEnvironmentVariables.active()) {
            return new GlobalLockManager(beanFactory.getBean(RedisClient.class));
        }
        return new org.entando.entando.aps.system.services.sync.GlobalLockManager();
    }

    @Override
    public Class<?> getObjectType() {
        return IGlobalLockManager.class;
    }
}