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

package com.phonemetra.turbo.internal.pagemem.store;

import java.nio.ByteBuffer;
import com.phonemetra.turbo.TurboSQLCheckedException;
import com.phonemetra.turbo.internal.processors.cache.persistence.StorageException;

/**
 * Persistent store of pages.
 */
public interface PageStore {
    /**
     * Checks if page exists.
     *
     * @return {@code True} if page exists.
     */
    public boolean exists();

    /**
     * Allocates next page index.
     *
     * @return Next page index.
     * @throws TurboSQLCheckedException If failed to allocate.
     */
    public long allocatePage() throws TurboSQLCheckedException;

    /**
     * Gets number of allocated pages.
     *
     * @return Number of allocated pages.
     */
    public int pages();

    /**
     * Reads a page.
     *
     * @param pageId Page ID.
     * @param pageBuf Page buffer to read into.
     * @param keepCrc by default reading zeroes CRC which was on file, but you can keep it in pageBuf if set keepCrc
     * @throws TurboSQLCheckedException If reading failed (IO error occurred).
     */
    public void read(long pageId, ByteBuffer pageBuf, boolean keepCrc) throws TurboSQLCheckedException;

    /**
     * Reads a header.
     *
     * @param buf Buffer to write to.
     * @throws TurboSQLCheckedException If failed.
     */
    public void readHeader(ByteBuffer buf) throws TurboSQLCheckedException;

    /**
     * Writes a page.
     *
     * @param pageId Page ID.
     * @param pageBuf Page buffer to write.
     * @param tag Partition file version, 1-based incrementing counter. For outdated pages {@code tag} has lower value,
     * and write does nothing.
     * @param calculateCrc if {@code False} crc calculation will be forcibly skipped.
     * @throws TurboSQLCheckedException If page writing failed (IO error occurred).
     */
    public void write(long pageId, ByteBuffer pageBuf, int tag, boolean calculateCrc) throws TurboSQLCheckedException;

    /**
     * Gets page offset within the store file.
     *
     * @param pageId Page ID.
     * @return Page offset.
     */
    public long pageOffset(long pageId);

    /**
     * Sync method used to ensure that the given pages are guaranteed to be written to the store.
     *
     * @throws TurboSQLCheckedException If sync failed (IO error occurred).
     */
    public void sync() throws TurboSQLCheckedException;

    /**
     * @throws TurboSQLCheckedException If sync failed (IO error occurred).
     */
    public void ensure() throws TurboSQLCheckedException;

    /**
     * @return Page store version.
     */
    public int version();

    /**
     * @param cleanFile {@code True} to delete file.
     * @throws StorageException If failed.
     */
    public void stop(boolean cleanFile) throws StorageException;

    /**
     * Starts recover process.
     */
    public void beginRecover();

    /**
     * Ends recover process.
     *
     * @throws StorageException If failed.
     */
    public void finishRecover() throws StorageException;

    /**
     * Truncates and deletes partition file.
     *
     * @param tag New partition tag.
     * @throws StorageException If failed.
     */
    public void truncate(int tag) throws StorageException;

    /**
     * @return Page size in bytes.
     */
    public int getPageSize();

    /**
     * @return Storage block size or negative value if unknown or not supported.
     */
    public int getBlockSize();

    /**
     * @return Size of the storage in bytes. May differ from {@link #pages()} * {@link #getPageSize()}
     *         due to delayed writes or due to other implementation specific details.
     */
    public long size();

    /**
     * @return Size of the storage adjusted for sparsity in bytes or negative
     *         value if not supported. Should be less than or equal to {@link #size()}.
     * @see #punchHole
     */
    public long getSparseSize();

    /**
     * Should free all the extra storage space after the given number of useful bytes in the given page.
     *
     * @param pageId Page id.
     * @param usefulBytes Number of meaningful bytes from the beginning of the page.
     */
    void punchHole(long pageId, int usefulBytes);
}
