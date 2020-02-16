package com.flash.framework.zookeeper.autoconfigure;

import com.flash.framework.zookeeper.election.ElectionProcesser;
import com.flash.framework.zookeeper.factory.ZkClientFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhurg
 * @date 2019/3/29 - 上午11:16
 */
@Configuration
@ConditionalOnProperty({"zk.servers"})
@EnableConfigurationProperties(ZkConfigure.class)
public class ZkConfiguration {

    @Bean
    @ConditionalOnMissingBean(ZkClientFactory.class)
    public ZkClientFactory zkClientFactory(ZkConfigure zkConfigure) {
        return new ZkClientFactory(zkConfigure);
    }

    @Bean
    @ConditionalOnBean(ZkClientFactory.class)
    @ConditionalOnProperty(prefix = "zk", name = "election.enable", havingValue = "true")
    public ElectionProcesser electionHandler(ZkConfigure zkConfigure, ZkClientFactory zkClientFactory) {
        return new ElectionProcesser(zkConfigure, zkClientFactory);
    }
}