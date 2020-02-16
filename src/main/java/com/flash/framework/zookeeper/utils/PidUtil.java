package com.flash.framework.zookeeper.utils;

import java.lang.management.ManagementFactory;

/**
 * @author zhurg
 * @date 2019/4/24 - 下午5:41
 */
public class PidUtil {

    /**
     * Resolve and get current process ID.
     *
     * @return current process ID
     */
    public static int getPid() {
        // Note: this will trigger local host resolve, which might be slow.
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return Integer.parseInt(name.split("@")[0]);
    }
}