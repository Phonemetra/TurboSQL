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

package com.phonemetra.turbo.internal;

import com.phonemetra.turbo.plugin.extensions.communication.Message;
import org.jetbrains.annotations.Nullable;

/**
 * Message with specified custom executor must be processed in the appropriate thread pool.
 */
public interface ExecutorAwareMessage extends Message {
    /**
     * @return Custom executor name. {@code null} In case the custom executor is not provided.
     */
    @Nullable public String executorName();
}
