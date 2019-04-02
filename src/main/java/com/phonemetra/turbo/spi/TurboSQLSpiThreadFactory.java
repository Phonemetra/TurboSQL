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

package com.phonemetra.turbo.spi;

import java.util.concurrent.ThreadFactory;
import com.phonemetra.turbo.TurboSQLLogger;

/**
 * This class provides implementation of {@link ThreadFactory}  factory
 * for creating grid SPI threads.
 */
public class TurboSQLSpiThreadFactory implements ThreadFactory {
    /** */
    private final TurboSQLLogger log;

    /** */
    private final String turboSQLInstanceName;

    /** */
    private final String threadName;

    /**
     * @param turboSQLInstanceName TurboSQL instance name, possibly {@code null} for default TurboSQL instance.
     * @param threadName Name for threads created by this factory.
     * @param log Grid logger.
     */
    public TurboSQLSpiThreadFactory(String turboSQLInstanceName, String threadName, TurboSQLLogger log) {
        assert log != null;
        assert threadName != null;

        this.turboSQLInstanceName = turboSQLInstanceName;
        this.threadName = threadName;
        this.log = log;
    }

    /** {@inheritDoc} */
    @Override public Thread newThread(final Runnable r) {
        return new TurboSQLSpiThread(turboSQLInstanceName, threadName, log) {
            /** {@inheritDoc} */
            @Override protected void body() {
                r.run();
            }
        };
    }
}