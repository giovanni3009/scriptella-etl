/*
 * Copyright 2006 The Scriptella Project Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scriptella.jdbc;

import scriptella.configuration.Resource;
import scriptella.core.StatisticInterceptor;
import scriptella.expressions.Expression;
import scriptella.expressions.ParametersCallback;
import scriptella.spi.QueryCallback;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class SQLSupport {
    private static final Logger LOG = Logger.getLogger(SQLSupport.class.getName());
    protected Resource resource;

    public SQLSupport(Resource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Resource cannot be null");
        }

        this.resource = resource;
    }

    public SQLSupport(final String sql) {
        resource = new Resource() {
            public Reader open() {
                return new StringReader(sql);
            }
        };
    }

    protected int parseAndExecute(final Connection connection,
                                  final ParametersCallback parametersCallback, final QueryCallback queryCallback) {
        Parser parser = new Parser(connection, queryCallback, parametersCallback);

        try {
            final Reader reader = resource.open();
            parser.parse(reader);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        //notify statistic interceptor on number of executed statements
        //Performance Note: this solution may be retrofitted to avoid usage of ThreadLocals
        StatisticInterceptor.statementsExecuted(parser.executedCount);
        return parser.updates ? parser.result : (-1);
    }

    private class Parser extends SQLParserBase {
        int result = 0;
        boolean updates = false;
        Connection con;
        QueryCallback callback;
        ParametersCallback paramsCallback;
        List params = new ArrayList();
        private JDBCSetter setter; //Setter for prepared statement parameters
        private int executedCount;//number of executed statements

        public Parser(Connection con, QueryCallback callback,
                      ParametersCallback params) {
            this.con = con;
            this.callback = callback;
            paramsCallback = params;
        }

        @Override
        protected String handleParameter(final String name,
                                         final boolean expression, boolean jdbcParam) {
            Object p;

            if (expression) {
                p = Expression.compile(name).evaluate(paramsCallback);
            } else {
                p = paramsCallback.getParameter(name);
            }

            if (jdbcParam) { //if insert as prepared stmt parameter
                params.add(p);
                return "?";
            } else { //otherwise return string representation.
                //todo we need to defines rules for toString transformations
                return p == null ? "" : p.toString();
            }
        }

        @Override
        public void statementParsed(final String sql) {
            int v0 = executeStatement(sql);

            if (v0 >= 0) {
                updates = true;
                result += v0;
            }
        }

        int executeStatement(final String sql) {
            PreparedStatement ps = null;
            try {
                ps = prepareStatement(sql);
                int updateCount = executeStatement(ps);
                executedCount++;
                return updateCount;
            } catch (SQLException e) {
                throw new JDBCException("Unable to execute statement", e, sql,
                        params);
            } catch (JDBCException e) {
                //if ProviderException has no SQL - attach it
                if (e.getErrorStatement() == null) {
                    e.setErrorStatement(sql, params);
                }
                throw e; //rethrow
            } finally {
                releaseStatement(ps);
            }

        }

        /**
         * Prepares statement for execution.
         *
         * @param sql statement content to prepare
         * @return prepared statement
         * @throws SQLException if driver fails
         */
        PreparedStatement prepareStatement(final String sql) throws SQLException {
            PreparedStatement ps = con.prepareStatement(sql);
            setter = new JDBCSetter(ps);

            for (int i = 0, n = params.size(); i < n; i++) {
                Object o = params.get(i);
                setter.setObject(i + 1, o);
            }
            return ps;
        }

        /**
         * Executes prepared statement.
         * <p>If statement is query, the returned result set it iterated and {#link #callBack} is
         * called for each row.
         *
         * @param ps
         * @return number of updated rows or -1 for queries.
         * @throws SQLException if JDBC driver fails to execute the operation/
         */
        int executeStatement(PreparedStatement ps) throws SQLException {
            int updateCount = -1;
            if (ps.execute()) {
                if (callback == null) {
                    LOG.warning("Missing callback for query with result set. Queries should have nested script elements.");
                } else {
                    ResultSetAdapter r = null;
                    try {
                        r = new ResultSetAdapter(ps.getResultSet(), paramsCallback);
                        while (r.next()) {
                            callback.processRow(r);
                        }
                    } finally {
                        if (r != null) {
                            r.close();
                        }
                    }
                }
            } else {
                updateCount = ps.getUpdateCount();
            }
            return updateCount;
        }

        private void releaseStatement(final PreparedStatement ps) {
            JDBCUtils.closeSilent(ps);
            params.clear();
            if (setter != null) {
                setter.close(); //closing resources opened by setter
                setter = null;
            }
        }

    }
}