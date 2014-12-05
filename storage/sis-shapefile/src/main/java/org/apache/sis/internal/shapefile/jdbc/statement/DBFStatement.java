/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.internal.shapefile.jdbc.statement;

import java.sql.*;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.sis.internal.shapefile.jdbc.SQLConnectionClosedException;
import org.apache.sis.internal.shapefile.jdbc.connection.DBFConnection;
import org.apache.sis.internal.shapefile.jdbc.resultset.AbstractResultSet;
import org.apache.sis.internal.shapefile.jdbc.resultset.DBFRecordBasedResultSet;
import org.apache.sis.internal.shapefile.jdbc.sql.SQLInvalidStatementException;
import org.apache.sis.storage.shapefile.Database;


/**
 * DBF Statement.
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
public class DBFStatement extends AbstractStatement {
    /** Connection this statement is relying on. */
    private DBFConnection connection;

    /** ResultSets that are currently opened. */
    private HashSet<AbstractResultSet> m_openedResultSets = new HashSet<>(); 
    
    /** The current result set, or {@code null} if none. */
    private AbstractResultSet currentResultSet;

    /** Indicates if the statement is currently closed. */
    private boolean isClosed;

    /**
     * Constructs a statement.
     * @param cnt Connection associated to this statement.
     */
    public DBFStatement(DBFConnection cnt) {
        this.connection = cnt;
    }

    /**
     * Returns the connection.
     * @return Connection.
     * @throws SQLConnectionClosedException if the connection is closed.
     */
    @Override
    public Connection getConnection() throws SQLConnectionClosedException {
        assertNotClosed();
        return connection;
    }
    
    /**
     * @see java.sql.Statement#execute(java.lang.String)
     */
    @Override
    public boolean execute(String sql) throws SQLException {
        // We are only able to handle SQL Queries at this time.
        if (sql.trim().toLowerCase().startsWith("select")) {
            executeQuery(sql);
            return true; // The result is a ResultSet.
        }
        else
            throw unsupportedOperation("execute something else than a SELECT statement");
    }

    /**
     * Executes the given SQL statement.
     * @return SQL Statement.
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLInvalidStatementException if the SQL Statement is invalid.
     */
    @Override
    public ResultSet executeQuery(final String sql) throws SQLConnectionClosedException, SQLInvalidStatementException {
        Objects.requireNonNull(sql, "The SQL query cannot be null.");
        assertNotClosed();
        
        DBFRecordBasedResultSet rs = new DBFRecordBasedResultSet(this, sql);
        registerResultSet(rs);
        return rs;
    }

    /**
     * Returns the JDBC interface implemented by this class.
     * This is used for formatting error messages.
     */
    @Override
    final protected Class<?> getInterface() {
        return Statement.class;
    }

    /**
     * @see java.sql.Statement#getMaxRows()
     */
    @Override
    public int getMaxRows() {
        return 0;
    }

    /**
     * Returns the result set created by the last call to {@link #executeQuery(String)}.
     * @return ResultSet.
     */
    @Override
    public ResultSet getResultSet() throws SQLConnectionClosedException {
        assertNotClosed();
        return currentResultSet;
    }

    /**
     * @see java.sql.Statement#getUpdateCount()
     */
    @Override
    public int getUpdateCount() {
        return 0; // We currently only handle select statements.
    }

    /**
     * @see java.sql.Statement#close()
     */
    @Override
    public void close() {
        if (isClosed())
            return;
        
        if (currentResultSet != null) {
            // Inform that this ResultSet could have been closed but that we are handling this :
            // Some developpers may expect their ResultSet should have been closed before in their program.
            format(Level.INFO, "log.closing_underlying_resultset", currentResultSet);
            currentResultSet.close();
            
            currentResultSet = null;
        }
        
        // Check if all the underlying ResultSets that has been opened with this statement has been closed.
        // If not, we log a warning to help the developper.
        if (m_openedResultSets.size() > 0) {
            format(Level.WARNING, "log.resultsets_left_opened", m_openedResultSets.size(), m_openedResultSets.stream().map(AbstractResultSet::toString).collect(Collectors.joining(", ")));  
        }
        
        isClosed = true;
        connection.notifyCloseStatement(this);
    }

    /**
     * Returns {@code true} if this statement has been closed or if the underlying connection is closed.
     * @return true if the database is closed.
     */
    @Override
    public boolean isClosed() {
        return isClosed || connection.isClosed();
    }
    
    /**
     * Returns the binary representation of the database.
     * This function shall not check the closed state of this connection, as it can be used in exception messages descriptions.
     * @return Database.
     */
    public Database getDatabase() {
        return connection.getDatabase();
    }

    /**
     * Asserts that the connection and the statement are together opened.
     * @throws SQLConnectionClosedException if one of them is closed.
     */
    public void assertNotClosed() throws SQLConnectionClosedException {
        connection.assertNotClosed(); // First, the underlying shall not be closed.
        
        // Then, this statement shouldn't be closed too.
        if (isClosed) {
            throw new SQLConnectionClosedException(format("excp.closed_statement", connection.getDatabase().getFile().getName()), null, connection.getDatabase().getFile());
        }
    }
    
    /**
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isAssignableFrom(getInterface());
    }

    /**
     * Method called by ResultSet class to notity this statement that a resultSet has been closed.
     * @param rs ResultSet that has been closed.
     */
    public void notifyCloseResultSet(AbstractResultSet rs) {
        Objects.requireNonNull(rs, "The ResultSet notified being closed cannot be null.");
        
        // If this ResultSet was the current ResultSet, now there is no more current ResultSet.
        if (currentResultSet == rs)
            currentResultSet = null;
        
        if (m_openedResultSets.remove(rs) == false) {
            throw new RuntimeException(format(Level.SEVERE, "assert.resultset_not_opened_by_me", rs, toString()));
        }
    }

    /**
     * Register a ResultSet as opened.
     * @param rs Result Set.
     */
    public void registerResultSet(AbstractResultSet rs) {
        currentResultSet = rs;
        m_openedResultSets.add(rs);
    }

    /**
     * @see java.sql.Statement#setMaxRows(int)
     */
    @Override
    public void setMaxRows(int max) {
        this.logUnsupportedOperation(MessageFormat.format("setMaxRows({0,number})", max));
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return format("toString", connection != null ? connection.toString() : null, isClosed() == false);
    }
}
