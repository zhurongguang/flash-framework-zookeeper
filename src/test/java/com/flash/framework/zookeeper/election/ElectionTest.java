package com.flash.framework.zookeeper.election;

import com.flash.framework.zookeeper.ZookeeperApplication;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zhurg
 * @date 2019/3/29 - 下午3:06
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ZookeeperApplication.class})
public class ElectionTest {

    @Autowired
    private ElectionProcesser electionProcesser;

    private ScheduledExecutorService scheduledExecutorService;

    @Before
    public void init() {
        this.scheduledExecutorService = new ScheduledThreadPoolExecutor(1, new BasicThreadFactory.Builder()
                .namingPattern("demo-%d").daemon(false).build());
    }

    @Test
    public void test01() throws InterruptedException {
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            electionProcesser.reelection();
        }, 0, 10, TimeUnit.SECONDS);
        Thread.sleep(Integer.MAX_VALUE);
    }

    @Test
    public void test02() throws InterruptedException {
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            electionProcesser.reelection();
        }, 0, 20, TimeUnit.SECONDS);
        Thread.sleep(Integer.MAX_VALUE);
    }
}