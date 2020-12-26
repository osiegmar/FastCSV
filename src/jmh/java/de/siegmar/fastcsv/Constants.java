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

public final class Constants {

    static final String[] ROW = {
        "Simple field",
        "Example with separator ,",
        "Example with delimiter \"",
        "Example with\nnewline",
        "Example with , and \" and \nnewline",
    };

    static final String DATA = "Simple field,"
        + "\"Example with separator ,\","
        + "\"Example with delimiter \"\"\","
        + "\"Example with\nnewline\","
        + "\"Example with , and \"\" and \nnewline\""
        + "\n";

    private Constants() {
    }

}
