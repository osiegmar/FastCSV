package specreader;

import java.util.LinkedHashSet;
import java.util.Set;

public record Variant(String orig, String data, Set<Pair> replacements) {

    public Variant {
        replacements = Set.copyOf(replacements);
    }

    public Variant extend(final String placeholder, final String replacement) {
        final Set<Pair> newReplacements = new LinkedHashSet<>(replacements);
        newReplacements.add(new Pair(placeholder, replacement));
        return new Variant(orig, data.replace(placeholder, replacement), newReplacements);
    }

    @Override
    public Set<Pair> replacements() {
        return Set.copyOf(replacements);
    }

}
