package it.ftb.sftp;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

public class SftpClientTest {

    @Test
    @Ignore
    public void main() throws IOException {
        try (SSHClient ssh = new SSHClient()) {
            try {
                ssh.addHostKeyVerifier(new PromiscuousVerifier());
                ssh.connect("localhost", 12344);
                ssh.authPassword("test", "test");
                SFTPClient sftp = ssh.newSFTPClient();
                sftp.get("/settings.gradle", "test.tmp");
            } finally {
                ssh.close();
            }
        }
    }
}
