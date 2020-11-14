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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

final class CharacterConv {

    private static final String[] STANDARD = {" ", "\r", "\n"};
    private static final String[] CONV = {"␣", "←", "↵"};

    private CharacterConv() {
    }

    public static String print(final List<CsvRow> data) {
        final StringBuilder sb = new StringBuilder();
        for (Iterator<CsvRow> iter = data.iterator(); iter.hasNext();) {
            final CsvRow datum = iter.next();
            final Iterator<String> iterator = Arrays.stream(datum.getFields()).iterator();
            while (iterator.hasNext()) {
                sb.append(print(iterator.next()));
                if (iterator.hasNext()) {
                    sb.append(',');
                }
            }
            if (iter.hasNext()) {
                sb.append('␤');
            }
        }
        return sb.toString();
    }

    public static String print(final String str) {
        return StringUtils.replaceEach(str, STANDARD, CONV);
    }

    public static String parse(final String str) {
        return StringUtils.replaceEach(str, CONV, STANDARD);
    }

}
