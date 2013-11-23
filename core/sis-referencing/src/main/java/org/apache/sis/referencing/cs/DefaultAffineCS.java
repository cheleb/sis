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
package org.apache.sis.referencing.cs;

import java.util.Map;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import org.opengis.referencing.cs.AffineCS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Immutable;


/**
 * A 2- or 3-dimensional coordinate system with straight axes that are not necessarily orthogonal.
 *
 * <table class="sis">
 * <tr><th>Used with CRS type(s)</th></tr>
 * <tr><td>
 *   {@linkplain org.geotoolkit.referencing.crs.DefaultEngineeringCRS Engineering},
 *   {@linkplain org.geotoolkit.referencing.crs.DefaultImageCRS       Image}
 * </td></tr></table>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
@Immutable
public class DefaultAffineCS extends AbstractCS implements AffineCS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7977674229369042440L;

    /**
     * Constructs a coordinate system of arbitrary dimension. This constructor is
     * not public because {@code AffineCS} are restricted to 2 and 3 dimensions.
     */
    DefaultAffineCS(final Map<String,?> properties, final CoordinateSystemAxis[] axis) {
        super(properties, axis);
    }

    /**
     * Constructs a two-dimensional coordinate system from a set of properties.
     * The properties map is given unchanged to the
     * {@linkplain AbstractCS#AbstractCS(Map,CoordinateSystemAxis[]) super-class constructor}.
     *
     * @param properties The properties to be given to the identified object.
     * @param axis0 The first axis.
     * @param axis1 The second axis.
     */
    public DefaultAffineCS(final Map<String,?>   properties,
                           final CoordinateSystemAxis axis0,
                           final CoordinateSystemAxis axis1)
    {
        super(properties, axis0, axis1);
    }

    /**
     * Constructs a three-dimensional coordinate system from a set of properties.
     * The properties map is given unchanged to the superclass constructor.
     *
     * @param properties The properties to be given to the identified object.
     * @param axis0 The first axis.
     * @param axis1 The second axis.
     * @param axis2 The third axis.
     */
    public DefaultAffineCS(final Map<String,?>   properties,
                           final CoordinateSystemAxis axis0,
                           final CoordinateSystemAxis axis1,
                           final CoordinateSystemAxis axis2)
    {
        super(properties, axis0, axis1, axis2);
    }

    /**
     * Creates a new coordinate system with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param cs The coordinate system to copy.
     *
     * @see #castOrCopy(AffineCS)
     */
    protected DefaultAffineCS(final AffineCS cs) {
        super(cs);
    }

    /**
     * Returns a SIS coordinate system implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * attribute values of the given object.
     *
     * <p>This method checks for the {@link CartesianCS} sub-interface. If that interface is found,
     * then this method delegates to the corresponding {@code castOrCopy} static method.</p>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultAffineCS castOrCopy(final AffineCS object) {
        if (object instanceof CartesianCS) {
            return DefaultCartesianCS.castOrCopy((CartesianCS) object);
        }
        return (object == null) || (object instanceof DefaultAffineCS)
                ? (DefaultAffineCS) object : new DefaultAffineCS(object);
    }

    /**
     * Returns {@code true} if the given axis direction is allowed for this coordinate system.
     * The default implementation accepts all directions except temporal ones
     * (i.e. {@link AxisDirection#FUTURE FUTURE} and {@link AxisDirection#PAST PAST}).
     */
    @Override
    final boolean isCompatibleDirection(final AxisDirection direction) {
        return !AxisDirection.FUTURE.equals(AxisDirections.absolute(direction));
    }

    /**
     * Returns {@code true} if the given unit is compatible with {@linkplain SI#METRE metres}.
     * In addition, this method also accepts {@link Unit#ONE}, which is used for coordinates in a grid.
     * This method is invoked at construction time for checking units compatibility.
     */
    @Override
    final boolean isCompatibleUnit(final AxisDirection direction, final Unit<?> unit) {
        return Units.isLinear(unit) || Unit.ONE.equals(unit);
        // Note: this condition is also coded in PredefinedCS.rightHanded(AffineCS).
    }
}
