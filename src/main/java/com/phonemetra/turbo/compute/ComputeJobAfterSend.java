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

package com.phonemetra.turbo.compute;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation allows to call a method right after the job has been
 * successfully sent for execution. It is useful to clean up the internal
 * state of the job when it is not immediately needed.
 * <p>
 * This annotation can be applied to methods of {@link ComputeJob} instance only.
 * It is invoked on the caller node after the job has been sent to remote node for execution.
 * <p>
 * Example:
 * <pre name="code" class="java">
 * public class MyGridJob implements ComputeJob {
 *     ...
 *     &#64;ComputeJobAfterSend
 *     public void onJobAfterSend() {
 *          ...
 *     }
 *     ...
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ComputeJobAfterSend {
    // No-op.
}