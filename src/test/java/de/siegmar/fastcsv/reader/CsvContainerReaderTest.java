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

public class CsvContainerReaderTest {

    private final CsvReaderBuilder crb = CsvReader.builder();
/*
    @Test
    public void emptyContainer() throws IOException {
        crb.containsHeader(true);
        final CsvContainer csv = read("");
        assertNotNull(csv);
        assertNull(csv.getHeader());
        assertEquals(0, csv.getRowCount());
        assertEquals(Collections.<CsvRow>emptyList(), csv.getRows());
    }

    private CsvContainer read(final String data) throws IOException {
        return crb.read(new StringReader(data));
    }
*/
}
