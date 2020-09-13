/*
 * Copyright 2015 Oliver Siegmar
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

package de.siegmar.fastcsv.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class LineDelimiterTest {

    @Test
    public void test() {
        assertEquals("\n", LineDelimiter.LF.toString());
        assertEquals("\r", LineDelimiter.CR.toString());
        assertEquals("\r\n", LineDelimiter.CRLF.toString());
        assertEquals(System.lineSeparator(), LineDelimiter.PLATFORM.toString());
    }

    @Test
    public void testOf() {
        assertEquals(LineDelimiter.CRLF, LineDelimiter.of("\r\n"));
        assertEquals(LineDelimiter.LF, LineDelimiter.of("\n"));
        assertEquals(LineDelimiter.CR, LineDelimiter.of("\r"));

        final IllegalArgumentException e =
            assertThrows(IllegalArgumentException.class, () -> LineDelimiter.of(";"));
        assertEquals("Unknown line delimiter: ;", e.getMessage());
    }

}
