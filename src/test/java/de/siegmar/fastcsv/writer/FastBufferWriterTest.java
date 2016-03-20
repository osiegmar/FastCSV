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

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test
public class FastBufferWriterTest {

    private FastBufferedWriter fbw;
    private StringWriter sw;

    @BeforeMethod
    public void init() {
        sw = new StringWriter();
        fbw = new FastBufferedWriter(sw);
    }

    public void appendSingle() throws IOException {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 8192; i++) {
            sb.append("ab");
            fbw.append('a');
            fbw.append('b');
        }
        fbw.close();

        assertEquals(sw.toString(), sb.toString());
    }

    public void appendArray() throws IOException {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 8192; i++) {
            sb.append("ab");
            fbw.append("ab");
        }
        fbw.close();

        assertEquals(sw.toString(), sb.toString());
    }

    public void appendLarge() throws IOException {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 8192; i++) {
            sb.append("ab");
        }
        fbw.append(sb.toString());

        // also test flush
        fbw.flush();

        assertEquals(sw.toString(), sb.toString());
    }

}
