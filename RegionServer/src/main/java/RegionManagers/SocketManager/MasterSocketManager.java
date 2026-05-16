package RegionManagers.SocketManager;

import RegionManagers.DataBaseManager;
import miniSQL.API;
import miniSQL.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/** 与 Master 的长连接：元数据上报与容灾指令处理。 */
public class MasterSocketManager implements Runnable {

    private static final long POLL_MS = 100L;
    private static final String TAG_DISASTER = "<master>[3]";
    private static final String TAG_RECOVER = "<master>[4]recover";
    private static final int DISASTER_HEAD_LEN = 11;

    private final Socket channel;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final FtpUtils ftp;
    private final DataBaseManager catalog;
    private boolean alive;

    public final int SERVER_PORT = 12345;
    public final String MASTER = "localhost";

    public MasterSocketManager() throws IOException {
        this.channel = new Socket(MASTER, SERVER_PORT);
        this.ftp = new FtpUtils();
        this.catalog = new DataBaseManager();
        this.reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
        this.writer = new PrintWriter(channel.getOutputStream(), true);
        this.alive = true;
    }

    public void sendToMaster(String payload) {
        writer.println(payload);
    }

    public void sendTableInfoToMaster(String tableList) {
        writer.println("<region>[1]" + tableList);
    }

    public void receiveFromMaster() throws IOException {
        if (channelBroken()) {
            System.out.println("新消息>>>Socket已经关闭!");
            return;
        }

        String cmd = reader.readLine();
        if (cmd == null) {
            return;
        }
        routeMasterCommand(cmd);
    }

    private void routeMasterCommand(String cmd) throws IOException {
        if (cmd.startsWith(TAG_DISASTER)) {
            executeDisasterPlan(cmd);
        } else if (TAG_RECOVER.equals(cmd)) {
            executeOnlineRecover();
        }
    }

    private void executeDisasterPlan(String cmd) throws IOException {
        String body = cmd.substring(DISASTER_HEAD_LEN);
        if (cmd.length() == DISASTER_HEAD_LEN) {
            return;
        }

        String[] chunks = body.split("#");
        String ownerIp = chunks[0];
        String[] tables = chunks[1].split("@");

        for (String table : tables) {
            pullTableBundle(table);
        }

        ftp.additionalDownloadFile("catalog", ownerIp + "#table_catalog");
        ftp.additionalDownloadFile("catalog", ownerIp + "#index_catalog");
        reloadEngine();
        System.out.println("here");
        writer.println("<region>[3]Complete disaster recovery");
    }

    private void pullTableBundle(String table) {
        wipeLocal(table);
        wipeLocal(table + "_index.index");
        ftp.downLoadFile("table", table, "");
        System.out.println("success " + table);
        ftp.downLoadFile("index", table + "_index.index", "");
        System.out.println("success " + table + "_index.index");
    }

    private void executeOnlineRecover() throws IOException {
        String[] tables = catalog.getMetaInfo().split(" ");
        for (String table : tables) {
            Interpreter.interpret("drop table " + table + " ;");
            persistAndReloadEngine();
        }
        writer.println("<master>[4]Online");
    }

    private void reloadEngine() {
        try {
            API.initial();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void persistAndReloadEngine() {
        try {
            API.store();
            API.initial();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delFile(String fileName) {
        wipeLocal(fileName);
    }

    private static void wipeLocal(String fileName) {
        File target = new File(fileName);
        if (target.exists() && target.isFile()) {
            target.delete();
        }
    }

    private boolean channelBroken() {
        return channel.isClosed() || channel.isInputShutdown() || channel.isOutputShutdown();
    }

    @Override
    public void run() {
        System.out.println("新消息>>>从节点的主服务器监听线程启动！");
        while (alive) {
            if (channelBroken()) {
                alive = false;
                break;
            }
            try {
                receiveFromMaster();
            } catch (IOException e) {
                e.printStackTrace();
            }
            sleepBriefly();
        }
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(POLL_MS);
        } catch (InterruptedException | NullPointerException e) {
            e.printStackTrace();
        }
    }
}
