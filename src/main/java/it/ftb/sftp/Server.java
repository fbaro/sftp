package it.ftb.sftp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.FileSystems;

public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocketChannel ch = ServerSocketChannel.open();
        ch.bind(new InetSocketAddress(23));
        while (true) {
            SocketChannel clientChannel = ch.accept();
            ClientHandler.start(clientChannel, FileSystems.getDefault());
        }
    }

}
