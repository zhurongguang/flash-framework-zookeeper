package com.flash.framework.zookeeper.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author zhurg
 * @date 2019/3/29 - 上午11:14
 */
@Data
@ConfigurationProperties(prefix = "zk")
public class ZkConfigure {

    private String servers;

    /**
     * 命名空间.
     */
    private String namespace = "ffzk";

    /**
     * 等待重试的间隔时间的初始值.
     * 单位毫秒.
     */
    private int baseSleepTimeMilliseconds = 1000;

    /**
     * 等待重试的间隔时间的最大值.
     * 单位毫秒.
     */
    private int maxSleepTimeMilliseconds = 3000;

    /**
     * 最大重试次数.
     */
    private int maxRetries = 3;

    /**
     * 会话超时时间.
     * 单位毫秒.
     */
    private int sessionTimeoutMilliseconds;

    /**
     * 连接超时时间.
     * 单位毫秒.
     */
    private int connectionTimeoutMilliseconds;

    /**
     * 连接Zookeeper的权限令牌.
     * 缺省为不需要权限验证.
     */
    private String digest;

    /**
     * 选举数据存放的目录
     */
    private String leadLatchPath = "/leader";
}