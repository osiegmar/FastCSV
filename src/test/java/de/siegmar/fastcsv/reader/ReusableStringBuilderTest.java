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

package de.siegmar.fastcsv.reader;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test
public class ReusableStringBuilderTest {

    private ReusableStringBuilder sb;

    @BeforeMethod
    public void init() {
        sb = new ReusableStringBuilder(1);
    }

    public void empty() {
        assertFalse(sb.hasContent());
        final String s = sb.toStringAndReset();
        assertEquals(s, "");
    }

    public void one() {
        sb.append('a');
        assertTrue(sb.hasContent());
        String s = sb.toStringAndReset();
        assertFalse(sb.hasContent());
        assertEquals(s, "a");

        s = sb.toStringAndReset();
        assertEquals(s, "");
    }

    public void two() {
        sb.append('a');
        sb.append('b');
        assertTrue(sb.hasContent());
        String s = sb.toStringAndReset();
        assertFalse(sb.hasContent());
        assertEquals(s, "ab");

        s = sb.toStringAndReset();
        assertEquals(s, "");
    }

    public void larger() {
        final String str = "A larger string to test re-sizing";
        sb.append(str.toCharArray(), 0, str.length());
        assertEquals(sb.toStringAndReset(), str);
    }

}
