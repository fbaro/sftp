package it.ftb.sftp;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TestSSL {
    public static void main(String[] args) throws IOException, InterruptedException {
        ServerSocket ss = SSLServerSocketFactory.getDefault().createServerSocket(12344);
        Socket socket = ss.accept();
        System.err.println("Incoming connection");
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        BlockingQueue<byte[]> q = new ArrayBlockingQueue<>(16);
        Thread readr = new Thread() {
            @Override
            public void run() {
                try {
                    int read;
                    byte[] buf = new byte[1024];
                    while (-1 != (read = in.read(buf))) {
                        System.err.println("Echoing data");
                        q.put(Arrays.copyOf(buf, read));
                    }
                } catch (IOException | InterruptedException ignored) {
                    ignored.printStackTrace(System.err);
                }
                try {
                    q.put(new byte[0]);
                } catch (InterruptedException e) {
                }
            }
        };
        Thread writr = new Thread() {
            @Override
            public void run() {
                try {
                    byte[] data;
                    while (null != (data = q.take())) {
                        out.write(data);
                    }
                } catch (IOException | InterruptedException ignored) {
                    ignored.printStackTrace(System.err);
                    readr.interrupt();
                }
            }
        };
        readr.start();
        writr.start();
        readr.join();
        writr.join();
    }
}
