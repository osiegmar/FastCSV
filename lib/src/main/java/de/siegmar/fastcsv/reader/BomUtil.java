package de.siegmar.fastcsv.reader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

@SuppressWarnings("checkstyle:MagicNumber")
final class BomUtil {

    /// The maximum number of bytes a BOM header can have.
    static final int POTENTIAL_BOM_SIZE = 4;

    private BomUtil() {
    }

    /// Detects the character encoding of a byte array based on the presence of a Byte Order Mark (BOM) header.
    /// The method supports the following BOM headers:
    ///
    /// | Encoding  | BOM         |
    /// |-----------|-------------|
    /// | UTF-8     | EF BB BF    |
    /// | UTF-16 BE | FE FF       |
    /// | UTF-16 LE | FF FE       |
    /// | UTF-32 BE | 00 00 FE FF |
    /// | UTF-32 LE | FF FE 00 00 |
    ///
    /// See <a href="https://en.wikipedia.org/wiki/Byte_order_mark">Byte order mark</a>.
    ///
    /// @param buf the byte array to detect the character encoding from
    /// @return an Optional containing the detected BomHeader if a BOM header is found,
    ///     or an empty Optional if no BOM header is found
    @SuppressWarnings({
        "checkstyle:CyclomaticComplexity",
        "checkstyle:BooleanExpressionComplexity",
        "checkstyle:NestedIfDepth",
        "checkstyle:ReturnCount",
        "PMD.AvoidLiteralsInIfCondition"
    })
    static Optional<BomHeader> detectCharset(final byte[] buf) {
        final int n = buf.length;

        if (n < 2) {
            // Not enough bytes to be a BOM header
            return Optional.empty();
        }

        if (buf[0] == (byte) 0xEF) {
            if (n > 2 && buf[1] == (byte) 0xBB && buf[2] == (byte) 0xBF) {
                return Optional.of(BomHeader.UTF_8);
            }
        } else if (buf[0] == (byte) 0xFE) {
            if (buf[1] == (byte) 0xFF) {
                return Optional.of(BomHeader.UTF_16_BE);
            }
        } else if (buf[0] == (byte) 0xFF) {
            if (buf[1] == (byte) 0xFE) {
                if (n > 3 && buf[2] == (byte) 0x00 && buf[3] == (byte) 0x00) {
                    return Optional.of(BomHeader.UTF_32_LE);
                } else {
                    return Optional.of(BomHeader.UTF_16_LE);
                }
            }
        } else if (buf[0] == (byte) 0x00) {
            if (n > 3
                && buf[1] == (byte) 0x00
                && buf[2] == (byte) 0xFE
                && buf[3] == (byte) 0xFF) {
                return Optional.of(BomHeader.UTF_32_BE);
            }
        }

        return Optional.empty();
    }

    /// Detects the character encoding of a file based on the presence of a Byte Order Mark (BOM) header.
    ///
    /// @param file the file to detect the character encoding from
    /// @return an [Optional] containing the detected [BomHeader] if a BOM header is found,
    ///     or [Optional#EMPTY] if no BOM header is found
    /// @throws IOException if an I/O error occurs reading the file
    static Optional<BomHeader> detectCharset(final Path file)
        throws IOException {
        try (var in = Files.newInputStream(file, StandardOpenOption.READ)) {
            return detectCharset(in.readNBytes(POTENTIAL_BOM_SIZE));
        }
    }

}
