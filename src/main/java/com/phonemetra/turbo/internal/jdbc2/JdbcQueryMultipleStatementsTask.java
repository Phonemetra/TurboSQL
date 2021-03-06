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

package com.phonemetra.turbo.internal.jdbc2;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.phonemetra.turbo.TurboSQL;
import com.phonemetra.turbo.TurboSQLJdbcDriver;
import com.phonemetra.turbo.cache.query.FieldsQueryCursor;
import com.phonemetra.turbo.cache.query.SqlFieldsQuery;
import com.phonemetra.turbo.internal.GridKernalContext;
import com.phonemetra.turbo.internal.TurboSQLKernal;
import com.phonemetra.turbo.internal.processors.cache.QueryCursorImpl;
import com.phonemetra.turbo.internal.processors.cache.query.SqlFieldsQueryEx;
import com.phonemetra.turbo.internal.util.typedef.internal.S;
import com.phonemetra.turbo.lang.TurboSQLCallable;
import com.phonemetra.turbo.resources.TurboSQLInstanceResource;

/**
 * Task for SQL queries execution through {@link TurboSQLJdbcDriver}.
 * The query can contains several SQL statements.
 */
class JdbcQueryMultipleStatementsTask implements TurboSQLCallable<List<JdbcStatementResultInfo>> {
    /** Serial version uid. */
    private static final long serialVersionUID = 0L;

    /** TurboSQL. */
    @TurboSQLInstanceResource
    private TurboSQL turboSQL;

    /** Schema name. */
    private final String schemaName;

    /** Sql. */
    private final String sql;

    /** Operation type flag - query or not. */
    private Boolean isQry;

    /** Args. */
    private final Object[] args;

    /** Fetch size. */
    private final int fetchSize;

    /** Local execution flag. */
    private final boolean loc;

    /** Local query flag. */
    private final boolean locQry;

    /** Collocated query flag. */
    private final boolean collocatedQry;

    /** Distributed joins flag. */
    private final boolean distributedJoins;

    /** Enforce join order flag. */
    private final boolean enforceJoinOrder;

    /** Lazy query execution flag. */
    private final boolean lazy;

    /**
     * @param turboSQL TurboSQL.
     * @param schemaName Schema name.
     * @param sql Sql query.
     * @param isQry Operation type flag - query or not - to enforce query type check.
     * @param loc Local execution flag.
     * @param args Args.
     * @param fetchSize Fetch size.
     * @param locQry Local query flag.
     * @param collocatedQry Collocated query flag.
     * @param distributedJoins Distributed joins flag.
     * @param enforceJoinOrder Enforce joins order falg.
     * @param lazy Lazy query execution flag.
     */
    public JdbcQueryMultipleStatementsTask(TurboSQL turboSQL, String schemaName, String sql, Boolean isQry, boolean loc,
        Object[] args, int fetchSize, boolean locQry, boolean collocatedQry, boolean distributedJoins,
        boolean enforceJoinOrder, boolean lazy) {
        this.turboSQL = turboSQL;
        this.args = args;
        this.schemaName = schemaName;
        this.sql = sql;
        this.isQry = isQry;
        this.fetchSize = fetchSize;
        this.loc = loc;
        this.locQry = locQry;
        this.collocatedQry = collocatedQry;
        this.distributedJoins = distributedJoins;
        this.enforceJoinOrder = enforceJoinOrder;
        this.lazy = lazy;
    }

    /** {@inheritDoc} */
    @Override public List<JdbcStatementResultInfo> call() throws Exception {
        SqlFieldsQuery qry = (isQry != null ? new SqlFieldsQueryEx(sql, isQry) : new SqlFieldsQuery(sql))
            .setArgs(args);

        qry.setPageSize(fetchSize);
        qry.setLocal(locQry);
        qry.setCollocated(collocatedQry);
        qry.setDistributedJoins(distributedJoins);
        qry.setEnforceJoinOrder(enforceJoinOrder);
        qry.setLazy(lazy);
        qry.setSchema(schemaName);

        GridKernalContext ctx = ((TurboSQLKernal)turboSQL).context();

        List<FieldsQueryCursor<List<?>>> curs = ctx.query().querySqlFields(qry, true, false);

        List<JdbcStatementResultInfo> resultsInfo = new ArrayList<>(curs.size());

        for (FieldsQueryCursor<List<?>> cur0 : curs) {
            QueryCursorImpl<List<?>> cur = (QueryCursorImpl<List<?>>)cur0;

            long updCnt = -1;

            UUID qryId = null;

            if (!cur.isQuery()) {
                List<List<?>> items = cur.getAll();

                assert items != null && items.size() == 1 && items.get(0).size() == 1
                    && items.get(0).get(0) instanceof Long :
                    "Invalid result set for not-SELECT query. [qry=" + sql +
                        ", res=" + S.toString(List.class, items) + ']';

                updCnt = (Long)items.get(0).get(0);

                cur.close();
            }
            else {
                qryId = UUID.randomUUID();

                JdbcQueryTask.Cursor jdbcCur = new JdbcQueryTask.Cursor(cur, cur.iterator());

                JdbcQueryTask.addCursor(qryId, jdbcCur);

                if (!loc)
                    JdbcQueryTask.scheduleRemoval(qryId);
            }

            JdbcStatementResultInfo resInfo = new JdbcStatementResultInfo(cur.isQuery(), qryId, updCnt);

            resultsInfo.add(resInfo);
        }

        return resultsInfo;
    }

}
