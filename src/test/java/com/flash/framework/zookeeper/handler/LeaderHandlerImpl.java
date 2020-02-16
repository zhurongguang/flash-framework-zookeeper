package com.flash.framework.zookeeper.handler;

import org.springframework.stereotype.Component;

/**
 * @author
 * @date 2019/4/17 - 上午10:44
 */
@Component
public class LeaderHandlerImpl implements LeaderHandler{


    @Override
    public void leaderHandle() {
        System.out.println("----------> I am leader");
    }
}