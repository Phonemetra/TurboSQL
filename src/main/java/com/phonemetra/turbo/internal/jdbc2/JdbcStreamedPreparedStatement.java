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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import com.phonemetra.turbo.TurboSQLDataStreamer;

/**
 * Prepared statement associated with a data streamer.
 */
class JdbcStreamedPreparedStatement extends JdbcPreparedStatement {
    /** */
    private final TurboSQLDataStreamer<?, ?> streamer;

    /**
     * Creates new prepared statement.
     *
     * @param conn Connection.
     * @param sql SQL query.
     * @param streamer Data streamer to use with this statement. Will be closed on statement close.
     * @param nativeStmt Native statement.
     */
    JdbcStreamedPreparedStatement(JdbcConnection conn, String sql, TurboSQLDataStreamer<?, ?> streamer,
        PreparedStatement nativeStmt) {
        super(conn, sql);

        this.streamer = streamer;

        nativeStatement = nativeStmt;
    }

    /** {@inheritDoc} */
    @Override void closeInternal() throws SQLException {
        streamer.close(false);

        super.closeInternal();
    }

    /** {@inheritDoc} */
    @Override protected void execute0(String sql, Boolean isQuery) throws SQLException {
        assert isQuery == null || !isQuery;

        long updCnt = conn.turboSQL().context().query().streamUpdateQuery(conn.cacheName(), conn.schemaName(),
            streamer, sql, getArgs());

        JdbcResultSet rs = new JdbcResultSet(this, updCnt);

        results = Collections.singletonList(rs);

        curRes = 0;
    }
}
