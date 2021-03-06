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

package com.phonemetra.turbo.internal.visor;

import com.phonemetra.turbo.TurboSQLException;
import com.phonemetra.turbo.compute.ComputeJobAdapter;
import com.phonemetra.turbo.internal.TurboSQLEx;
import com.phonemetra.turbo.internal.util.typedef.internal.U;
import com.phonemetra.turbo.resources.TurboSQLInstanceResource;
import org.jetbrains.annotations.Nullable;

import static com.phonemetra.turbo.internal.visor.util.VisorTaskUtils.logFinish;
import static com.phonemetra.turbo.internal.visor.util.VisorTaskUtils.logStart;

/**
 * Base class for Visor jobs.
 *
 * @param <A> Job argument type.
 * @param <R> Job result type.
 */
public abstract class VisorJob<A, R> extends ComputeJobAdapter {
    /** Auto-injected grid instance. */
    @TurboSQLInstanceResource
    protected transient TurboSQLEx turboSQL;

    /** Job start time. */
    protected transient long start;

    /** Debug flag. */
    protected boolean debug;

    /**
     * Create job with specified argument.
     *
     * @param arg Job argument.
     * @param debug Flag indicating whether debug information should be printed into node log.
     */
    protected VisorJob(@Nullable A arg, boolean debug) {
        super(arg);

        this.debug = debug;
    }

    /** {@inheritDoc} */
    @Nullable @Override public Object execute() {
        start = U.currentTimeMillis();

        A arg = argument(0);

        try {
            if (debug)
                logStart(turboSQL.log(), getClass(), start);

            return run(arg);
        }
        finally {
            if (debug)
                logFinish(turboSQL.log(), getClass(), start);
        }
    }

    /**
     * Execution logic of concrete job.
     *
     * @param arg Task argument.
     * @return Result.
     * @throws TurboSQLException In case of error.
     */
    protected abstract R run(@Nullable A arg) throws TurboSQLException;
}