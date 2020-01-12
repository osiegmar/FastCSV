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

package de.siegmar.fastcsv.writer;

public enum TextDelimitStrategy {

    /**
     * Delimits only text fields that requires it. Simple strings (not containing delimiters,
     * field separators, new line or carriage return characters), empty strings and null fields
     * will not be delimited.
     */
    REQUIRED,

    /**
     * In addition to fields that require delimiting also delimit empty text fields to
     * differentiate between empty and null fields.
     * This is required for PostgreSQL CSV imports for example.
     */
    EMPTY,

    /**
     * Delimits any text field regardless of its content (even empty and null fields).
     */
    ALWAYS

}
