package de.siegmar.fastcsv;

import java.io.IOException;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;

public class FastCsvWriteBenchmark {

    @Benchmark
    public void write(final WriteState state) {
        state.writer.writeRecord(state.rowSupplier.get());
    }

    @State(Scope.Benchmark)
    public static class WriteState {

        private final RowSupplier rowSupplier = new RowSupplier(CsvConstants.RECORDS);
        private CsvWriter writer;

        @Setup
        public void setup(final Blackhole bh) {
            writer = CsvWriter.builder()
                .lineDelimiter(LineDelimiter.LF)
                .bufferSize(0)
                .build(new NullWriter(bh));
        }

        @TearDown
        public void teardown() throws IOException {
            writer.close();
        }

    }

}
