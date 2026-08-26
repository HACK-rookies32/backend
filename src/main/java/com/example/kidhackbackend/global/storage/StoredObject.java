package com.example.kidhackbackend.global.storage;

import java.util.Optional;

/**
 * 저장된 첨부파일 하나를 가리키는 좌표 (분류 + 파일명).
 *
 * <p>DB 에는 {@code /api/files/{category}/{filename}} 이라는 공개 경로 문자열 하나만
 * 남는다. 삭제하거나 존재를 확인하려면 그 문자열을 다시 (분류, 파일명) 으로 되돌려야
 * 하는데, 그 왕복을 여기 한 곳에 모은다.
 *
 * <p>경로를 만드는 코드가 S3ObjectStorage 와 LocalObjectStorage 두 곳에 같은 모양으로
 * 있었다. 만드는 쪽과 되읽는 쪽이 떨어져 있으면 한쪽만 고쳤을 때 조용히 어긋나므로
 * {@link #publicPath()} 와 {@link #parse(String)} 를 붙여 둔다.
 */
public record StoredObject(UploadCategory category, String filename) {

    private static final String PREFIX = "/api/files/";

    /** DB 와 API 응답에 실리는 공개 경로. */
    public String publicPath() {
        return PREFIX + category.segment() + "/" + filename;
    }

    /**
     * 공개 경로를 좌표로 되돌린다.
     *
     * <p>해석할 수 없으면 비어 있는 Optional 이다. 실패가 예외가 아닌 이유는 첨부
     * 필드에 우리가 만들지 않은 값이 들어 있을 수 있기 때문이다. 예전 데이터나
     * 외부 URL 을 만나도 호출하는 쪽이 그냥 건너뛰면 된다.
     */
    public static Optional<StoredObject> parse(String publicPath) {
        if (publicPath == null || !publicPath.startsWith(PREFIX)) {
            return Optional.empty();
        }

        String rest = publicPath.substring(PREFIX.length());
        int slash = rest.indexOf('/');
        if (slash <= 0) {
            return Optional.empty();
        }

        String filename = rest.substring(slash + 1);
        if (isUnsafe(filename)) {
            return Optional.empty();
        }

        return UploadCategory.from(rest.substring(0, slash))
                .map(category -> new StoredObject(category, filename));
    }

    /**
     * 파일명이 경로를 빠져나가는지 본다.
     *
     * <p>업로드 시 파일명에 원본 이름이 그대로 들어가므로([VULN-10]) 구분자나 상위
     * 참조가 섞여 들어올 수 있다. 여기서 막지 않으면 로컬 모드에서 저장 디렉터리를
     * 벗어난 파일을 읽거나 지울 수 있고, S3 모드에서는 prefix 밖의 키를 건드릴 수 있다.
     *
     * <p>이건 의도적으로 심은 취약점이 아니라 새로 만드는 경로이므로 막는다.
     */
    public static boolean isUnsafe(String filename) {
        return filename == null
                || filename.isBlank()
                || filename.contains("..")
                || filename.contains("/")
                || filename.contains("\\");
    }
}
