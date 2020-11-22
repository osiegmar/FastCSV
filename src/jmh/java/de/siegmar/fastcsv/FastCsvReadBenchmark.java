package de.siegmar.fastcsv;

import java.io.IOException;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;

public class FastCsvReadBenchmark {

    @Benchmark
    public CsvRow read(final ReadState state) {
        return state.it.next();
    }

    @State(Scope.Benchmark)
    public static class ReadState {

        private CloseableIterator<CsvRow> it;

        @Setup
        public void setup() {
            it = CsvReader.builder().build(new InfiniteDataReader(Constants.DATA)).iterator();
        }

        @TearDown
        public void teardown() throws IOException {
            it.close();
        }

    }

}
