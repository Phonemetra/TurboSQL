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
import com.phonemetra.turbo.lang.TurboSQLBiPredicate;

/**
 * Defines {@code alias} for {@link com.phonemetra.turbo.lang.TurboSQLBiPredicate} by extending it. Since Java doesn't provide type aliases
 * (like Scala, for example) we resort to these types of measures. This is intended to provide for more
 * concise code in cases when readability won't be sacrificed. For more information see {@link com.phonemetra.turbo.lang.TurboSQLBiPredicate}.
 * @param <T1> Type of the first free variable, i.e. the element the closure is called on.
 * @param <T2> Type of the second free variable, i.e. the element the closure is called on.
 * @see GridFunc
 * @see com.phonemetra.turbo.lang.TurboSQLBiPredicate
 */
public interface P2<T1, T2> extends TurboSQLBiPredicate<T1, T2> { /* No-op. */ }