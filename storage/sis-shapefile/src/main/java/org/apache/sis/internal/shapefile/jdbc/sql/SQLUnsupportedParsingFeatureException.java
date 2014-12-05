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
package org.apache.sis.internal.shapefile.jdbc.sql;

import java.io.File;
import java.sql.SQLException;

/**
 * Exception thrown when a parsing feature is not supported.
 * @author Marc LE BIHAN
 */
public class SQLUnsupportedParsingFeatureException extends SQLException {
    /** Serial ID. */
    private static final long serialVersionUID = 6944940576163675495L;

    /** The SQL Statement that whas attempted. */
    private String m_sql;
    
    /** The database that was queried. */
    private File m_database;
    
    /**
     * Build the exception.
     * @param message Exception message.
     * @param sql SQL Statement who encountered the trouble.
     * @param database The database that was queried.
     */
    public SQLUnsupportedParsingFeatureException(String message, String sql, File database) {
        super(message);
        m_sql = sql;
        m_database = database;
    }
    
    /**
     * Returns the SQL statement who encountered the "end of data" alert.
     * @return SQL statement.
     */
    public String getSQL() {
        return m_sql;
    }
    
    /**
     * Returns the database file that was queried.
     * @return The database that was queried.
     */
    public File getDatabase() {
        return m_database;
    }
}
