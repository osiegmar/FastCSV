/*
 * Copyright 2020 Oliver Siegmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.siegmar.fastcsv;

import java.io.IOException;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;

@SuppressWarnings({"checkstyle:VisibilityModifier", "checkstyle:InnerTypeLast"})
public class FastCsvBenchmark {

    @State(Scope.Benchmark)
    public static class WriteState {

        private CsvWriter writer;

        @Setup
        public void setup(final Blackhole bh) {
            writer = CsvWriter.builder()
                .lineDelimiter(LineDelimiter.LF)
                .build(new NullWriter(bh));
        }

        @TearDown
        public void teardown() throws IOException {
            writer.close();
        }

    }

    @Benchmark
    public void write(final WriteState state) throws IOException {
        state.writer.writeLine(Constants.ROW);
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

    @Benchmark
    public CsvRow read(final ReadState state) {
        return state.it.next();
    }

}
