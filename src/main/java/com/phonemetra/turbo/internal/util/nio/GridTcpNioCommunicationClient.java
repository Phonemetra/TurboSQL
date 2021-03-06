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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;
import com.phonemetra.turbo.TurboSQLCheckedException;
import com.phonemetra.turbo.TurboSQLException;
import com.phonemetra.turbo.TurboSQLLogger;
import com.phonemetra.turbo.internal.util.lang.TurboSQLInClosure2X;
import com.phonemetra.turbo.internal.util.typedef.internal.S;
import com.phonemetra.turbo.internal.util.typedef.internal.U;
import com.phonemetra.turbo.lang.TurboSQLInClosure;
import com.phonemetra.turbo.plugin.extensions.communication.Message;
import org.jetbrains.annotations.Nullable;

/**
 * Grid client for NIO server.
 */
public class GridTcpNioCommunicationClient extends GridAbstractCommunicationClient {
    /** Session. */
    private final GridNioSession ses;

    /** Logger. */
    private final TurboSQLLogger log;

    /**
     * @param connIdx Connection index.
     * @param ses Session.
     * @param log Logger.
     */
    public GridTcpNioCommunicationClient(
        int connIdx,
        GridNioSession ses,
        TurboSQLLogger log
    ) {
        super(connIdx, null);

        assert ses != null;
        assert log != null;

        this.ses = ses;
        this.log = log;
    }

    /**
     * @return Gets underlying session.
     */
    public GridNioSession session() {
        return ses;
    }

    /** {@inheritDoc} */
    @Override public void doHandshake(TurboSQLInClosure2X<InputStream, OutputStream> handshakeC) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public boolean close() {
        boolean res = super.close();

        if (res)
            ses.close();

        return res;
    }

    /** {@inheritDoc} */
    @Override public void forceClose() {
        super.forceClose();

        ses.close();
    }

    /** {@inheritDoc} */
    @Override public void sendMessage(byte[] data, int len) throws TurboSQLCheckedException {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public void sendMessage(ByteBuffer data) throws TurboSQLCheckedException {
        if (closed())
            throw new TurboSQLCheckedException("Client was closed: " + this);

        GridNioFuture<?> fut = ses.send(data);

        if (fut.isDone())
            fut.get();
    }

    /** {@inheritDoc} */
    @Override public boolean sendMessage(@Nullable UUID nodeId, Message msg, TurboSQLInClosure<TurboSQLException> c)
        throws TurboSQLCheckedException {
        try {
            // Node ID is never provided in asynchronous send mode.
            assert nodeId == null;

            ses.sendNoFuture(msg, c);
        }
        catch (TurboSQLCheckedException e) {
            if (log.isDebugEnabled())
                log.debug("Failed to send message [client=" + this + ", err=" + e + ']');

            if (e.getCause() instanceof IOException) {
                ses.close();

                return true;
            }
            else
                throw new TurboSQLCheckedException("Failed to send message [client=" + this + ']', e);
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override public boolean async() {
        return true;
    }

    /** {@inheritDoc} */
    @Override public long getIdleTime() {
        long now = U.currentTimeMillis();

        // Session can be used for receiving and sending.
        return Math.min(Math.min(now - ses.lastReceiveTime(), now - ses.lastSendScheduleTime()),
            now - ses.lastSendTime());
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridTcpNioCommunicationClient.class, this, super.toString());
    }
}
