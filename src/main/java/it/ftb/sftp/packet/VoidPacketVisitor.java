package it.ftb.sftp.packet;

import com.google.common.collect.ImmutableList;
import it.ftb.sftp.network.Bytes;

import java.util.Optional;

public interface VoidPacketVisitor<P> {

    void visit(P parameter);

    default void visitInit(int uVersion, P parameter) {
        visit(parameter);
    }

    default void visitVersion(int uVersion, ImmutableList<ExtensionPair> extensions, P parameter) {
        visit(parameter);
    }

    default void visitOpen(int uRequestId, String filename, int uDesideredAccess, int uFlags, Attrs attrs, P parameter) {
        visit(parameter);
    }

    default void visitOpenDir(int uRequestId, String path, P parameter) {
        visit(parameter);
    }

    default void visitHandle(int uRequestId, Bytes handle, P parameter) {
        visit(parameter);
    }

    default void visitClose(int uRequestId, Bytes handle, P parameter) {
        visit(parameter);
    }

    default void visitReadDir(int uRequestId, Bytes handle, P parameter) {
        visit(parameter);
    }

    default void visitRead(int uRequestId, Bytes handle, long uOffset, int uLength, P parameter) {
        visit(parameter);
    }

    default void visitData(int uRequestId, Bytes data, boolean endOfFile, P parameter) {
        visit(parameter);
    }

    default void visitStatus(int uRequestId, ErrorCode errorCode, String errorMessage, String errorMessageLanguage, P parameter) {
        visit(parameter);
    }

    default void visitName(int uRequestId, ImmutableList<String> names, ImmutableList<Attrs> attributes, Optional<Boolean> endOfList, P parameter) {
        visit(parameter);
    }

    default void visitLstat(int uRequestId, String path, int uFlags, P parameter) {
        visit(parameter);
    }

    default void visitStat(int uRequestId, String path, int uFlags, P parameter) {
        visit(parameter);
    }

    default void visitFstat(int uRequestId, Bytes handle, int uFlags, P parameter) {
        visit(parameter);
    }

    default void visitRealpath(int uRequestId, String originalPath, SshFxpRealpath.ControlByte controlByte, ImmutableList<String> composePath, P parameter) {
        visit(parameter);
    }

    default void visitAttrs(int uRequestId, Attrs attrs, P parameter) {
        visit(parameter);
    }

    default void visitWrite(int uRequestId, Bytes handle, long uOffset, Bytes data, P parameter) {
        visit(parameter);
    }

    default void visitSetstat(int uRequestId, String path, Attrs attrs, P parameter) {
        visit(parameter);
    }
}
