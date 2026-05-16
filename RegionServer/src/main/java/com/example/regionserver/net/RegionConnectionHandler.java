package com.example.regionserver.net;

import com.example.regionserver.service.RSRequestHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

final class RegionConnectionHandler implements Runnable {

    private final Socket socket;
    private final RSRequestHandler handler;

    RegionConnectionHandler(Socket socket, RSRequestHandler handler) {
        this.socket = socket;
        this.handler = handler;
    }

    @Override
    public void run() {
        try (Socket s = socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {

            String line = reader.readLine();
            String response = handler.handle(line);
            writer.write(response + "\n");
            writer.flush();
        } catch (IOException e) {
            System.err.println("Region connection error: " + e.getMessage());
        }
    }
}

