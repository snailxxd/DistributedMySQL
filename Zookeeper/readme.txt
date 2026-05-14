https://dlcdn.apache.org/zookeeper/zookeeper-3.9.5/apache-zookeeper-3.9.5-bin.tar.gz
从以上网站中下载zookeeper源文件，然后解压（比如说到D:\zookeeper），将config文件夹里的zoo_sample_cfg改名为zoo.cfg
cd C:\zookeeper\conf
copy zoo_sample.cfg zoo.cfg

随后用cmd跳转到bin文件夹，输入zkServer.cmd
cd D:\zookeeper\bin
zkServer.cmd

如果启动时提示 JAVA_HOME is not set，需要配置 Java 环境变量。