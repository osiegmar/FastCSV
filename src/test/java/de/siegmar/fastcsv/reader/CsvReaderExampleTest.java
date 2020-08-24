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

package de.siegmar.fastcsv.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

public class CsvReaderExampleTest {

    @Test
    public void simple() throws IOException {
        final CsvContainer csv = CsvReader.builder().read(new StringReader("foo,bar"));
        assertEquals("foo", csv.getRow(0).getField(0));
        assertEquals("bar", csv.getRow(0).getField(1));
    }

}
