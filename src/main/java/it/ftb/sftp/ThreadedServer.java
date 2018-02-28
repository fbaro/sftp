package it.ftb.sftp;

import com.google.common.collect.ImmutableList;
import it.ftb.sftp.network.MalformedPacketException;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.channel.ChannelOutputStream;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.StaticPasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystems;
import java.util.concurrent.TimeUnit;

public class ThreadedServer {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadedServer.class);

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

    private static class MySftpCommand implements Command, Runnable {
        private ReadableByteChannel in;
        private WritableByteChannel out;
        private String user;
        private Thread cmdThread;
        private ExitCallback callback;

        @Override
        public void setInputStream(InputStream in) {
            this.in = Channels.newChannel(in);
        }

        @Override
        public void setOutputStream(OutputStream out) {
            if (out instanceof ChannelOutputStream) {
                ((ChannelOutputStream)out).setNoDelay(true);
            }
            this.out = Channels.newChannel(out);
        }

        @Override
        public void setErrorStream(OutputStream err) {
        }

        @Override
        public void setExitCallback(ExitCallback callback) {
            this.callback = callback;
        }

        @Override
        public void start(Environment env) {
            user = env.getEnv().get(Environment.ENV_USER);
            LOG.info("Starting SFTP for user {}", user);
            cmdThread = new Thread(this);
            cmdThread.start();
        }

        @Override
        public void destroy() throws InterruptedException {
            LOG.info("Closing SFTP for user {}", user);
            if (cmdThread != null) {
                cmdThread.interrupt();
                cmdThread.join();
            }
        }

        @Override
        public void run() {
            try (MDC.MDCCloseable ignored = MDC.putCloseable("user", user)) {
                SftpFileSystem<? extends SftpPath> sftpFileSystem = SftpFileSystems.rooted(FileSystems.getDefault().getPath("."));
                run(sftpFileSystem);
                callback.onExit(0);
            } catch (MalformedPacketException ex) {
                LOG.debug("Malformed packet reading from client stream", ex);
                callback.onExit(-1, ex.toString());
            } catch (IOException ex) {
                LOG.warn("I/O exception reading from client stream", ex);
                callback.onExit(-2, ex.toString());
            } catch (RuntimeException ex) {
                LOG.error("Error handling client stream", ex);
                callback.onExit(-3, ex.toString());
            }
        }

        private <P extends SftpPath<P>> void run(SftpFileSystem<P> fs) throws IOException {
            PacketEncoder packetEncoder = new PacketEncoder(out);
            DefaultPacketProcessor<P> processor = new DefaultPacketProcessor<>(fs, packetEncoder::write);
            try (ClientInputHandler handler = new ClientInputHandler(processor)) {
                ByteBuffer buf = ByteBuffer.allocate(0x10000);
                while (-1 != in.read(buf)) {
                    buf.flip();
                    handler.receive(buf);
                    buf.compact();
                }
            }
        }
    }
}
