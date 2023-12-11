package com.moyu.test.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author xiaomingzhang
 * @date 2023/7/5
 */
public class TcpServer {

    private int port = 8888;

    public static void main(String[] args) {
        TcpServer tcpServer = new TcpServer();
        tcpServer.listen();
    }


    public TcpServer() {

    }

    public TcpServer(int port) {
        this.port = port;
    }

    public void listen() {
        printDatabaseMsg();
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(this.port);
            int threadNum = 0;
            while (true) {
                Socket socket = serverSocket.accept();
                TcpServerThread tcpServerThread = new TcpServerThread(socket);
                Thread thread = new Thread(tcpServerThread, "TcpServerThread-" + threadNum);
                thread.setDaemon(true);
                thread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    private void printDatabaseMsg(int port) {
        System.out.println("+-------------------------------+");
        System.out.println("|       YanySQL1.0  (^_^)        |");
        System.out.println("+-------------------------------+");
        System.out.println("TCP服务启动中，端口为" + port + "。等待连接...");
    }

    private void printDatabaseMsg() {
        System.out.println(
                "  __     __                    _____   ____   _\n" +
                        "  \\ \\   / /                   / ____| / __ \\ | |\n" +
                        "   \\ \\_/ /__ _  _ __   _   _ | (___  | |  | || |\n" +
                        "    \\   // _` || '_ \\ | | | | \\___ \\ | |  | || |\n" +
                        "     | || (_| || | | || |_| | ____) || |__| || |____\n" +
                        "     |_| \\__,_||_| |_| \\__, ||_____/  \\___\\_\\|______|\n" +
                        "                        __/ |  yanySQL 1.0\n" +
                        "                       |___/   type: TCP、port:" + this.port);
        System.out.println("等待连接...");
    }


}
