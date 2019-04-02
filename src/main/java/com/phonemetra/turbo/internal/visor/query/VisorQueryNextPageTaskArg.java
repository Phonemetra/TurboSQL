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

package com.phonemetra.turbo.internal.visor.query;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import com.phonemetra.turbo.internal.util.typedef.internal.S;
import com.phonemetra.turbo.internal.util.typedef.internal.U;
import com.phonemetra.turbo.internal.visor.VisorDataTransferObject;

/**
 * Arguments for {@link VisorQueryNextPageTask}.
 */
public class VisorQueryNextPageTaskArg extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** ID of query execution. */
    private String qryId;

    /** Number of rows to load. */
    private int pageSize;

    /**
     * Default constructor.
     */
    public VisorQueryNextPageTaskArg() {
        // No-op.
    }

    /**
     * @param qryId ID of query execution.
     * @param pageSize Number of rows to load.
     */
    public VisorQueryNextPageTaskArg(String qryId, int pageSize) {
        this.qryId = qryId;
        this.pageSize = pageSize;
    }

    /**
     * @return ID of query execution.
     */
    public String getQueryId() {
        return qryId;
    }

    /**
     * @return Number of rows to load.
     */
    public int getPageSize() {
        return pageSize;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        U.writeString(out, qryId);
        out.writeInt(pageSize);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(byte protoVer, ObjectInput in) throws IOException, ClassNotFoundException {
        qryId = U.readString(in);
        pageSize = in.readInt();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorQueryNextPageTaskArg.class, this);
    }
}