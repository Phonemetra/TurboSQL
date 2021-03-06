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

package com.phonemetra.turbo.internal.util.typedef;

import com.phonemetra.turbo.internal.util.lang.GridFunc;
import com.phonemetra.turbo.lang.TurboSQLPredicate;

/**
 * Defines {@code alias} for {@link com.phonemetra.turbo.lang.TurboSQLPredicate} by extending it. Since Java doesn't provide type aliases
 * (like Scala, for example) we resort to these types of measures. This is intended to provide for more
 * concise code in cases when readability won't be sacrificed. For more information see {@link com.phonemetra.turbo.lang.TurboSQLPredicate}.
 * @param <E1> Type of the free variable, i.e. the element the predicate is called on.
 * @see GridFunc
 * @see com.phonemetra.turbo.lang.TurboSQLPredicate
 */
public interface P1<E1> extends TurboSQLPredicate<E1> { /* No-op. */ }