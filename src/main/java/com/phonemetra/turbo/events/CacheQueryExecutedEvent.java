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

package com.phonemetra.turbo.events;

import java.util.UUID;
import com.phonemetra.turbo.cache.CacheEntryEventSerializableFilter;
import com.phonemetra.turbo.cluster.ClusterNode;
import com.phonemetra.turbo.internal.util.tostring.GridToStringInclude;
import com.phonemetra.turbo.internal.util.typedef.internal.S;
import com.phonemetra.turbo.internal.util.typedef.internal.U;
import com.phonemetra.turbo.lang.TurboSQLBiPredicate;
import org.jetbrains.annotations.Nullable;

/**
 * Cache query execution event.
 * <p>
 * Grid events are used for notification about what happens within the grid. Note that by
 * design TurboSQL keeps all events generated on the local node locally and it provides
 * APIs for performing a distributed queries across multiple nodes:
 * <ul>
 *      <li>
 *          {@link com.phonemetra.turbo.TurboSQLEvents#remoteQuery(com.phonemetra.turbo.lang.TurboSQLPredicate, long, int...)} -
 *          asynchronously querying events occurred on the nodes specified, including remote nodes.
 *      </li>
 *      <li>
 *          {@link com.phonemetra.turbo.TurboSQLEvents#localQuery(com.phonemetra.turbo.lang.TurboSQLPredicate, int...)} -
 *          querying only local events stored on this local node.
 *      </li>
 *      <li>
 *          {@link com.phonemetra.turbo.TurboSQLEvents#localListen(com.phonemetra.turbo.lang.TurboSQLPredicate, int...)} -
 *          listening to local grid events (events from remote nodes not included).
 *      </li>
 * </ul>
 * User can also wait for events using method {@link com.phonemetra.turbo.TurboSQLEvents#waitForLocal(com.phonemetra.turbo.lang.TurboSQLPredicate, int...)}.
 * <h1 class="header">Events and Performance</h1>
 * Note that by default all events in TurboSQL are enabled and therefore generated and stored
 * by whatever event storage SPI is configured. TurboSQL can and often does generate thousands events per seconds
 * under the load and therefore it creates a significant additional load on the system. If these events are
 * not needed by the application this load is unnecessary and leads to significant performance degradation.
 * <p>
 * It is <b>highly recommended</b> to enable only those events that your application logic requires
 * by using {@link com.phonemetra.turbo.configuration.TurboSQLConfiguration#getIncludeEventTypes()} method in TurboSQL configuration. Note that certain
 * events are required for TurboSQL's internal operations and such events will still be generated but not stored by
 * event storage SPI if they are disabled in TurboSQL configuration.
 *
 * @see EventType#EVT_CACHE_QUERY_EXECUTED
 * @see EventType#EVTS_CACHE_QUERY
 */
public class CacheQueryExecutedEvent<K, V> extends EventAdapter {
    /** */
    private static final long serialVersionUID = 3738753361235304496L;

    /** Query type. */
    private final String qryType;

    /** Cache name. */
    private final String cacheName;

    /** Class name. */
    private final String clsName;

    /** Clause. */
    private final String clause;

    /** Scan query filter. */
    @GridToStringInclude
    private final TurboSQLBiPredicate<K, V> scanQryFilter;

    /** Continuous query filter. */
    @GridToStringInclude
    private final CacheEntryEventSerializableFilter<K, V> contQryFilter;

    /** Query arguments. */
    @GridToStringInclude
    private final Object[] args;

    /** Security subject ID. */
    private final UUID subjId;

    /** Task name. */
    private final String taskName;

    /**
     * @param node Node where event was fired.
     * @param msg Event message.
     * @param type Event type.
     * @param qryType Query type.
     * @param cacheName Cache name.
     * @param clsName Class name.
     * @param clause Clause.
     * @param scanQryFilter Scan query filter.
     * @param args Query arguments.
     * @param subjId Security subject ID.
     */
    public CacheQueryExecutedEvent(
        ClusterNode node,
        String msg,
        int type,
        String qryType,
        @Nullable String cacheName,
        @Nullable String clsName,
        @Nullable String clause,
        @Nullable TurboSQLBiPredicate<K, V> scanQryFilter,
        @Nullable CacheEntryEventSerializableFilter<K, V> contQryFilter,
        @Nullable Object[] args,
        @Nullable UUID subjId,
        @Nullable String taskName) {
        super(node, msg, type);

        assert qryType != null;

        this.qryType = qryType;
        this.cacheName = cacheName;
        this.clsName = clsName;
        this.clause = clause;
        this.scanQryFilter = scanQryFilter;
        this.contQryFilter = contQryFilter;
        this.args = args;
        this.subjId = subjId;
        this.taskName = taskName;
    }

    /**
     * Gets query type.
     *
     * @return Query type. Can be {@code "SQL"}, {@code "SQL_FIELDS"}, {@code "FULL_TEXT"}, {@code "SCAN"}, 
     * {@code "CONTINUOUS"} or {@code "SPI"}.
     */
    public String queryType() {
        return qryType;
    }

    /**
     * Gets cache name on which query was executed.
     *
     * @return Cache name.
     */
    @Nullable public String cacheName() {
        return cacheName;
    }

    /**
     * Gets queried class name.
     * <p>
     * Applicable for {@code SQL} and @{code full text} queries.
     *
     * @return Queried class name.
     */
    @Nullable public String className() {
        return clsName;
    }

    /**
     * Gets query clause.
     * <p>
     * Applicable for {@code SQL}, {@code SQL fields} and @{code full text} queries.
     *
     * @return Query clause.
     */
    @Nullable public String clause() {
        return clause;
    }

    /**
     * Gets scan query filter.
     * <p>
     * Applicable for {@code scan} queries.
     *
     * @return Scan query filter.
     */
    @Nullable public TurboSQLBiPredicate<K, V> scanQueryFilter() {
        return scanQryFilter;
    }

    /**
     * Gets continuous query filter.
     * <p>
     * Applicable for {@code continuous} queries.
     *
     * @return Continuous query filter.
     */
    @Nullable public CacheEntryEventSerializableFilter<K, V> continuousQueryFilter() {
        return contQryFilter;
    }

    /**
     * Gets query arguments.
     * <p>
     * Applicable for {@code SQL} and {@code SQL fields} queries.
     *
     * @return Query arguments.
     */
    @Nullable public Object[] arguments() {
        return args;
    }

    /**
     * Gets security subject ID.
     *
     * @return Security subject ID.
     */
    @Nullable public UUID subjectId() {
        return subjId;
    }

    /**
     * Gets the name of the task that executed the query (if any).
     *
     * @return Task name.
     */
    @Nullable public String taskName() {
        return taskName;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(CacheQueryExecutedEvent.class, this,
            "nodeId8", U.id8(node().id()),
            "msg", message(),
            "type", name(),
            "tstamp", timestamp());
    }
}