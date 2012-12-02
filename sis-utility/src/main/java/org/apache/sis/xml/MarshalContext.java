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
package org.apache.sis.xml;

import java.util.Locale;
import java.util.TimeZone;
import org.apache.sis.util.Version;


/**
 * Context of a marshalling or unmarshalling process.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.07)
 * @version 0.3
 * @module
 */
public abstract class MarshalContext {
    /**
     * A constant for GML version 3.0.
     *
     * @see #getVersion(String)
     */
    public static final Version GML_3_0 = new Version("3.0");

    /**
     * A constant for GML version 3.2.
     *
     * @see #getVersion(String)
     */
    public static final Version GML_3_2 = new Version("3.2");

    /**
     * Creates a new (un)marshalling context.
     */
    protected MarshalContext() {
    }

    /**
     * Returns the schema version of the XML document being (un)marshalled.
     * The {@code prefix} argument can be any of the following values (case-sensitive):
     *
     * <table>
     *   <tr>
     *     <th>Prefix</th>
     *     <th>Standard</th>
     *     <th>Typical values</th>
     *   </tr>
     *   <tr>
     *     <td>gml</td><td>Geographic Markup Language</td>
     *     <td>{@link #GML_3_0}, {@link #GML_3_2}</td>
     *   </tr>
     * </table>
     *
     * @param  prefix One of the above-cited prefix.
     * @return The version for the given schema, or {@code null} if unknown.
     */
    public abstract Version getVersion(final String prefix);

    /**
     * Returns the locale to use for (un)marshalling, or {@code null} if no locale were explicitly
     * specified. The locale returned by this method can be used for choosing a language in an
     * {@link org.opengis.util.InternationalString}.
     *
     * <p>Note that the locale may varies in different fragments of the same XML document.
     * In particular children of {@link org.opengis.metadata.Metadata} inherit the locale
     * specified by the {@link org.opengis.metadata.Metadata#getLanguage()} attribute.</p>
     *
     * @return The locale for the XML fragment being (un)marshalled, or {@code null} is unspecified.
     */
    public abstract Locale getLocale();

    /**
     * Returns the timezone to use for (un)marshalling, or {@code null} if none were explicitely
     * specified. If {@code null}, then an implementation-default (typically UTC) timezone is
     * assumed.
     *
     * @return The timezone for the XML fragment being (un)marshalled, or {@code null} if unspecified.
     */
    public abstract TimeZone getTimeZone();
}