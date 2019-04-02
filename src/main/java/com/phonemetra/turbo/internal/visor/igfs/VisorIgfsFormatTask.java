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

package com.phonemetra.turbo.internal.visor.igfs;

import com.phonemetra.turbo.TurboSQLException;
import com.phonemetra.turbo.internal.processors.task.GridInternal;
import com.phonemetra.turbo.internal.processors.task.GridVisorManagementTask;
import com.phonemetra.turbo.internal.util.typedef.internal.S;
import com.phonemetra.turbo.internal.visor.VisorJob;
import com.phonemetra.turbo.internal.visor.VisorOneNodeTask;

/**
 * Format IGFS instance.
 */
@GridInternal
@GridVisorManagementTask
public class VisorIgfsFormatTask extends VisorOneNodeTask<VisorIgfsFormatTaskArg, Void> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorIgfsFormatJob job(VisorIgfsFormatTaskArg arg) {
        return new VisorIgfsFormatJob(arg, debug);
    }

    /**
     * Job that format IGFS.
     */
    private static class VisorIgfsFormatJob extends VisorJob<VisorIgfsFormatTaskArg, Void> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * @param arg IGFS name to format.
         * @param debug Debug flag.
         */
        private VisorIgfsFormatJob(VisorIgfsFormatTaskArg arg, boolean debug) {
            super(arg, debug);
        }

        /** {@inheritDoc} */
        @Override protected Void run(VisorIgfsFormatTaskArg arg) {
            try {
                turboSQL.fileSystem(arg.getIgfsName()).clear();
            }
            catch (IllegalArgumentException iae) {
                throw new TurboSQLException("Failed to format IGFS: " + arg.getIgfsName(), iae);
            }

            return null;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorIgfsFormatJob.class, this);
        }
    }
}
