package RegionManagers;

import MasterManagers.ZookeeperManager;
import MasterManagers.utils.CuratorHolder;
import MasterManagers.utils.SocketUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;

import java.util.List;

/** ZooKeeper 临时节点注册，成功后阻塞本线程。 */
@Slf4j
public class zkServiceManager implements Runnable {

    private static final int REGION_NODE_PREFIX_LEN = 7;

    @Override
    public void run() {
        serviceRegister();
    }

    private void serviceRegister() {
        try {
            CuratorHolder curator = new CuratorHolder();
            String zRoot = ZookeeperManager.ZNODE;
            List<String> existing = curator.getChildren(zRoot);
            String nodeId = pickNextNodeId(existing);
            String path = buildRegisterPath() + nodeId;
            curator.createNode(path, SocketUtils.getHostAddress(), CreateMode.EPHEMERAL);
            parkThreadForever();
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    private static String pickNextNodeId(List<String> children) {
        if (children.isEmpty()) {
            return "0";
        }
        String tail = children.get(children.size() - 1);
        int prev = Integer.parseInt(tail.substring(REGION_NODE_PREFIX_LEN));
        return Integer.toString(prev + 1);
    }

    private void parkThreadForever() throws InterruptedException {
        synchronized (this) {
            wait();
        }
    }

    private static String buildRegisterPath() {
        return ZookeeperManager.ZNODE + "/" + ZookeeperManager.HOST_NAME_PREFIX;
    }
}
