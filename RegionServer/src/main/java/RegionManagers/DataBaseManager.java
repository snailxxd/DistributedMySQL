package RegionManagers;

import miniSQL.CATALOGMANAGER.CatalogManager;
import miniSQL.CATALOGMANAGER.Table;
import miniSQL.INDEXMANAGER.Index;

import java.io.IOException;
import java.util.LinkedHashMap;

/** 本节点 catalog 快照，供 Master 路由。 */
public class DataBaseManager {

    private final LinkedHashMap<String, Table> tables;
    private final LinkedHashMap<String, Index> indices;

    public DataBaseManager() throws IOException {
        loadCatalogFromDisk();
        this.tables = CatalogManager.getTables();
        this.indices = CatalogManager.getIndex();
    }

    /** 表名列表，空格分隔，末尾带空格。 */
    public String getMetaInfo() {
        return formatTableNameList(this.tables);
    }

    private static void loadCatalogFromDisk() throws IOException {
        CatalogManager.initialCatalog();
    }

    private static String formatTableNameList(LinkedHashMap<String, Table> tableMap) {
        StringBuilder names = new StringBuilder();
        for (String name : tableMap.keySet()) {
            names.append(name).append(' ');
        }
        return names.toString();
    }
}
