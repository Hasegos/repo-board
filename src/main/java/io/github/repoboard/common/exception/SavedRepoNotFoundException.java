package io.github.repoboard.common.exception;

public class SavedRepoNotFoundException extends RuntimeException {
    public SavedRepoNotFoundException() {
        super("저장한 레포지토리가 존재하지 않습니다.");
    }
}