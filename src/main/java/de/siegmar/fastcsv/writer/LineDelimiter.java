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

/**
 * Enumeration for different line delimiters (LF, CR, CRLF, platform default).
 */
public enum LineDelimiter {

    /**
     * Line Feed - (UNIX).
     */
    LF,

    /**
     * Carriage Return - (Mac classic).
     */
    CR,

    /**
     * Carriage Return and Line Feed (Windows).
     */
    CRLF,

    /**
     * Use current platform default ({@link System#lineSeparator()}.
     */
    PLATFORM;

    @SuppressWarnings("checkstyle:returncount")
    @Override
    public String toString() {
        switch (this) {
            case LF:
                return "\n";
            case CR:
                return "\r";
            case CRLF:
                return "\r\n";
            case PLATFORM:
                return System.lineSeparator();
            default:
                throw new IllegalStateException();
        }
    }

}
