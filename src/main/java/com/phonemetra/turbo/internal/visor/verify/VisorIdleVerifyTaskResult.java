/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.phonemetra.turbo.internal.visor.verify;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import java.util.Map;
import com.phonemetra.turbo.internal.processors.cache.verify.PartitionHashRecord;
import com.phonemetra.turbo.internal.processors.cache.verify.PartitionKey;
import com.phonemetra.turbo.internal.util.typedef.internal.S;
import com.phonemetra.turbo.internal.util.typedef.internal.U;
import com.phonemetra.turbo.internal.visor.VisorDataTransferObject;

/**
 * Result for task {@link VisorIdleVerifyTask}
 */
public class VisorIdleVerifyTaskResult extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Results. */
    private Map<PartitionKey, List<PartitionHashRecord>> conflicts;

    /**
     * Default constructor.
     */
    public VisorIdleVerifyTaskResult() {
        // No-op.
    }

    /**
     * @param conflicts Result.
     */
    public VisorIdleVerifyTaskResult(Map<PartitionKey, List<PartitionHashRecord>> conflicts) {
        this.conflicts = conflicts;
    }

    /**
     * @return Results.
     */
    public Map<PartitionKey, List<PartitionHashRecord>> getConflicts() {
        return conflicts;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        U.writeMap(out, conflicts);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(byte protoVer, ObjectInput in) throws IOException, ClassNotFoundException {
        conflicts = U.readMap(in);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorIdleVerifyTaskResult.class, this);
    }
}
