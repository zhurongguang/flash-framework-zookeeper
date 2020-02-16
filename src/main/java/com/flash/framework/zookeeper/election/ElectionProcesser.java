package com.flash.framework.zookeeper.election;

import com.flash.framework.zookeeper.autoconfigure.ZkConfigure;
import com.flash.framework.zookeeper.factory.ZkClientFactory;
import com.flash.framework.zookeeper.handler.LeaderHandler;
import com.flash.framework.zookeeper.handler.StandByHandler;
import com.flash.framework.zookeeper.utils.HostNameUtil;
import com.flash.framework.zookeeper.utils.PidUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.utils.CloseableUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 选举处理
 *
 * @author zhurg
 * @date 2019/3/29 - 下午2:43
 */
@Slf4j
public class ElectionProcesser implements EnvironmentAware {

    @Autowired
    private ApplicationContext applicationContext;

    private LeaderLatch leaderLatch;

    private final ZkConfigure zkConfigure;

    private final ZkClientFactory zkClientFactory;

    private LeaderHandler leaderHandler;

    private StandByHandler standByHandler;

    private String id;

    private ScheduledExecutorService scheduledExecutorService;

    private AtomicBoolean standbyInit = new AtomicBoolean(false);

    public ElectionProcesser(ZkConfigure zkConfigure, ZkClientFactory zkClientFactory) {
        this.zkConfigure = zkConfigure;
        this.zkClientFactory = zkClientFactory;

        this.scheduledExecutorService = new ScheduledThreadPoolExecutor(1, new BasicThreadFactory.Builder()
                .namingPattern("zookeeper-election-thread-%d").daemon(true).build());
    }

    @PostConstruct
    public void init() {
        if (!zkClientFactory.isExisted(zkConfigure.getLeadLatchPath())) {
            try {
                zkClientFactory.get().createContainers(zkConfigure.getLeadLatchPath());
            } catch (Exception e) {
                log.error("[Zookeeper] create path {} failed", zkConfigure.getLeadLatchPath(), e);
            }
        }
        if (null == leaderHandler) {
            try {
                leaderHandler = applicationContext.getBean(LeaderHandler.class);
            } catch (Exception e) {
                log.warn("[Zookeeper] LeaderHandler not fund in Spring Bean");
            }
        }
        if (null == standByHandler) {
            try {
                standByHandler = applicationContext.getBean(StandByHandler.class);
            } catch (Exception e) {
                log.warn("[Zookeeper] StandByHandler not fund in Spring Bean");
            }
        }

        leaderLatch = new LeaderLatch(zkClientFactory.get(), zkConfigure.getLeadLatchPath(), id);
        leaderLatch.addListener(new LeaderLatchListener() {

            @Override
            public void isLeader() {
                if (log.isDebugEnabled()) {
                    log.debug("[Zookeeper] current application is leader");
                }
                log.info("[Zookeeper] election current node from {} to Master", ElectionContext.pre);
                ElectionContext.pre = ElectionContext.curr;
                ElectionContext.curr = ElectionContext.Node.Master;
                if (null != leaderHandler) {
                    leaderHandler.leaderHandle();
                }
            }

            @Override
            public void notLeader() {
                if (log.isDebugEnabled()) {
                    log.debug("[Zookeeper] current application is not leader");
                }
                log.info("[Zookeeper] election current node from {} to Slave", ElectionContext.pre);
                ElectionContext.pre = ElectionContext.curr;
                ElectionContext.curr = ElectionContext.Node.Slave;
                if (null != standByHandler) {
                    standByHandler.standByHandle();
                }
            }
        });


        try {
            if (null != leaderLatch) {
                leaderLatch.start();
            }
        } catch (Exception e) {
            log.error("[Zookeeper] Election fail ", e);
        }

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            if (StringUtils.isEmpty(currentLeaderId())) {
                return;
            }
            if (isLeader()) {
                return;
            }
            boolean standby = !standbyInit.getAndSet(true) || (ElectionContext.pre.equals(ElectionContext.Node.Master) && ElectionContext.curr.equals(ElectionContext.Node.Slave));
            if (standby) {
                log.info("[Zookeeper] election current node from {} to Slave", ElectionContext.pre);
                ElectionContext.pre = ElectionContext.curr;
                ElectionContext.curr = ElectionContext.Node.Slave;
                if (null != standByHandler) {
                    standByHandler.standByHandle();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * @return
     * @throws Exception
     */
    public boolean isLeader() {
        return leaderLatch.hasLeadership();
    }

    /**
     * @return
     * @throws Exception
     */
    public String currentLeaderId() {
        try {
            return leaderLatch.getLeader().getId();
        } catch (Exception e) {
            log.error("[Zookeeper] get leader id failed ", e);
            return null;
        }
    }

    /**
     * 重新选举
     */
    public void reelection() {
        close();
        init();
    }

    @PreDestroy
    public void close() {
        CloseableUtils.closeQuietly(leaderLatch);
        standbyInit.getAndSet(false);
    }

    @Override
    public void setEnvironment(Environment environment) {
        String applicationName = environment.getProperty("spring.application.name");
        if (StringUtils.isEmpty(applicationName)) {
            applicationName = HostNameUtil.getIp();
        }
        this.id = applicationName + "@" + PidUtil.getPid();
    }
}