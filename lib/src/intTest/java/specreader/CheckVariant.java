package specreader;

import java.util.List;

public record CheckVariant(Variant variant, String description, List<List<String>> records) {

    public CheckVariant {
        records = List.copyOf(records);
    }

    @Override
    public List<List<String>> records() {
        return List.copyOf(records);
    }

}
