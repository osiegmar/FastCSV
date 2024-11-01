package specreader.spec;

import java.util.List;

public record TestSpecCheck(String id, String input, String description, List<List<String>> records) {

    public TestSpecCheck {
        records = List.copyOf(records);
    }

    @Override
    public List<List<String>> records() {
        return List.copyOf(records);
    }

}
