package com.example.master.balancer;

import com.example.master.cluster.RegionNode;
import com.example.master.cluster.RegionRegistry;
import com.example.master.meta.TableMetaStore;

import java.util.List;

/**
 * 负载均衡器：从活跃 RegionServer 中挑选当前承载表数最少的。
 */
public final class LoadBalancer {

    private final RegionRegistry registry;
    private final TableMetaStore metaStore;

    public LoadBalancer(RegionRegistry registry, TableMetaStore metaStore) {
        this.registry = registry;
        this.metaStore = metaStore;
    }

    /** 在所有活跃 region 中选最少表的，没有活跃节点返回 null。 */
    public RegionNode pick() {
        return pickExcluding(null);
    }

    /** 在活跃 region 中选最少表的，排除指定 regionId。 */
    public RegionNode pickExcluding(String excludeRegionId) {
        List<RegionNode> actives = registry.listActive();
        RegionNode best = null;
        int bestCount = Integer.MAX_VALUE;
        for (RegionNode node : actives) {
            if (excludeRegionId != null && excludeRegionId.equals(node.getRegionId())) {
                continue;
            }
            int count = metaStore.tableCount(node.getRegionId());
            if (count < bestCount) {
                bestCount = count;
                best = node;
            }
        }
        return best;
    }
}
