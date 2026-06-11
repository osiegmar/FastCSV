package de.siegmar.fastcsv.reader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.nio.charset.Charset;

/// A [Reader] over a [RandomAccessFile] that supports repositioning via [#seek(long)].
///
/// A plain [InputStreamReader] (more precisely its internal `StreamDecoder`) retains undecoded
/// leftover bytes when a read ends mid-multibyte-sequence. After repositioning the file these stale
/// bytes would otherwise be prepended to the freshly read data, corrupting it. [#seek(long)]
/// therefore repositions the file *and* discards the current decoder atomically, so the two can
/// never get out of sync.
final class SeekableInputStreamReader extends Reader {

    private final RandomAccessFile raf;
    private final RandomAccessFileInputStream inputStream;
    private final Charset charset;
    private InputStreamReader delegate;

    SeekableInputStreamReader(final RandomAccessFile raf, final Charset charset) {
        this.raf = raf;
        this.charset = charset;
        inputStream = new RandomAccessFileInputStream(raf);
        delegate = new InputStreamReader(inputStream, charset);
    }

    /// Repositions the underlying file to the given byte offset and resumes decoding from there.
    ///
    /// The previous decoder is abandoned (not closed – that would close the shared file), dropping
    /// any retained, now-stale leftover bytes from a multibyte sequence straddling the previous read
    /// boundary.
    ///
    /// @param position the byte offset to seek to.
    /// @throws IOException if seeking fails.
    void seek(final long position) throws IOException {
        raf.seek(position);
        delegate = new InputStreamReader(inputStream, charset);
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        return delegate.read(cbuf, off, len);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

}
