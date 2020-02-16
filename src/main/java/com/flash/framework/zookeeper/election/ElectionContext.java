package com.flash.framework.zookeeper.election;

/**
 * 选举上下文
 *
 * @author zhurg
 * @date 2019/6/24 - 上午10:38
 */
public class ElectionContext {

    public static enum Node {
        Slave,
        Master;
    }

    public static volatile Node pre;

    public static volatile Node curr;
}