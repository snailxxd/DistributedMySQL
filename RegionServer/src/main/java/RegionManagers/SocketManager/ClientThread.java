package RegionManagers.SocketManager;

import MasterManagers.utils.SocketUtils;
import miniSQL.API;
import miniSQL.Interpreter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/** 单 Client 会话：执行 SQL、回包、同步 FTP、按需通知 Master。 */
public class ClientThread implements Runnable {

    private static final long READ_POLL_MS = 1000L;
    private static final String MASTER_NOOP = "No modified";
    private static final String RESULT_TAG = "<result>";

    private final Socket peer;
    private final MasterSocketManager masterLink;
    private final FtpUtils ftp;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private boolean active;

    public ClientThread(Socket peer, MasterSocketManager masterLink) throws IOException {
        this.peer = peer;
        this.masterLink = masterLink;
        this.active = true;
        this.ftp = new FtpUtils();
        this.reader = new BufferedReader(new InputStreamReader(peer.getInputStream()));
        this.writer = new PrintWriter(peer.getOutputStream(), true);
        System.out.println("服务端建立了新的客户端子线程：" + peer.getPort());
    }

    @Override
    public void run() {
        System.out.println("服务器监听客户端消息中" + peer.getInetAddress() + peer.getPort());
        try {
            while (active) {
                Thread.sleep(READ_POLL_MS);
                String sql = reader.readLine();
                if (sql == null) {
                    continue;
                }
                String masterMsg = commandProcess(sql, peer.getInetAddress().toString());
                forwardToMasterIfNeeded(masterMsg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void forwardToMasterIfNeeded(String masterMsg) {
        if (!MASTER_NOOP.equals(masterMsg)) {
            masterLink.sendToMaster(masterMsg);
        }
    }

    public void sendToClient(String body) {
        writer.println(RESULT_TAG + body);
    }

    public String commandProcess(String sql, String clientIp) throws Exception {
        System.out.println("要处理的命令：" + sql);
        String interpretOut = Interpreter.interpret(sql);
        API.store();
        sendToClient(interpretOut);
        pushCatalogToFtp();

        return buildMasterNotification(sql, interpretOut);
    }

    private String buildMasterNotification(String sql, String interpretOut) throws Exception {
        String[] sqlParts = sql.split(" ");
        String[] outParts = interpretOut.split(" ");
        if (outParts.length == 0) {
            return MASTER_NOOP;
        }

        String opcode = outParts[0];
        switch (opcode) {
            case "-->Create":
                mirrorTableToFtp(outParts[2]);
                return regionSchemaMsg(outParts[2], " add");
            case "-->Drop":
                purgeTableOnFtp(outParts[2]);
                return regionSchemaMsg(outParts[2], " delete");
            case "-->Insert":
            case "-->Delete":
                refreshTableOnFtp(sqlParts[2]);
                return MASTER_NOOP;
            default:
                return MASTER_NOOP;
        }
    }

    private static String regionSchemaMsg(String table, String action) {
        return "<region>[2]" + table + action;
    }

    private void refreshTableOnFtp(String table) {
        System.out.println(table);
        purgeTableOnFtp(table);
        mirrorTableToFtp(table);
        System.out.println("success");
    }

    private void mirrorTableToFtp(String table) {
        ftp.uploadFile(table, "table");
        ftp.uploadFile(table + "_index.index", "index");
    }

    private void purgeTableOnFtp(String table) {
        ftp.deleteFile(table, "table");
        ftp.deleteFile(table + "_index.index", "index");
    }

    private void pushCatalogToFtp() {
        String hostTag = SocketUtils.getHostAddress();
        ftp.uploadFile("table_catalog", hostTag, "catalog");
        ftp.uploadFile("index_catalog", hostTag, "catalog");
    }
}
