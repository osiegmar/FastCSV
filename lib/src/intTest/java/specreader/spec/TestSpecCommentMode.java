package specreader.spec;

public enum TestSpecCommentMode {

    /**
     * No special handling of comments. Treat hash as a regular character.
     */
    NONE,

    /**
     * Treat hash as a comment character but read the comment allowing to access it.
     */
    READ,

    /**
     * Treat hash as a comment character and skip the comment.
     */
    SKIP

}
