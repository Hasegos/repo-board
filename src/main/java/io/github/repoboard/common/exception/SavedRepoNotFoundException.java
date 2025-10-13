package io.github.repoboard.common.exception;

/**
 * 사용자가 저장한 레포지토리를 찾지 못했을 때 발생하는 예외.
 */
public class SavedRepoNotFoundException extends RuntimeException {
    public SavedRepoNotFoundException() {
        super("저장한 레포지토리가 존재하지 않습니다.");
    }
}