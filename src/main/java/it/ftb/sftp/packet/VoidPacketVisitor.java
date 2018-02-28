package it.ftb.sftp.packet;

import com.google.common.collect.ImmutableList;
import it.ftb.sftp.network.Bytes;

import java.util.Optional;

public interface VoidPacketVisitor {

    void visit();

    default void visitInit(int uVersion) {
        visit();
    }

    default void visitVersion(int uVersion, ImmutableList<ExtensionPair> extensions) {
        visit();
    }

    default void visitOpen(int uRequestId, String filename, int uDesideredAccess, int uFlags, Attrs attrs) {
        visit();
    }

    default void visitOpenDir(int uRequestId, String path) {
        visit();
    }

    default void visitHandle(int uRequestId, Bytes handle) {
        visit();
    }

    default void visitClose(int uRequestId, Bytes handle) {
        visit();
    }

    default void visitReadDir(int uRequestId, Bytes handle) {
        visit();
    }

    default void visitRead(int uRequestId, Bytes handle, long uOffset, int uLength) {
        visit();
    }

    default void visitData(int uRequestId, Bytes data, boolean endOfFile) {
        visit();
    }

    default void visitStatus(int uRequestId, ErrorCode errorCode, String errorMessage, String errorMessageLanguage) {
        visit();
    }

    default void visitName(int uRequestId, ImmutableList<String> names, ImmutableList<Attrs> attributes, Optional<Boolean> endOfList) {
        visit();
    }

    default void visitLstat(int uRequestId, String path, int uFlags) {
        visit();
    }

    default void visitStat(int uRequestId, String path, int uFlags) {
        visit();
    }

    default void visitFstat(int uRequestId, Bytes handle, int uFlags) {
        visit();
    }

    default void visitRealpath(int uRequestId, String originalPath, SshFxpRealpath.ControlByte controlByte, ImmutableList<String> composePath) {
        visit();
    }

    default void visitAttrs(int uRequestId, Attrs attrs) {
        visit();
    }

    default void visitWrite(int uRequestId, Bytes handle, long uOffset, Bytes data) {
        visit();
    }

    default void visitSetstat(int uRequestId, String path, Attrs attrs) {
        visit();
    }
}
