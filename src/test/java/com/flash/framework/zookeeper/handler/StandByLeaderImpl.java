package com.flash.framework.zookeeper.handler;

import org.springframework.stereotype.Component;

/**
 * @author
 * @date 2019/4/17 - 上午10:45
 */
@Component
public class StandByLeaderImpl implements StandByHandler {


    @Override
    public void standByHandle() {
        System.out.println("---------> I am standby");
    }
}