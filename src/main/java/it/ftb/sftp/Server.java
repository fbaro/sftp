package it.ftb.sftp;

import com.google.common.collect.ImmutableList;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.StaticPasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystems;
import java.util.concurrent.TimeUnit;

public class Server {
    public static void main(String[] args) throws IOException, InterruptedException {
        SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setHost("127.0.0.1");
        sshd.setPort(22);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser")));
        sshd.setPasswordAuthenticator(new StaticPasswordAuthenticator(true));
        sshd.setSubsystemFactories(ImmutableList.of(new NamedFactory<Command>() {
            @Override
            public Command create() {
                return new MySftpCommand();
            }

            @Override
            public String getName() {
                return "sftp";
            }
        }));
//        sshd.setSubsystemFactories(ImmutableList.of(new SftpSubsystemFactory()));
        sshd.start();
        TimeUnit.MINUTES.sleep(10);
    }

    private static class MySftpCommand implements Command {
        private ReadableByteChannel in;
        private WritableByteChannel out;
        private OutputStream outStream;
        private OutputStream err;
        private ClientHandler handler;

        @Override
        public void setInputStream(InputStream in) {
            this.in = Channels.newChannel(in);
        }

        @Override
        public void setOutputStream(OutputStream out) {
            this.outStream = out;
            this.out = Channels.newChannel(out);
        }

        @Override
        public void setErrorStream(OutputStream err) {
            this.err = err;
        }

        @Override
        public void setExitCallback(ExitCallback callback) {
            System.out.println("setExitCallback");
        }

        @Override
        public void start(Environment env) throws IOException {
            System.out.println("start, user: " + env.getEnv().get(Environment.ENV_USER));
            this.handler = ClientHandler.start(new ByteChannel() {
                @Override
                public int read(ByteBuffer dst) throws IOException {
                    return in.read(dst);
                }

                @Override
                public int write(ByteBuffer src) throws IOException {
                    int ret = out.write(src);
                    if (!src.hasRemaining()) {
                        outStream.flush();
                    }
                    return ret;
                }

                @Override
                public boolean isOpen() {
                    return in.isOpen() && out.isOpen();
                }

                @Override
                public void close() throws IOException {
                    try (WritableByteChannel ignored = out; ReadableByteChannel ignored2 = in) {
                    }
                }
            }, SftpFileSystems.rooted(FileSystems.getDefault().getPath(".")));
        }

        @Override
        public void destroy() throws Exception {
            System.out.println("destroy");
            handler.close();
        }
    }
}
