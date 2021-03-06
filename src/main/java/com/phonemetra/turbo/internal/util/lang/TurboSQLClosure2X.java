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

package com.phonemetra.turbo.internal.util.lang;

import com.phonemetra.turbo.TurboSQLCheckedException;
import com.phonemetra.turbo.internal.util.typedef.CX2;
import com.phonemetra.turbo.internal.util.typedef.F;
import com.phonemetra.turbo.lang.TurboSQLBiClosure;

/**
 * Convenient closure subclass that allows for thrown grid exception. This class
 * implements {@link #apply(Object, Object)} method that calls {@link #applyx(Object, Object)}
 * method and properly wraps {@link TurboSQLCheckedException} into {@link GridClosureException} instance.
 * @see CX2
 */
public abstract class TurboSQLClosure2X<E1, E2, R> implements TurboSQLBiClosure<E1, E2, R> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override public R apply(E1 e1, E2 e2) {
        try {
            return applyx(e1, e2);
        }
        catch (TurboSQLCheckedException e) {
            throw F.wrap(e);
        }
    }

    /**
     * Closure body that can throw {@link TurboSQLCheckedException}.
     *
     * @param e1 First bound free variable, i.e. the element the closure is called or closed on.
     * @param e2 Second bound free variable, i.e. the element the closure is called or closed on.
     * @return Optional return value.
     * @throws TurboSQLCheckedException Thrown in case of any error condition inside of the closure.
     */
    public abstract R applyx(E1 e1, E2 e2) throws TurboSQLCheckedException;
}