/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.picasso3;

import android.support.annotation.NonNull;
import java.io.IOException;
import java.io.InputStream;
import okio.Buffer;
import okio.BufferedSource;

/**
 * An {@link InputStream} that fills the buffer of an {@link BufferedSource} as reads are requested
 * and copies its bytes into the byte arrays of the caller. This allows you to read as much of the
 * underlying {@link BufferedSource} as you want through the eyes of this {@link InputStream} while
 * still preserving the ability to still consume the {@link BufferedSource} once you are done with
 * this instance.
 */
@org.checkerframework.framework.qual.AnnotatedFor("org.checkerframework.checker.nullness.NullnessChecker")
final class SourceBufferingInputStream extends InputStream {

    private final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull BufferedSource source;

    private final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Buffer buffer;

    private  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull long position;

    private  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull long markPosition = -1;

    private  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull long markLimit = -1;

    @org.checkerframework.dataflow.qual.Impure
    SourceBufferingInputStream(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull BufferedSource source) {
        this.source = source;
        this.buffer = source.buffer();
    }

    private final @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull Buffer temp = new Buffer();

    @org.checkerframework.dataflow.qual.Impure
    private  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int copyTo( @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull byte @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull [] sink,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int offset,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int byteCount) {
        // TODO replace this with https://github.com/square/okio/issues/362
        // `copyTo` treats offset as the read position, `read` treats offset as the write offset.
        buffer.copyTo(temp, position, byteCount);
        return temp.read(sink, offset, byteCount);
    }

    @org.checkerframework.dataflow.qual.Impure
    public  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int read(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull SourceBufferingInputStream this) throws IOException {
        if (!source.request(position + 1)) {
            return -1;
        }
        byte value = buffer.getByte(position++);
        if (position > markLimit) {
            markPosition = -1;
        }
        return value;
    }

    @org.checkerframework.dataflow.qual.Impure
    public  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int read(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull SourceBufferingInputStream this,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull byte @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull [] b,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int off,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int len) throws IOException {
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }
        int count = len;
        if (!source.request(position + count)) {
            count = available();
        }
        if (count == 0)
            return -1;
        int copied = /*buffer.*/
        copyTo(b, off, count);
        position += copied;
        if (position > markLimit) {
            markPosition = -1;
        }
        return copied;
    }

    @org.checkerframework.dataflow.qual.Impure
    public  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull long skip(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull SourceBufferingInputStream this,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull long n) throws IOException {
        source.require(position + n);
        position += n;
        if (position > markLimit) {
            markPosition = -1;
        }
        return n;
    }

    @org.checkerframework.dataflow.qual.Pure
    public  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull boolean markSupported(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull SourceBufferingInputStream this) {
        return true;
    }

    @org.checkerframework.dataflow.qual.Impure
    public void mark(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull SourceBufferingInputStream this,  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int readlimit) {
        markPosition = position;
        markLimit = position + readlimit;
    }

    @org.checkerframework.dataflow.qual.Impure
    public void reset(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull SourceBufferingInputStream this) throws IOException {
        if (markPosition == -1) {
            throw new IOException("No mark or mark expired");
        }
        position = markPosition;
        markPosition = -1;
        markLimit = -1;
    }

    @org.checkerframework.dataflow.qual.Impure
    public  @org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull int available(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull SourceBufferingInputStream this) {
        return (int) Math.min(buffer.size() - position, Integer.MAX_VALUE);
    }

    @org.checkerframework.dataflow.qual.SideEffectFree
    public void close(@org.checkerframework.checker.initialization.qual.Initialized @org.checkerframework.checker.nullness.qual.NonNull SourceBufferingInputStream this) {
    }
}
