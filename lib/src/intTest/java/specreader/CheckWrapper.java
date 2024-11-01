package specreader;

import specreader.spec.TestSpecCheck;

public record CheckWrapper(TestSpecFile specFile, TestSpecCheck check) {
}
