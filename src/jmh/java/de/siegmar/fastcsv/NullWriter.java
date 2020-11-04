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

import java.io.Writer;

import org.openjdk.jmh.infra.Blackhole;

/**
 * Writer implementation that sends all data to a black hole.
 */
class NullWriter extends Writer {

    private final Blackhole blackhole;

    NullWriter(final Blackhole blackhole) {
        this.blackhole = blackhole;
    }

    @Override
    public void write(final char[] cbuf, final int off, final int len) {
        blackhole.consume(cbuf);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

}
