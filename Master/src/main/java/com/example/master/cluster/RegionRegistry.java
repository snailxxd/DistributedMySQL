package com.example.master.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 维护所有 RegionServer 节点（含历史与当前活跃）。
 * 线程安全：所有读写都基于 ConcurrentHashMap，状态变更短临界区加锁。
 */
public final class RegionRegistry {

    /** 历史 + 当前所有见过的节点（key: regionId）。 */
    private final Map<String, RegionNode> nodes = new ConcurrentHashMap<>();

    /** 是否含有该 regionId（包含历史失效的）。 */
    public boolean isKnown(String regionId) {
        return nodes.containsKey(regionId);
    }

    /** 该 region 当前是否为活跃状态。 */
    public boolean isActive(String regionId) {
        RegionNode node = nodes.get(regionId);
        return node != null && node.getStatus() == RegionNode.Status.ACTIVE;
    }

    /** 第一次见到的节点入册并置为 ACTIVE。 */
    public RegionNode addNew(String regionId, String host, int port) {
        RegionNode node = new RegionNode(regionId, host, port, RegionNode.Status.ACTIVE);
        nodes.put(regionId, node);
        return node;
    }

    /** 把之前失效的节点恢复为 ACTIVE。 */
    public RegionNode markActive(String regionId, String host, int port) {
        RegionNode node = nodes.get(regionId);
        if (node == null) {
            return addNew(regionId, host, port);
        }
        node.setStatus(RegionNode.Status.ACTIVE);
        return node;
    }

    /** 把节点标记为失效。 */
    public RegionNode markInvalid(String regionId) {
        RegionNode node = nodes.get(regionId);
        if (node != null) {
            node.setStatus(RegionNode.Status.INVALID);
        }
        return node;
    }

    public RegionNode get(String regionId) {
        return nodes.get(regionId);
    }

    public List<RegionNode> listActive() {
        return nodes.values().stream()
                .filter(n -> n.getStatus() == RegionNode.Status.ACTIVE)
                .collect(Collectors.toList());
    }

    public Set<String> activeIds() {
        return nodes.values().stream()
                .filter(n -> n.getStatus() == RegionNode.Status.ACTIVE)
                .map(RegionNode::getRegionId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public Collection<RegionNode> all() {
        return new ArrayList<>(nodes.values());
    }
}
