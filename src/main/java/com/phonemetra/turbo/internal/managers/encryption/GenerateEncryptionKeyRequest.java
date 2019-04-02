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

package com.phonemetra.turbo.internal.managers.encryption;

import java.nio.ByteBuffer;
import com.phonemetra.turbo.internal.util.typedef.internal.S;
import com.phonemetra.turbo.lang.TurboSQLUuid;
import com.phonemetra.turbo.plugin.extensions.communication.Message;
import com.phonemetra.turbo.plugin.extensions.communication.MessageReader;
import com.phonemetra.turbo.plugin.extensions.communication.MessageWriter;

/**
 * Generate encryption key request.
 */
public class GenerateEncryptionKeyRequest implements Message {
    /** */
    private static final long serialVersionUID = 0L;

    /** Request ID. */
    private TurboSQLUuid id = TurboSQLUuid.randomUuid();

    /** */
    private int keyCnt;

    /** */
    public GenerateEncryptionKeyRequest() {
    }

    /**
     * @param keyCnt Count of encryption key to generate.
     */
    public GenerateEncryptionKeyRequest(int keyCnt) {
        this.keyCnt = keyCnt;
    }

    /**
     * @return Request id.
     */
    public TurboSQLUuid id() {
        return id;
    }

    /**
     * @return Count of encryption key to generate.
     */
    public int keyCount() {
        return keyCnt;
    }

    /** {@inheritDoc} */
    @Override public boolean writeTo(ByteBuffer buf, MessageWriter writer) {
        writer.setBuffer(buf);

        if (!writer.isHeaderWritten()) {
            if (!writer.writeHeader(directType(), fieldsCount()))
                return false;

            writer.onHeaderWritten();
        }

        switch (writer.state()) {
            case 0:
                if (!writer.writeTurboSQLUuid("id", id))
                    return false;

                writer.incrementState();

            case 1:
                if (!writer.writeInt("keyCnt", keyCnt))
                    return false;

                writer.incrementState();

        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean readFrom(ByteBuffer buf, MessageReader reader) {
        reader.setBuffer(buf);

        if (!reader.beforeMessageRead())
            return false;

        switch (reader.state()) {
            case 0:
                id = reader.readTurboSQLUuid("id");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 1:
                keyCnt = reader.readInt("keyCnt");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

        }

        return reader.afterMessageRead(GenerateEncryptionKeyRequest.class);
    }

    /** {@inheritDoc} */
    @Override public short directType() {
        return 162;
    }

    /** {@inheritDoc} */
    @Override public byte fieldsCount() {
        return 2;
    }

    /** {@inheritDoc} */
    @Override public void onAckReceived() {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GenerateEncryptionKeyRequest.class, this);
    }
}
