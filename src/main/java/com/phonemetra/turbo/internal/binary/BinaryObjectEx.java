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

package com.phonemetra.turbo.internal.binary;

import com.phonemetra.turbo.binary.BinaryObject;
import com.phonemetra.turbo.binary.BinaryObjectException;
import com.phonemetra.turbo.binary.BinaryType;
import org.jetbrains.annotations.Nullable;

/**
 * Extended binary object interface.
 */
public interface BinaryObjectEx extends BinaryObject {
    /**
     * @return Type ID.
     */
    public int typeId();

    /**
     * Get raw type.
     *
     * @return Raw type
     * @throws BinaryObjectException If failed.
     */
    @Nullable public BinaryType rawType() throws BinaryObjectException;

    /**
     * Check if flag set.
     *
     * @param flag flag to check.
     * @return {@code true} if flag is set, {@code false} otherwise.
     */
    public boolean isFlagSet(short flag);
}
