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

package com.phonemetra.turbo.plugin;

import com.phonemetra.turbo.TurboSQL;
import com.phonemetra.turbo.TurboSQLLogger;
import com.phonemetra.turbo.cluster.ClusterNode;
import com.phonemetra.turbo.configuration.CacheConfiguration;
import com.phonemetra.turbo.configuration.TurboSQLConfiguration;
import com.phonemetra.turbo.spi.discovery.DiscoverySpi;

/**
 * Cache plugin context.
 */
public interface CachePluginContext<C extends CachePluginConfiguration> {
    /**
     * @return TurboSQL configuration.
     */
    public TurboSQLConfiguration turboSQLConfiguration();
    
    /**
     * @return TurboSQL cache configuration.
     */
    public CacheConfiguration turboSQLCacheConfiguration();

    /**
     * @return Grid.
     */
    public TurboSQL grid();

    /**
     * Gets local grid node. Instance of local node is provided by underlying {@link DiscoverySpi}
     * implementation used.
     *
     * @return Local grid node.
     * @see DiscoverySpi
     */
    public ClusterNode localNode();

    /**
     * Gets logger for given class.
     *
     * @param cls Class to get logger for.
     * @return Logger.
     */
    public TurboSQLLogger log(Class<?> cls);
}