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

import java.util.Collection;
import javax.cache.CacheException;
import com.phonemetra.turbo.TurboSQL;
import com.phonemetra.turbo.TurboSQLCache;
import com.phonemetra.turbo.TurboSQLFileSystem;
import com.phonemetra.turbo.cluster.ClusterNode;
import com.phonemetra.turbo.configuration.CacheConfiguration;
import com.phonemetra.turbo.internal.cluster.TurboSQLClusterEx;
import com.phonemetra.turbo.internal.processors.cache.GridCacheUtilityKey;
import com.phonemetra.turbo.internal.processors.cache.TurboSQLInternalCache;
import com.phonemetra.turbo.internal.processors.hadoop.Hadoop;
import com.phonemetra.turbo.lang.TurboSQLBiTuple;
import com.phonemetra.turbo.lang.TurboSQLPredicate;
import org.jetbrains.annotations.Nullable;

/**
 * Extended Grid interface which provides some additional methods required for kernal and Visor.
 */
public interface TurboSQLEx extends TurboSQL {
    /**
     * Gets utility cache.
     *
     * @return Utility cache.
     */
    public <K extends GridCacheUtilityKey, V> TurboSQLInternalCache<K, V> utilityCache();

    /**
     * Gets the cache instance for the given name if one is configured or
     * <tt>null</tt> otherwise returning even non-public caches.
     *
     * @param <K> Key type.
     * @param <V> Value type.
     * @param name Cache name.
     * @return Cache instance for given name or <tt>null</tt> if one does not exist.
     */
    @Nullable public <K, V> TurboSQLInternalCache<K, V> cachex(String name);

    /**
     * Gets configured cache instance that satisfy all provided predicates including non-public caches. If no
     * predicates provided - all configured caches will be returned.
     *
     * @param p Predicates. If none provided - all configured caches will be returned.
     * @return Configured cache instances that satisfy all provided predicates.
     */
    public Collection<TurboSQLInternalCache<?, ?>> cachesx(@Nullable TurboSQLPredicate<? super TurboSQLInternalCache<?, ?>>... p);

    /**
     * Gets existing cache with the given name or creates new one with the given configuration.
     * <p>
     * If a cache with the same name already exists, this method will not check that the given
     * configuration matches the configuration of existing cache and will return an instance
     * of the existing cache.
     *
     * @param cacheCfg Cache configuration to use.
     * @param sql {@code true} if this call is triggered by SQL command {@code CREATE TABLE}, {@code false} otherwise.
     * @return Tuple [Existing or newly created cache; {@code true} if cache was newly crated, {@code false} otherwise]
     * @throws CacheException If error occurs.
     */
    public <K, V> TurboSQLBiTuple<TurboSQLCache<K, V>, Boolean> getOrCreateCache0(CacheConfiguration<K, V> cacheCfg,
        boolean sql) throws CacheException;

    /**
     * Stops dynamically started cache.
     *
     * @param cacheName Cache name to stop.
     * @param sql {@code true} if only cache created with SQL command {@code CREATE TABLE} should be affected,
     *     {@code false} otherwise.
     * @return {@code true} if cache has been stopped as the result of this call, {@code false} otherwise.
     * @throws CacheException If error occurs.
     */
    public boolean destroyCache0(String cacheName, boolean sql) throws CacheException;

    /**
     * Checks if the event type is user-recordable.
     *
     * @param type Event type to check.
     * @return {@code true} if passed event should be recorded, {@code false} - otherwise.
     */
    public boolean eventUserRecordable(int type);

    /**
     * Checks whether all provided events are user-recordable.
     * <p>
     * Note that this method supports only predefined TurboSQL events.
     *
     * @param types Event types.
     * @return Whether all events are recordable.
     * @throws IllegalArgumentException If {@code types} contains user event type.
     */
    public boolean allEventsUserRecordable(int[] types);

    /**
     * Whether or not remote JMX management is enabled for this node.
     *
     * @return {@code True} if remote JMX management is enabled - {@code false} otherwise.
     */
    public boolean isJmxRemoteEnabled();

    /**
     * Whether or not node restart is enabled.
     *
     * @return {@code True} if restart mode is enabled, {@code false} otherwise.
     */
    public boolean isRestartEnabled();

    /**
     * Get IGFS instance returning null if it doesn't exist.
     *
     * @param name IGFS name.
     * @return IGFS.
     */
    @Nullable public TurboSQLFileSystem igfsx(String name);

    /**
     * Get Hadoop facade.
     *
     * @return Hadoop.
     */
    public Hadoop hadoop();

    /** {@inheritDoc} */
    @Override TurboSQLClusterEx cluster();

    /**
     * Get latest version in string form.
     *
     * @return Latest version.
     */
    @Nullable public String latestVersion();

    /**
     * Gets local grid node.
     *
     * @return Local grid node.
     */
    public ClusterNode localNode();

    /**
     * Internal context.
     *
     * @return Kernal context.
     */
    public GridKernalContext context();

    /**
     * Get rebalance enabled flag.
     *
     * @return {@code True} if rebalance enabled on node, {@code False} otherwise.
     */
    public boolean isRebalanceEnabled();

    /**
     * Set rebalance enable flag on node.
     *
     * @param rebalanceEnabled rebalance enabled flag.
     */
    public void rebalanceEnabled(boolean rebalanceEnabled);
}
