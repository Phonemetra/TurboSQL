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
import com.phonemetra.turbo.configuration.TurboSQLConfiguration;
import com.phonemetra.turbo.internal.util.typedef.internal.S;
import com.phonemetra.turbo.internal.visor.VisorDataTransferObject;

/**
 * Data transfer object for node metrics configuration properties.
 */
public class VisorMetricsConfiguration extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Metrics expired time. */
    private long expTime;

    /** Number of node metrics stored in memory. */
    private int histSize;

    /** Frequency of metrics log printout. */
    private long logFreq;

    /**
     * Default constructor.
     */
    public VisorMetricsConfiguration() {
        // No-op.
    }

    /**
     * Create transfer object for node metrics configuration properties.
     *
     * @param c Grid configuration.
     */
    public VisorMetricsConfiguration(TurboSQLConfiguration c) {
        expTime = c.getMetricsExpireTime();
        histSize = c.getMetricsHistorySize();
        logFreq = c.getMetricsLogFrequency();
    }

    /**
     * @return Metrics expired time.
     */
    public long getExpireTime() {
        return expTime;
    }

    /**
     * @return Number of node metrics stored in memory.
     */
    public int getHistorySize() {
        return histSize;
    }

    /**
     * @return Frequency of metrics log printout.
     */
    public long getLoggerFrequency() {
        return logFreq;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        out.writeLong(expTime);
        out.writeInt(histSize);
        out.writeLong(logFreq);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(byte protoVer, ObjectInput in) throws IOException, ClassNotFoundException {
        expTime = in.readLong();
        histSize = in.readInt();
        logFreq = in.readLong();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorMetricsConfiguration.class, this);
    }
}
