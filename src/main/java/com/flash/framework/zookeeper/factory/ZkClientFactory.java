package com.flash.framework.zookeeper.factory;

import com.flash.framework.zookeeper.autoconfigure.ZkConfigure;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author zhurg
 * @date 2019/3/29 - 上午11:21
 */
@Slf4j
public class ZkClientFactory {

    private CuratorFramework client;

    private final ZkConfigure zkConfigure;

    public ZkClientFactory(ZkConfigure zkConfigure) {
        this.zkConfigure = zkConfigure;
    }

    @PostConstruct
    public void init() {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString(zkConfigure.getServers())
                .retryPolicy(new ExponentialBackoffRetry(zkConfigure.getBaseSleepTimeMilliseconds(), zkConfigure.getMaxRetries(), zkConfigure.getMaxSleepTimeMilliseconds()));
        if (0 != zkConfigure.getSessionTimeoutMilliseconds()) {
            builder.sessionTimeoutMs(zkConfigure.getSessionTimeoutMilliseconds());
        }
        if (0 != zkConfigure.getConnectionTimeoutMilliseconds()) {
            builder.connectionTimeoutMs(zkConfigure.getConnectionTimeoutMilliseconds());
        }
        if (!Strings.isNullOrEmpty(zkConfigure.getDigest())) {
            builder.authorization("digest", zkConfigure.getDigest().getBytes(Charsets.UTF_8))
                    .aclProvider(new ACLProvider() {

                        @Override
                        public List<ACL> getDefaultAcl() {
                            return ZooDefs.Ids.CREATOR_ALL_ACL;
                        }

                        @Override
                        public List<ACL> getAclForPath(final String path) {
                            return ZooDefs.Ids.CREATOR_ALL_ACL;
                        }
                    });
        }
        client = builder.build();
        client.start();
        try {
            if (!client.blockUntilConnected(zkConfigure.getMaxSleepTimeMilliseconds() * zkConfigure.getMaxRetries(), TimeUnit.MILLISECONDS)) {
                client.close();
                throw new KeeperException.OperationTimeoutException();
            }
        } catch (final Exception ex) {
            log.error("[Zookeeper] CuratorFramework close fail ", ex);
        }
    }

    public CuratorFramework get() {
        return this.client;
    }

    /**
     * 删除注册数据
     *
     * @param key
     */
    public void remove(final String key) {
        try {
            client.delete().deletingChildrenIfNeeded().forPath(key);
        } catch (final Exception ex) {
            log.error("[Zookeeper] remove key {} failed ", key, ex);
        }
    }

    /**
     * 更新注册数据
     *
     * @param key
     * @param value
     */
    public void update(final String key, final String value) {
        try {
            client.transactionOp().setData().forPath(key, value.getBytes(Charsets.UTF_8));
            //client.inTransaction().check().forPath(key).and().setData().forPath(key, value.getBytes(Charsets.UTF_8)).and().commit();
        } catch (final Exception ex) {
            log.error("[Zookeeper] update key {} value {} failed ", key, value, ex);
        }
    }

    /**
     * 持久化注册数据
     *
     * @param key
     * @param value
     */
    public void persist(final String key, final String value) {
        try {
            if (!isExisted(key)) {
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(key, value.getBytes(Charsets.UTF_8));
            } else {
                update(key, value);
            }
        } catch (final Exception ex) {
            log.error("[Zookeeper] persist key {} value {} failed ", key, value, ex);
        }
    }

    /**
     * 获取注册数据
     *
     * @param key
     * @return
     */
    public String get(final String key) {
        try {
            return new String(client.getData().forPath(key), Charsets.UTF_8);
        } catch (final Exception ex) {
            log.error("[Zookeeper] get key {} failed ", key, ex);
            return null;
        }
    }

    /**
     * 获取数据是否存在.
     *
     * @param key
     * @return
     */
    public boolean isExisted(final String key) {
        try {
            return null != client.checkExists().forPath(key);
        } catch (final Exception ex) {
            log.error("[Zookeeper] isExisted key {} failed ", key, ex);
            return false;
        }
    }

    /**
     * 持久化临时注册数据
     *
     * @param key
     * @param value
     */
    public void persistEphemeral(final String key, final String value) {
        try {
            if (isExisted(key)) {
                client.delete().deletingChildrenIfNeeded().forPath(key);
            }
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(key, value.getBytes(Charsets.UTF_8));
        } catch (final Exception ex) {
            log.error("[Zookeeper] persistEphemeral key {} value {} failed ", key, value, ex);
        }
    }

    /**
     * 持久化顺序注册数据
     *
     * @param key
     * @param value
     * @return
     */
    public String persistSequential(final String key, final String value) {
        try {
            return client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT_SEQUENTIAL).forPath(key, value.getBytes(Charsets.UTF_8));
        } catch (final Exception ex) {
            log.error("[Zookeeper] persistSequential key {} value {} failed ", key, value, ex);
        }
        return null;
    }

    /**
     * 持久化临时顺序注册数据
     *
     * @param key
     */
    public void persistEphemeralSequential(final String key) {
        try {
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(key);
        } catch (final Exception ex) {
            log.error("[Zookeeper] persistEphemeralSequential key {} failed ", key, ex);
        }
    }

    @PreDestroy
    public void close() {
        CloseableUtils.closeQuietly(client);
    }
}