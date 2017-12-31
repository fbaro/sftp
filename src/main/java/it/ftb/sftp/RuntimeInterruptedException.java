package it.ftb.sftp;

public class RuntimeInterruptedException extends RuntimeException {
    public RuntimeInterruptedException(InterruptedException cause) {
        super(cause);
    }

    @Override
    public InterruptedException getCause() {
        return (InterruptedException) super.getCause();
    }
}
