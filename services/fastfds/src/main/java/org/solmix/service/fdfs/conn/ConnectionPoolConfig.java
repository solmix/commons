package org.solmix.service.fdfs.conn;

import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

/**
 * 连接池配置
 * 
 * @author tobato
 *
 */
public class ConnectionPoolConfig extends GenericKeyedObjectPoolConfig {

    public ConnectionPoolConfig() {
        setMaxTotal(50); // 从池中借出的对象的最大数目50
        setTestWhileIdle(true);
        setBlockWhenExhausted(true);
        setMaxWaitMillis(100);
        setMinEvictableIdleTimeMillis(180000); // 视休眠时间超过了180秒的对象为过期
        setTimeBetweenEvictionRunsMillis(60000); // 每过60秒进行一次后台对象清理的行动
        setNumTestsPerEvictionRun(-1);
    }

}