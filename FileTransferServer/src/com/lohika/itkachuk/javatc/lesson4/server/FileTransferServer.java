package com.lohika.itkachuk.javatc.lesson4.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created with IntelliJ IDEA.
 * User: itkachuk
 * Date: 3/18/13 3:21 PM
 */

public class FileTransferServer extends Thread {
    private int port;
    public static final int BUFFER_SIZE = 1024;
    public static final String DEFAULT_TARGET_PATH = "use current directory";

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(getPort());

            while (true) {
                Socket socket = serverSocket.accept();
                saveFile(socket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Input error: server port should be from range 1-65535");
        }
        this.port = port;
    }

    private void saveFile(Socket socket) throws Exception {
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        FileOutputStream fos = null;
        byte [] buffer;
        String fileName = null;
        String targetPath = null;

        // 1. Read file name.
        Object o = ois.readObject();
        if (o instanceof String) {
            fileName = o.toString();
        } else {
            throwException("File transfer error: file name wasn't received");
        }

        // 2. Read target path
        o = ois.readObject();
        if (o instanceof String) {
            targetPath = o.toString();
        } else {
            throwException("File transfer error: file target path wasn't received");
        }

        // build file name and create FileOutputStream
        if (targetPath.equals(DEFAULT_TARGET_PATH)) {
            fos = new FileOutputStream(fileName); // use current directory for saving file
        } else {
            File targetDir = new File(targetPath);
            if (!targetDir.isDirectory()) {
                throwException("File transfer error: wrong target path (not a directory)");
            } else {
                fos = new FileOutputStream(targetDir + File.separator + fileName); // use received target path
            }
        }

        try {
            // 3. Read file to the end.
            Integer bytesRead;
            do {
                o = ois.readObject();

                if (!(o instanceof Integer)) {
                    throwException("File transfer error: buffer size wasn't received");
                }

                bytesRead = (Integer)o;

                o = ois.readObject();

                if (!(o instanceof byte[])) {
                    throwException("File transfer error: bytes array reading failed");
                }

                buffer = (byte[])o;

                // 4. Write data to output file.
                fos.write(buffer, 0, bytesRead);
            } while (bytesRead == BUFFER_SIZE);
        } catch (IOException ioe) {
            // if file transfer was interrupted by client, or by other reason, we need to remove partially loaded file (since we are not supporting resume download)
            fos.close();
            File file;
            if (targetPath.equals(DEFAULT_TARGET_PATH)) {
                file = new File(fileName); // use current directory
            } else {
                file = new File(targetPath + File.separator + fileName); // use received target path
            }
            try {
                file.delete();
            } catch (Exception e) {}
        } finally {
            try {
                fos.close();
                ois.close();
                oos.close();
            } catch (Exception e) {}
        }
    }

    public static void throwException(String message) throws Exception {
        throw new Exception(message);
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java FileTransferServer <port>");
            System.exit(1);
        }
        int port = 0;
        try {
            port = Integer.valueOf(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Input error: port parameter is not a number");
            System.exit(1);
        }

        FileTransferServer server = new FileTransferServer();
        server.setPort(port);
        server.start();
    }
}
