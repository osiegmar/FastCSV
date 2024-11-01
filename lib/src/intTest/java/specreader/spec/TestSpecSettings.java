package specreader.spec;

public record TestSpecSettings(boolean skipEmptyLines, boolean exceptionAllowed, TestSpecCommentMode commentMode) {
    public TestSpecSettings {
        if (commentMode == null) {
            commentMode = TestSpecCommentMode.NONE;
        }
    }
}
