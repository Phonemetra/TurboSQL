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

package com.phonemetra.turbo.internal.visor.node;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import com.phonemetra.turbo.internal.processors.affinity.AffinityTopologyVersion;
import com.phonemetra.turbo.internal.util.typedef.internal.S;
import com.phonemetra.turbo.internal.visor.VisorDataTransferObject;

/**
 * Data transfer object for {@link AffinityTopologyVersion}
 */
public class VisorAffinityTopologyVersion extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private long topVer;

    /** */
    private int minorTopVer;

    /**
     * Default constructor.
     */
    public VisorAffinityTopologyVersion() {
        // No-op.
    }

    /**
     * Create data transfer object for affinity topology version.
     *
     * @param affTopVer Affinity topology version.
     */
    public VisorAffinityTopologyVersion(AffinityTopologyVersion affTopVer) {
        topVer = affTopVer.topologyVersion();
        minorTopVer = affTopVer.minorTopologyVersion();
    }

    /**
     * @return Topology version.
     */
    public long getTopologyVersion() {
        return topVer;
    }

    /**
     * @return Minor topology version.
     */
    public int getMinorTopologyVersion() {
        return minorTopVer;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        out.writeLong(topVer);
        out.writeInt(minorTopVer);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(byte protoVer, ObjectInput in) throws IOException, ClassNotFoundException {
        topVer = in.readLong();
        minorTopVer = in.readInt();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorAffinityTopologyVersion.class, this);
    }
}
