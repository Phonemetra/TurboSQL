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
import java.util.Collection;
import com.phonemetra.turbo.internal.GridDirectCollection;
import com.phonemetra.turbo.internal.util.typedef.internal.S;
import com.phonemetra.turbo.lang.TurboSQLUuid;
import com.phonemetra.turbo.plugin.extensions.communication.Message;
import com.phonemetra.turbo.plugin.extensions.communication.MessageCollectionItemType;
import com.phonemetra.turbo.plugin.extensions.communication.MessageReader;
import com.phonemetra.turbo.plugin.extensions.communication.MessageWriter;

/**
 * Generate encryption key response.
 */
public class GenerateEncryptionKeyResponse implements Message {
    /** */
    private static final long serialVersionUID = 0L;

    /** Request message ID. */
    private TurboSQLUuid id;

    /** */
    @GridDirectCollection(byte[].class)
    private Collection<byte[]> encKeys;

    /** */
    public GenerateEncryptionKeyResponse() {
    }

    /**
     * @param id Request id.
     * @param encKeys Encryption keys.
     */
    public GenerateEncryptionKeyResponse(TurboSQLUuid id, Collection<byte[]> encKeys) {
        this.id = id;
        this.encKeys = encKeys;
    }

    /**
     * @return Request id.
     */
    public TurboSQLUuid requestId() {
        return id;
    }

    /**
     * @return Encryption keys.
     */
    public Collection<byte[]> encryptionKeys() {
        return encKeys;
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
                if (!writer.writeCollection("encKeys", encKeys, MessageCollectionItemType.BYTE_ARR))
                    return false;

                writer.incrementState();

            case 1:
                if (!writer.writeTurboSQLUuid("id", id))
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
                encKeys = reader.readCollection("encKeys", MessageCollectionItemType.BYTE_ARR);

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 1:
                id = reader.readTurboSQLUuid("id");

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

        }

        return reader.afterMessageRead(GenerateEncryptionKeyResponse.class);
    }

    /** {@inheritDoc} */
    @Override public short directType() {
        return 163;
    }

    /** {@inheritDoc} */
    @Override public byte fieldsCount() {
        return 2;
    }

    /** {@inheritDoc} */
    @Override public void onAckReceived() {
        //No-op.
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GenerateEncryptionKeyResponse.class, this);
    }
}
