package RegionManagers.SocketManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/** FTP 客户端封装，供 Region 同步表文件与 catalog。 */
public class FtpUtils {

    public String hostname = "10.181.241.133";
    public int port = 21;
    public String username = "test";
    public String password = "test";
    private static final int IO_BUFFER = 4 * 1024 * 1024;
    public FTPClient ftpClient = null;

    private void openSession() {
        ftpClient = new FTPClient();
        ftpClient.setControlEncoding("utf-8");
        try {
            ftpClient.connect(hostname, port);
            ftpClient.login(username, password);
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            ftpClient.setBufferSize(IO_BUFFER);
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                closeSession();
                System.out.println("FTP服务器连接失败");
            }
        } catch (Exception e) {
            System.out.println("FTP登录失败" + e.getMessage());
        }
    }

    private void closeSession() {
        if (ftpClient != null && ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException e) {
                System.out.println("关闭FTP连接失败" + e.getMessage());
            }
        }
    }

    public Boolean downLoadFile(String ftpPath, String fileName, String savePath) {
        openSession();
        OutputStream os = null;
        if (ftpClient != null) {
            try {
                if (!ftpClient.changeWorkingDirectory(ftpPath)) {
                    System.out.println("/" + ftpPath + "该目录不存在");
                    return false;
                }
                ftpClient.enterLocalPassiveMode();

                FTPFile[] ftpFiles = ftpClient.listFiles();

                if (ftpFiles == null || ftpFiles.length == 0) {
                    System.out.println("/" + ftpPath + "该目录下无文件");
                    return false;
                }
                for(FTPFile file : ftpFiles){
                    if (nameMatches(fileName, file.getName())) {
                        if(!file.isDirectory()) {
                            File saveFile = new File(savePath + file.getName());
                            os = new FileOutputStream(saveFile);
                            ftpClient.retrieveFile(file.getName(), os);
                            os.close();
                        }
                    }
                }
                return true;
            } catch (IOException e) {
                System.out.println("下载文件失败" + e.getMessage());
            } finally {
                if(null != os) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                closeSession();
            }
        }
        return false;
    }

    public boolean additionalDownloadFile(String ftpPath, String fileName) {
        openSession();
        OutputStream os = null;
        if (ftpClient != null) {
            try {
                if (!ftpClient.changeWorkingDirectory(ftpPath)) {
                    System.out.println("/" + ftpPath + "该目录不存在");
                    return false;
                }
                ftpClient.enterLocalPassiveMode();

                FTPFile[] ftpFiles = ftpClient.listFiles();

                if (ftpFiles == null || ftpFiles.length == 0) {
                    System.out.println("/" + ftpPath + "该目录下无文件");
                    return false;
                }
                for(FTPFile file : ftpFiles){
                    if (nameMatches(fileName, file.getName())) {
                        if(!file.isDirectory()) {
                            File saveFile = new File(file.getName().split("#")[1]);
                            os = new FileOutputStream(saveFile, true);
                            ftpClient.retrieveFile(file.getName(), os);
                            os.close();
                        }
                    }
                }
                return true;
            } catch (IOException e) {
                System.out.println("下载文件失败" + e.getMessage());
            } finally {
                if(null != os) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                closeSession();
            }
        }
        return false;
    }

    public boolean uploadFile(String fileName, String savePath) {
        openSession();
        boolean flag = false;
        InputStream inputStream = null;
        if (ftpClient != null) {
            try{
                inputStream = new FileInputStream(new File(fileName));
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                ftpClient.makeDirectory(savePath);
                ftpClient.changeWorkingDirectory(savePath);
                ftpClient.storeFile(fileName, inputStream);
                inputStream.close();
                flag = true;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(null != inputStream) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                closeSession();
            }
        }
        return flag;
    }

    public boolean uploadFile(String fileName, String IP, String savePath) {
        openSession();
        boolean flag = false;
        InputStream inputStream = null;
        if (ftpClient != null) {
            try{
                inputStream = new FileInputStream(new File(fileName));
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                ftpClient.makeDirectory(savePath);
                ftpClient.changeWorkingDirectory(savePath);
                ftpClient.storeFile(fileName, inputStream);
                ftpClient.rename(fileName, "/catalog/" + IP + "#" + fileName);
                inputStream.close();
                flag = true;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(null != inputStream) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                closeSession();
            }
        }
        return flag;
    }

    public boolean deleteFile(String fileName, String filePath) {
        openSession();
        boolean flag = false;
        if (ftpClient != null) {
            try {
                ftpClient.changeWorkingDirectory(filePath);
                ftpClient.dele(fileName);
                flag = true;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeSession();
            }
        }
        return flag;
    }

    private static boolean nameMatches(String expected, String actual) {
        return expected.equals("") || expected.equalsIgnoreCase(actual);
    }
}
