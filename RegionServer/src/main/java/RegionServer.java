import RegionManagers.RegionManager;

/**
 * Region 从节点入口；在 Master 侧注册时节点名前缀为 Region_，data 为 url。
 */
public final class RegionServer {

    private RegionServer() {
    }

    public static void main(String[] args) throws Exception {
        boot();
    }

    private static void boot() throws Exception {
        RegionManager node = new RegionManager();
        node.run();
    }
}
