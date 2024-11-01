package specreader;

import java.nio.file.Path;

import specreader.spec.TestSpec;

public record TestSpecFile(Path file, TestSpec spec) {
}
