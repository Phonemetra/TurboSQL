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

package com.phonemetra.turbo.internal.util.nio;

import com.phonemetra.turbo.TurboSQLException;
import com.phonemetra.turbo.internal.util.future.GridFutureAdapter;
import com.phonemetra.turbo.internal.util.typedef.internal.S;
import com.phonemetra.turbo.lang.TurboSQLInClosure;

/**
 * Default future implementation.
 */
public class GridNioFutureImpl<R> extends GridFutureAdapter<R> implements GridNioFuture<R> {
    /** */
    private boolean msgThread;

    /** */
    protected final TurboSQLInClosure<TurboSQLException> ackC;

    /**
     * @param ackC Ack closure.
     */
    public GridNioFutureImpl(TurboSQLInClosure<TurboSQLException> ackC) {
        this.ackC = ackC;
    }

    /** {@inheritDoc} */
    @Override public void messageThread(boolean msgThread) {
        this.msgThread = msgThread;
    }

    /** {@inheritDoc} */
    @Override public boolean messageThread() {
        return msgThread;
    }

    /** {@inheritDoc} */
    @Override public boolean skipRecovery() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public void onAckReceived() {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public TurboSQLInClosure<TurboSQLException> ackClosure() {
        return ackC;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridNioFutureImpl.class, this);
    }
}