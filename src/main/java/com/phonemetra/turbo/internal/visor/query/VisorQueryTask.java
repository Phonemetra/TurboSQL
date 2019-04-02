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

package com.phonemetra.turbo.internal.visor.query;

import java.util.Map;
import java.util.UUID;
import com.phonemetra.turbo.internal.processors.query.GridQueryCancel;
import com.phonemetra.turbo.internal.processors.task.GridInternal;
import com.phonemetra.turbo.internal.processors.task.GridVisorManagementTask;
import com.phonemetra.turbo.internal.util.typedef.internal.S;
import com.phonemetra.turbo.internal.visor.VisorEither;
import com.phonemetra.turbo.internal.visor.VisorJob;
import com.phonemetra.turbo.internal.visor.VisorOneNodeTask;
import com.phonemetra.turbo.internal.visor.util.VisorExceptionWrapper;

import static com.phonemetra.turbo.internal.visor.query.VisorQueryUtils.scheduleQueryStart;

/**
 * Task for execute SQL fields query and get first page of results.
 */
@GridInternal
@GridVisorManagementTask
public class VisorQueryTask extends VisorOneNodeTask<VisorQueryTaskArg, VisorEither<VisorQueryResult>> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorQueryJob job(VisorQueryTaskArg arg) {
        return new VisorQueryJob(arg, debug);
    }

    /**
     * Job for execute SCAN or SQL query and get first page of results.
     */
    private static class VisorQueryJob extends VisorJob<VisorQueryTaskArg, VisorEither<VisorQueryResult>> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * Create job with specified argument.
         *
         * @param arg Job argument.
         * @param debug Debug flag.
         */
        private VisorQueryJob(VisorQueryTaskArg arg, boolean debug) {
            super(arg, debug);
        }

        /** {@inheritDoc} */
        @Override protected VisorEither<VisorQueryResult> run(final VisorQueryTaskArg arg) {
            try {
                UUID nid = turboSQL.localNode().id();

                GridQueryCancel cancel = new GridQueryCancel();

                Map<String, VisorQueryHolder> storage = turboSQL.cluster().nodeLocalMap();

                VisorQueryHolder holder = new VisorQueryHolder(true, null, cancel);

                storage.put(holder.getQueryID(), holder);

                scheduleQueryStart(turboSQL, holder, arg, cancel);

                return new VisorEither<>(new VisorQueryResult(nid, holder.getQueryID(), null, null, false, 0));
            }
            catch (Throwable e) {
                return new VisorEither<>(new VisorExceptionWrapper(e));
            }
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorQueryJob.class, this);
        }
    }
}
