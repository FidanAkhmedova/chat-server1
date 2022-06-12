package ru.geekbrains.jt.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
public class SocketThread extends Thread {
    public static final Logger logger = LogManager.getLogger(SocketThread.class);
    private final SocketThreadListener listener;
    private final Socket socket;
    private final  ExecutorService service;
    private DataOutputStream out;

    //Executors.newSingleThreadExecutor();

    public SocketThread(SocketThreadListener listener, String name, Socket socket) {
        super(name);
        this.socket = socket;
        this.listener = listener;
        this.service = Executors.newCachedThreadPool();
        start();

        service.execute(new Runnable() {
            public void run() {
                System.out.println("Another thread was executed");
            }
        });
    }

    @Override
    public void run() {
        try {
            listener.onSocketStart(this, socket);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            listener.onSocketReady(this, socket);
            while (!isInterrupted()) {
                String msg = in.readUTF();
                listener.onReceiveString(this, socket, msg);
            }
        } catch (IOException e) {
            listener.onSocketException(this, e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                listener.onSocketException(this, e);
            }
            listener.onSocketStop(this);
        }
    }
    public synchronized boolean sendMessage(String msg) {
        try {
            out.writeUTF(msg);
            out.flush();
            logger.info(msg);
            return true;
        } catch (IOException e) {
            listener.onSocketException(this, e);
            return false;
        }
    }

    public synchronized void close() {
//       service.shutdown();
        interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            listener.onSocketException(this, e);
        }
    }
}
