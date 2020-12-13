package com.github.rawsanj.zk;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

/**
 * 分布式配置中心demo
 * @author
 *
 */
@Component
public class SpringCloudZk  {

	@Autowired
	private DiscoveryClient discoveryClient;

}
