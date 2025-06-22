package de.siegmar.fastcsv;

import java.io.IOException;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;

public class FastCsvReadRelaxedBenchmark {

    @Benchmark
    public CsvRecord read(final ReadState state) {
        return state.it.next();
    }

    @State(Scope.Benchmark)
    public static class ReadState {

        private CloseableIterator<CsvRecord> it;

        @Setup
        public void setup() {
            it = CsvReader.builder()
                .allowMissingFields(true)
                .fieldSeparator("||")
                .ofCsvRecord(new InfiniteDataReader(CsvConstants.DATA_WITH_MULTI_CHAR_SEPARATOR))
                .iterator();
        }

        @TearDown
        public void teardown() throws IOException {
            it.close();
        }

    }

}
