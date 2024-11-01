package specreader.spec;

import java.util.List;

public record TestSpec(String name, String description, TestSpecSettings settings, List<TestSpecCheck> checks) {

    public TestSpec {
        if (settings == null) {
            settings = new TestSpecSettings(false, false, null);
        }
        checks = List.copyOf(checks);
    }

    @Override
    public List<TestSpecCheck> checks() {
        return List.copyOf(checks);
    }

}
