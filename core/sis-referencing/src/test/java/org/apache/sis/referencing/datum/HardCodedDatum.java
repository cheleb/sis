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
package org.apache.sis.referencing.datum;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import javax.measure.unit.NonSI;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.datum.VerticalDatumType;
import org.apache.sis.test.mock.GeodeticDatumMock;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.internal.referencing.VerticalDatumTypes;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;

import static org.opengis.referencing.IdentifiedObject.*;


/**
 * Collection of datum for testing purpose.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.5
 * @module
 */
public final strictfp class HardCodedDatum {
    /**
     * Greenwich meridian (EPSG:8901), with angular measurements in decimal degrees.
     */
    public static final DefaultPrimeMeridian GREENWICH = new DefaultPrimeMeridian(
            properties("Greenwich", "8901"), 0, NonSI.DEGREE_ANGLE);

    /**
     * Paris meridian (EPSG:8903), with angular measurements in grad.
     *
     * @since 0.5
     */
    public static final DefaultPrimeMeridian PARIS = new DefaultPrimeMeridian(
            properties("Paris", "8903"), 2.5969213, NonSI.GRADE);

    /**
     * Old Paris meridian (EPSG:8914) defined as 2°20'13.95"E.
     * Equivalent to 2.596898 grad (value given by EPSG).
     *
     * <p>When used together with {@link #PARIS}, this prime meridian is useful for testing
     * two real-world prime meridians that are very close together but still different.</p>
     *
     * @since 0.5
     */
    public static final DefaultPrimeMeridian PARIS_RGS = new DefaultPrimeMeridian(
            properties("Paris RGS", "8914"), 2 + (20 + 13.95/60)/60, NonSI.DEGREE_ANGLE);

    /**
     * WGS 1984 datum (EPSG:6326). Prime meridian is Greenwich.
     * This datum is used in GPS systems.
     */
    public static final DefaultGeodeticDatum WGS84 = new DefaultGeodeticDatum(
            properties("World Geodetic System 1984", "6326"),
            new DefaultEllipsoid(GeodeticDatumMock.WGS84.getEllipsoid()), GREENWICH);

    /**
     * WGS 1972 datum (EPSG:6322). Prime meridian is Greenwich.
     * This datum is used, together with {@linkplain #WGS84}, in
     * {@linkplain org.apache.sis.referencing.operation.transform.EarthGravitationalModel
     * Earth Gravitational Model}.
     */
    public static final DefaultGeodeticDatum WGS72 = new DefaultGeodeticDatum(
            properties("World Geodetic System 1972", "6322"),
            new DefaultEllipsoid(GeodeticDatumMock.WGS84.getEllipsoid()), GREENWICH);

    /**
     * Nouvelle Triangulation Française datum (EPSG:6807). Prime meridian is Paris.
     *
     * @since 0.5
     */
    public static final DefaultGeodeticDatum NTF = new DefaultGeodeticDatum(
            properties("Nouvelle Triangulation Française", "6807"),
            new DefaultEllipsoid(GeodeticDatumMock.NTF.getEllipsoid()), PARIS);

    /**
     * Spherical datum based on GRS 1980 Authalic Sphere (EPSG:6047). Prime meridian is Greenwich.
     */
    public static final DefaultGeodeticDatum SPHERE = new DefaultGeodeticDatum(
            properties("Not specified (based on GRS 1980 Authalic Sphere)", "6047"),
            new DefaultEllipsoid(GeodeticDatumMock.SPHERE.getEllipsoid()), GREENWICH);

    /**
     * Mean sea level, which can be used as an approximation of geoid.
     */
    public static final DefaultVerticalDatum MEAN_SEA_LEVEL = new DefaultVerticalDatum(
            properties("Mean Sea Level", "5100"), VerticalDatumType.GEOIDAL);

    /**
     * Ellipsoid for measurements of height above the ellipsoid.
     * This is not a valid datum according ISO 19111, but is used by Apache SIS for internal calculation.
     */
    public static final DefaultVerticalDatum ELLIPSOID = new DefaultVerticalDatum(
            properties("Ellipsoid", null), VerticalDatumTypes.ELLIPSOIDAL);

    /**
     * Default datum for time measured since January 1st, 1970 at 00:00 UTC.
     */
    public static final DefaultTemporalDatum UNIX = new DefaultTemporalDatum(
            properties("UNIX", null), new Date(0));

    /**
     * Default datum for time measured since November 17, 1858 at 00:00 UTC.
     */
    public static final DefaultTemporalDatum MODIFIED_JULIAN = new DefaultTemporalDatum(
            properties("Modified Julian", null), new Date(-40587 * (24*60*60*1000L)));

    /**
     * Image with {@link PixelInCell#CELL_CENTER}.
     */
    public static final DefaultImageDatum IMAGE = new DefaultImageDatum(
            properties("Image", null), PixelInCell.CELL_CENTER);

    /**
     * An engineering datum for unknown coordinate reference system. Such CRS are usually
     * assumed Cartesian, but will not have any transformation path to other CRS.
     */
    public static final DefaultEngineeringDatum UNKNOWN = new DefaultEngineeringDatum(properties("Unknown", null));

    /**
     * Creates a map of properties for the given name and EPSG code.
     */
    private static Map<String,?> properties(final String name, final String code) {
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(NAME_KEY, name);
        if (code != null) {
            properties.put(IDENTIFIERS_KEY, new NamedIdentifier(HardCodedCitations.EPSG, code));
        }
        return properties;
    }

    /**
     * Do not allow instantiation of this class.
     */
    private HardCodedDatum() {
    }
}
