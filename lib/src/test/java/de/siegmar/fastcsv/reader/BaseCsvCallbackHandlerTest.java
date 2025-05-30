package de.siegmar.fastcsv.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class BaseCsvCallbackHandlerTest {

    @Test
    void onlyAbstract() {
        final var handler = new AbstractBaseCsvCallbackHandler<String>() {
            @Override
            protected String buildRecord() {
                return "ignored";
            }
        };

        final Stream<String> stream = CsvReader.builder()
            .commentStrategy(CommentStrategy.READ)
            .build(handler, "#foo\n\nbar").stream();

        assertThat(stream).satisfiesExactly(
            l -> assertThat(l).isEqualTo("ignored"),
            l -> assertThat(l).isEqualTo("ignored")
        );
    }

    @Test
    void simple() {
        final SimpleFieldCollector handler = new SimpleFieldCollector();

        final Iterator<List<String>> it = CsvReader.builder()
            .commentStrategy(CommentStrategy.READ)
            .skipEmptyLines(false)
            .build(handler, "#foo\n\nbar")
            .iterator();

        assertThat(it.next()).containsExactly("foo");
        assertThat(handler.getStartingLineNumber()).isOne();
        assertThat(handler.getRecordType()).isEqualTo(RecordType.COMMENT);
        assertThat(handler.getFieldCount()).isOne();

        assertThat(it.next()).containsExactly("");
        assertThat(handler.getStartingLineNumber()).isEqualTo(2);
        assertThat(handler.getRecordType()).isEqualTo(RecordType.EMPTY);
        assertThat(handler.getFieldCount()).isOne();

        assertThat(it.next()).containsExactly("bar");
        assertThat(handler.getStartingLineNumber()).isEqualTo(3);
        assertThat(handler.getRecordType()).isEqualTo(RecordType.DATA);
        assertThat(handler.getFieldCount()).isOne();
    }

    private static class SimpleFieldCollector extends AbstractBaseCsvCallbackHandler<List<String>> {

        private final List<String> fields = new ArrayList<>();

        @Override
        protected void handleBegin(final long startingLineNumber) {
            fields.clear();
        }

        @Override
        protected void handleField(final int fieldIdx, final char[] buf, final int offset, final int len,
                                   final boolean quoted) {
            fields.add(new String(buf, offset, len));
        }

        @Override
        protected void handleComment(final char[] buf, final int offset, final int len) {
            fields.add(new String(buf, offset, len));
        }

        @Override
        protected void handleEmpty() {
            fields.add("");
        }

        @Override
        public List<String> buildRecord() {
            return List.copyOf(fields);
        }

    }

}
