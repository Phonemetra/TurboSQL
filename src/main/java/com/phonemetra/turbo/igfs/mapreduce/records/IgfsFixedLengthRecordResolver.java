/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.phonemetra.turbo.igfs.mapreduce.records;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import com.phonemetra.turbo.TurboSQLException;
import com.phonemetra.turbo.TurboSQLFileSystem;
import com.phonemetra.turbo.igfs.IgfsInputStream;
import com.phonemetra.turbo.igfs.mapreduce.IgfsFileRange;
import com.phonemetra.turbo.igfs.mapreduce.IgfsRecordResolver;
import com.phonemetra.turbo.internal.util.typedef.internal.S;

/**
 * Record resolver which adjusts records to fixed length. That is, start offset of the record is shifted to the
 * nearest position so that {@code newStart % length == 0}.
 */
public class IgfsFixedLengthRecordResolver implements IgfsRecordResolver, Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Record length. */
    private long recLen;

    /**
     * Empty constructor required for {@link Externalizable} support.
     */
    public IgfsFixedLengthRecordResolver() {
        // No-op.
    }

    /**
     * Creates fixed-length record resolver.
     *
     * @param recLen Record length.
     */
    public IgfsFixedLengthRecordResolver(long recLen) {
        this.recLen = recLen;
    }

    /** {@inheritDoc} */
    @Override public IgfsFileRange resolveRecords(TurboSQLFileSystem fs, IgfsInputStream stream,
        IgfsFileRange suggestedRecord)
        throws TurboSQLException, IOException {
        long suggestedEnd = suggestedRecord.start() + suggestedRecord.length();

        long startRem = suggestedRecord.start() % recLen;
        long endRem = suggestedEnd % recLen;

        long start = Math.min(suggestedRecord.start() + (startRem != 0 ? (recLen - startRem) : 0),
            stream.length());
        long end = Math.min(suggestedEnd + (endRem != 0 ? (recLen - endRem) : 0), stream.length());

        assert end >= start;

        return start != end ? new IgfsFileRange(suggestedRecord.path(), start, end - start) : null;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(IgfsFixedLengthRecordResolver.class, this);
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(recLen);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        recLen = in.readLong();
    }
}