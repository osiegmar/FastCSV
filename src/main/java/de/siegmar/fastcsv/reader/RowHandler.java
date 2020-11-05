/*
 * Copyright 2018 Oliver Siegmar
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

final class RowHandler {

    private int len;
    private String[] row;
    private int idx;
    private int lines = 1;

    RowHandler(final int len) {
        this.len = len;
        row = new String[len];
    }

    void add(final String value) {
        if (idx == len) {
            extendCapacity();
        }
        row[idx++] = value;
    }

    private void extendCapacity() {
        len *= 2;
        final String[] newRow = new String[len];
        System.arraycopy(row, 0, newRow, 0, idx);
        row = newRow;
    }

    String[] end() {
        final String[] ret = new String[idx];
        System.arraycopy(row, 0, ret, 0, idx);
        idx = 0;
        return ret;
    }

    long getLines() {
        return lines;
    }

    public void incLines() {
        this.lines++;
    }

}
