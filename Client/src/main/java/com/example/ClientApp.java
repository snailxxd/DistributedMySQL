package com.example;

import com.example.client.ClientManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ClientApp {

    /**
     * 客户端进程
     * @param args [masterHost] [masterPort]
     */
    public static void main(String[] args) {
        ClientManager clientManager;
        if (args.length >= 2) {
            String host = args[0];
            int port;
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid master port: " + args[1]);
                return;
            }
            clientManager = new ClientManager(host, port);
        } else {
            String host = null;
            Integer port = null;
            System.out.println("Please enter [MasterHost] [MasterPort]");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String line;
                try {
                    line = reader.readLine();
                } catch (IOException e) {
                    System.out.println("Input error: " + e.getMessage());
                    return;
                }
                if (line == null) {
                    System.out.println("Input closed. Exit.");
                    return;
                }
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    host = parts[0];
                    try {
                        port = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid master port: " + parts[1]);
                        continue;
                    }
                    break;
                }
                System.out.println("Please enter [masterhost] [masterport]");
            }
            clientManager = new ClientManager(host, port);
        }
        clientManager.run();
    }
}