package io.github.repoboard.common.exception;

import lombok.Getter;

/**
 * GitHub API 호출 응답의 Content-Type이 예상과 다를 때 발생하는 예외.
 * <p>
 * RepoBoard에서는 주로 application/json 또는 application/vnd.github.v3+json형태의 응답을 기대한다. <br>
 * 이외의 Content-Type이 응답으로 온 경우 해당 예외가 발생하여 호출부에서 처리되도록 한다.
 * </p>
 */
@Getter
public class UnexpectedContentTypeException extends RuntimeException{
    private final String contentType;

    public UnexpectedContentTypeException(String contentType){
        super("예상하지 못한 Content-Type: " + contentType);
        this.contentType = contentType;
    }
}