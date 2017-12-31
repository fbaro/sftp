package it.ftb.sftp;

import java.io.IOException;

public class RuntimeIOException extends RuntimeException {
    public RuntimeIOException(IOException cause) {
        super(cause);
    }

    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
