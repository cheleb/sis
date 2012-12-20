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
package org.apache.sis.geometry;

/*
 * Do not add dependency to java.awt.Rectangle2D in this class, because not every platforms
 * support Java2D (e.g. Android),  or applications that do not need it may want to avoid to
 * force installation of the Java2D module (e.g. JavaFX/SWT).
 */
import java.util.Arrays;
import java.io.Serializable;
import java.lang.reflect.Field;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.geometry.MismatchedReferenceSystemException;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.*;
import static org.apache.sis.math.MathFunctions.isNegative;
import static org.apache.sis.math.MathFunctions.isSameSign;


/**
 * A minimum bounding box or rectangle. Regardless of dimension, an {@code Envelope} can
 * be represented without ambiguity as two {@linkplain DirectPosition direct positions}
 * (coordinate points). To encode an {@code Envelope}, it is sufficient to encode these
 * two points.
 *
 * {@note <code>Envelope</code> uses an arbitrary <cite>Coordinate Reference System</cite>, which
 * doesn't need to be geographic. This is different than the <code>GeographicBoundingBox</code>
 * class provided in the metadata package, which can be used as a kind of envelope restricted to
 * a Geographic CRS having Greenwich prime meridian.}
 *
 * This particular implementation of {@code Envelope} is said "General" because it uses
 * coordinates of an arbitrary number of dimensions. This is in contrast with
 * {@link Envelope2D}, which can use only two-dimensional coordinates.
 *
 * <p>A {@code GeneralEnvelope} can be created in various ways:</p>
 * <ul>
 *   <li>{@linkplain #GeneralEnvelope(int) From a given number of dimension}, with all ordinates initialized to 0.</li>
 *   <li>{@linkplain #GeneralEnvelope(double[], double[]) From two coordinate points}.</li>
 *   <li>{@linkplain #GeneralEnvelope(Envelope) From a an other envelope} (copy constructor).</li>
 *   <li>{@linkplain #GeneralEnvelope(GeographicBoundingBox) From a geographic bounding box}.</li>
 *   <li>{@linkplain #GeneralEnvelope(CharSequence) From a character sequence}
 *       representing a {@code BBOX} in <cite>Well Known Text</cite> (WKT) format.</li>
 * </ul>
 *
 * {@section Spanning the anti-meridian of a Geographic CRS}
 * The <cite>Web Coverage Service</cite> (WCS) specification authorizes (with special treatment)
 * cases where <var>upper</var> &lt; <var>lower</var> at least in the longitude case. They are
 * envelopes crossing the anti-meridian, like the red box below (the green box is the usual case).
 * The default implementation of methods listed in the right column can handle such cases.
 *
 * <table class="compact" align="center"><tr><td>
 *   <img src="doc-files/AntiMeridian.png">
 * </td><td>
 * Supported methods:
 * <ul>
 *   <li>{@link #getMinimum(int)}</li>
 *   <li>{@link #getMaximum(int)}</li>
 *   <li>{@link #getMedian(int)}</li>
 *   <li>{@link #getSpan(int)}</li>
 *   <li>{@link #isEmpty()}</li>
 *   <li>{@link #contains(DirectPosition) contains(DirectPosition)}</li>
 *   <li>{@link #contains(Envelope, boolean) contains(Envelope, boolean)}</li>
 *   <li>{@link #intersects(Envelope, boolean) intersects(Envelope, boolean)}</li>
 *   <li>{@link #intersect(Envelope)}</li>
 *   <li>{@link #add(Envelope)}</li>
 *   <li>{@link #add(DirectPosition)}</li>
 * </ul>
 * </td></tr></table>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 *
 * @see Envelope2D
 * @see org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox
 */
public class GeneralEnvelope extends ArrayEnvelope implements Cloneable, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1752330560227688940L;

    /**
     * Used for setting the {@link #ordinates} field during a {@link #clone()} operation only.
     * Will be fetch when first needed.
     */
    private static volatile Field ordinatesField;

    /**
     * Constructs an envelope defined by two corners given as direct positions.
     * If at least one corner is associated to a CRS, then the new envelope will also
     * be associated to that CRS.
     *
     * @param  lowerCorner The limits in the direction of decreasing ordinate values for each dimension.
     * @param  upperCorner The limits in the direction of increasing ordinate values for each dimension.
     * @throws MismatchedDimensionException If the two positions do not have the same dimension.
     * @throws MismatchedReferenceSystemException If the CRS of the two position are not equal.
     */
    public GeneralEnvelope(final DirectPosition lowerCorner, final DirectPosition upperCorner)
            throws MismatchedDimensionException, MismatchedReferenceSystemException
    {
        super(lowerCorner, upperCorner);
    }

    /**
     * Constructs an envelope defined by two corners given as sequences of ordinate values.
     * The Coordinate Reference System is initially {@code null}.
     *
     * @param  lowerCorner The limits in the direction of decreasing ordinate values for each dimension.
     * @param  upperCorner The limits in the direction of increasing ordinate values for each dimension.
     * @throws MismatchedDimensionException If the two sequences do not have the same length.
     */
    public GeneralEnvelope(final double[] lowerCorner, final double[] upperCorner) throws MismatchedDimensionException {
        super(lowerCorner, upperCorner);
    }

    /**
     * Constructs an empty envelope of the specified dimension. All ordinates
     * are initialized to 0 and the coordinate reference system is undefined.
     *
     * @param dimension The envelope dimension.
     */
    public GeneralEnvelope(final int dimension) {
        super(dimension);
    }

    /**
     * Constructs an empty envelope with the specified coordinate reference system.
     * All ordinate values are initialized to 0.
     *
     * @param crs The coordinate reference system.
     */
    public GeneralEnvelope(final CoordinateReferenceSystem crs) {
        super(crs);
    }

    /**
     * Constructs a new envelope with the same data than the specified envelope.
     *
     * @param envelope The envelope to copy.
     *
     * @see #castOrCopy(Envelope)
     */
    public GeneralEnvelope(final Envelope envelope) {
        super(envelope);
    }

    /**
     * Constructs a new envelope with the same data than the specified geographic bounding box.
     * The coordinate reference system is set to {@code "CRS:84"}.
     *
     * @param box The bounding box to copy.
     */
    public GeneralEnvelope(final GeographicBoundingBox box) {
        super(box);
    }

    /**
     * Constructs a new envelope initialized to the values parsed from the given string in
     * <cite>Well Known Text</cite> (WKT) format. The given string is typically a {@code BOX}
     * element like below:
     *
     * {@preformat wkt
     *     BOX(-180 -90, 180 90)
     * }
     *
     * However this constructor is lenient to other geometry types like {@code POLYGON}.
     * Actually this constructor ignores the geometry type and just applies the following
     * simple rules:
     *
     * <ul>
     *   <li>Character sequences complying to the rules of Java identifiers are skipped.</li>
     *   <li>Coordinates are separated by a coma ({@code ,}) character.</li>
     *   <li>The ordinates in a coordinate are separated by a space.</li>
     *   <li>Ordinate numbers are assumed formatted in US locale.</li>
     *   <li>The coordinate having the highest dimension determines the dimension of this envelope.</li>
     * </ul>
     *
     * This constructor does not check the consistency of the provided WKT. For example it doesn't
     * check that every points in a {@code LINESTRING} have the same dimension. However this
     * constructor ensures that the parenthesis are balanced, in order to catch some malformed WKT.
     *
     * <p>The following examples can be parsed by this constructor in addition of the standard
     * {@code BOX} element. This constructor creates the bounding box of those geometries:</p>
     *
     * <ul>
     *   <li>{@code POINT(6 10)}</li>
     *   <li>{@code MULTIPOLYGON(((1 1, 5 1, 1 5, 1 1),(2 2, 3 2, 3 3, 2 2)))}</li>
     *   <li>{@code GEOMETRYCOLLECTION(POINT(4 6),LINESTRING(3 8,7 10))}</li>
     * </ul>
     *
     * @param  wkt The {@code BOX}, {@code POLYGON} or other kind of element to parse.
     * @throws IllegalArgumentException If the given string can not be parsed.
     *
     * @see Envelopes#parseWKT(String)
     * @see Envelopes#toWKT(Envelope)
     */
    public GeneralEnvelope(final CharSequence wkt) throws IllegalArgumentException {
        super(wkt);
    }

    /**
     * Returns the given envelope as a {@code GeneralEnvelope} instance. If the given envelope
     * is already an instance of {@code GeneralEnvelope}, then it is returned unchanged.
     * Otherwise the coordinate values and the CRS of the given envelope are
     * {@linkplain #GeneralEnvelope(Envelope) copied} in a new {@code GeneralEnvelope}.
     *
     * @param  envelope The envelope to cast, or {@code null}.
     * @return The values of the given envelope as a {@code GeneralEnvelope} instance.
     *
     * @see AbstractEnvelope#castOrCopy(Envelope)
     * @see ImmutableEnvelope#castOrCopy(Envelope)
     */
    public static GeneralEnvelope castOrCopy(final Envelope envelope) {
        if (envelope == null || envelope instanceof GeneralEnvelope) {
            return (GeneralEnvelope) envelope;
        }
        return new GeneralEnvelope(envelope);
    }

    /**
     * Sets the coordinate reference system in which the coordinate are given.
     * This method <strong>does not</strong> reproject the envelope, and do not
     * check if the envelope is contained in the new domain of validity.
     *
     * <p>If the envelope coordinates need to be transformed to the new CRS, consider
     * using {@link Envelopes#transform(Envelope, CoordinateReferenceSystem)} instead.</p>
     *
     * @param  crs The new coordinate reference system, or {@code null}.
     * @throws MismatchedDimensionException if the specified CRS doesn't have the expected
     *         number of dimensions.
     */
    public void setCoordinateReferenceSystem(final CoordinateReferenceSystem crs)
            throws MismatchedDimensionException
    {
        AbstractDirectPosition.ensureDimensionMatch(crs, getDimension());
        this.crs = crs;
    }

    /**
     * Sets the envelope range along the specified dimension.
     *
     * @param  dimension The dimension to set.
     * @param  lower     The limit in the direction of decreasing ordinate values.
     * @param  upper     The limit in the direction of increasing ordinate values.
     * @throws IndexOutOfBoundsException If the given index is out of bounds.
     */
    @Override
    public void setRange(final int dimension, final double lower, final double upper)
            throws IndexOutOfBoundsException
    {
        final int d = ordinates.length >>> 1;
        ensureValidIndex(d, dimension);
        ordinates[dimension + d] = upper;
        ordinates[dimension]     = lower;
    }

    /**
     * Sets the envelope to the specified values, which must be the lower corner coordinates
     * followed by upper corner coordinates. The number of arguments provided shall be twice
     * this {@linkplain #getDimension envelope dimension}, and minimum shall not be greater
     * than maximum.
     *
     * <p><b>Example:</b></p>
     * (<var>x</var><sub>min</sub>, <var>y</var><sub>min</sub>, <var>z</var><sub>min</sub>,
     *  <var>x</var><sub>max</sub>, <var>y</var><sub>max</sub>, <var>z</var><sub>max</sub>)
     *
     * @param ordinates The new ordinate values.
     */
    public void setEnvelope(final double... ordinates) {
        if ((ordinates.length & 1) != 0) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.OddArrayLength_1, ordinates.length));
        }
        final int dimension  = ordinates.length >>> 1;
        final int check = this.ordinates.length >>> 1;
        if (dimension != check) {
            throw new MismatchedDimensionException(Errors.format(
                    Errors.Keys.MismatchedDimension_3, "ordinates", dimension, check));
        }
        System.arraycopy(ordinates, 0, this.ordinates, 0, ordinates.length);
    }

    /**
     * Sets this envelope to the same coordinate values than the specified envelope.
     * If the given envelope has a non-null Coordinate Reference System (CRS), then
     * the CRS of this envelope will be set to the CRS of the given envelope.
     *
     * @param  envelope The envelope to copy coordinates from.
     * @throws MismatchedDimensionException if the specified envelope doesn't have
     *         the expected number of dimensions.
     */
    public void setEnvelope(final Envelope envelope) throws MismatchedDimensionException {
        ensureNonNull("envelope", envelope);
        final int dimension = ordinates.length >>> 1;
        AbstractDirectPosition.ensureDimensionMatch("envelope", envelope.getDimension(), dimension);
        if (envelope instanceof ArrayEnvelope) {
            System.arraycopy(((ArrayEnvelope) envelope).ordinates, 0, ordinates, 0, ordinates.length);
        } else {
            final DirectPosition lower = envelope.getLowerCorner();
            final DirectPosition upper = envelope.getUpperCorner();
            for (int i=0; i<dimension; i++) {
                ordinates[i]           = lower.getOrdinate(i);
                ordinates[i+dimension] = upper.getOrdinate(i);
            }
        }
        final CoordinateReferenceSystem envelopeCRS = envelope.getCoordinateReferenceSystem();
        if (envelopeCRS != null) {
            crs = envelopeCRS;
            assert crs.getCoordinateSystem().getDimension() == getDimension() : crs;
            assert envelope.getClass() != getClass() || equals(envelope) : envelope;
        }
    }

    /**
     * Sets the lower corner to {@linkplain Double#NEGATIVE_INFINITY negative infinity}
     * and the upper corner to {@linkplain Double#POSITIVE_INFINITY positive infinity}.
     * The {@linkplain #getCoordinateReferenceSystem() coordinate reference system}
     * (if any) stay unchanged.
     */
    public void setToInfinite() {
        final int mid = ordinates.length >>> 1;
        Arrays.fill(ordinates, 0,   mid,              Double.NEGATIVE_INFINITY);
        Arrays.fill(ordinates, mid, ordinates.length, Double.POSITIVE_INFINITY);
        assert isInfinite() : this;
    }

    /**
     * Returns {@code true} if at least one ordinate has an
     * {@linkplain Double#isInfinite infinite} value.
     *
     * @return {@code true} if this envelope has infinite value.
     */
    public boolean isInfinite() {
        for (int i=0; i<ordinates.length; i++) {
            if (Double.isInfinite(ordinates[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets all ordinate values to {@linkplain Double#NaN NaN}.
     * The {@linkplain #getCoordinateReferenceSystem() coordinate reference system}
     * (if any) stay unchanged.
     *
     * @see #isNull()
     */
    public void setToNull() {
        Arrays.fill(ordinates, Double.NaN);
        assert isNull() : this;
    }

    /**
     * Adds to this envelope a point of the given array.
     * This method does not check for anti-meridian spanning. It is invoked only
     * by the {@link Envelopes} transform methods, which build "normal" envelopes.
     *
     * @param  array The array which contains the ordinate values.
     * @param  offset Index of the first valid ordinate value in the given array.
     */
    final void add(final double[] array, final int offset) {
        final int dim = ordinates.length >>> 1;
        for (int i=0; i<dim; i++) {
            final double value = array[offset + i];
            if (value < ordinates[i    ]) ordinates[i    ] = value;
            if (value > ordinates[i+dim]) ordinates[i+dim] = value;
        }
    }

    /**
     * Adds a point to this envelope. The resulting envelope is the smallest envelope that
     * contains both the original envelope and the specified point.
     *
     * <p>After adding a point, a call to {@link #contains(DirectPosition) contains(DirectPosition)}
     * with the added point as an argument will return {@code true}, except if one of the point
     * ordinates was {@link Double#NaN} in which case the corresponding ordinate has been ignored.</p>
     *
     * {@note This method assumes that the specified point uses the same CRS than this envelope.
     *        For performance raisons, it will no be verified unless Java assertions are enabled.}
     *
     * {@section Spanning the anti-meridian of a Geographic CRS}
     * This method supports envelopes spanning the anti-meridian. In such cases it is possible to
     * move both envelope borders in order to encompass the given point, as illustrated below (the
     * new point is represented by the {@code +} symbol):
     *
     * {@preformat text
     *    ─────┐   + ┌─────
     *    ─────┘     └─────
     * }
     *
     * The default implementation moves only the border which is closest to the given point.
     *
     * @param  position The point to add.
     * @throws MismatchedDimensionException if the specified point doesn't have the expected dimension.
     * @throws AssertionError If assertions are enabled and the envelopes have mismatched CRS.
     */
    public void add(final DirectPosition position) throws MismatchedDimensionException {
        ensureNonNull("position", position);
        final int dim = ordinates.length >>> 1;
        AbstractDirectPosition.ensureDimensionMatch("position", position.getDimension(), dim);
        assert equalsIgnoreMetadata(crs, position.getCoordinateReferenceSystem(), true) : position;
        for (int i=0; i<dim; i++) {
            final double value = position.getOrdinate(i);
            final double min = ordinates[i];
            final double max = ordinates[i+dim];
            if (!isNegative(max - min)) { // Standard case, or NaN.
                if (value < min) ordinates[i    ] = value;
                if (value > max) ordinates[i+dim] = value;
            } else {
                /*
                 * Spanning the anti-meridian. The [max…min] range (not that min/max are
                 * interchanged) is actually an exclusion area. Changes only the closest
                 * side.
                 */
                addToClosest(i, value, max, min);
            }
        }
        assert contains(position) || isEmpty() || hasNaN(position) : position;
    }

    /**
     * Invoked when a point is added to a range spanning the anti-meridian.
     * In the example below, the new point is represented by the {@code +}
     * symbol. The point is added only on the closest side.
     *
     * {@preformat text
     *    ─────┐   + ┌─────
     *    ─────┘     └─────
     * }
     *
     * @param  i     The dimension of the ordinate
     * @param  value The ordinate value to add to this envelope.
     * @param  left  The border on the left side,  which is the <em>max</em> value (yes, this is confusing!)
     * @param  right The border on the right side, which is the <em>min</em> value (yes, this is confusing!)
     */
    private void addToClosest(int i, final double value, double left, double right) {
        left = value - left;
        if (left > 0) {
            right -= value;
            if (right > 0) {
                if (right > left) {
                    i += (ordinates.length >>> 1);
                }
                ordinates[i] = value;
            }
        }
    }

    /**
     * Adds an envelope object to this envelope. The resulting envelope is the union of the
     * two {@code Envelope} objects.
     *
     * {@note This method assumes that the specified envelope uses the same CRS than this envelope.
     *        For performance raisons, it will no be verified unless Java assertions are enabled.}
     *
     * {@section Spanning the anti-meridian of a Geographic CRS}
     * This method supports envelopes spanning the anti-meridian. If one or both envelopes span
     * the anti-meridian, then the result of the {@code add} operation may be an envelope expanding
     * to infinities. In such case, the ordinate range will be either [-∞…∞] or [0…-0] depending on
     * whatever the original range span the anti-meridian or not.
     *
     * @param  envelope the {@code Envelope} to add to this envelope.
     * @throws MismatchedDimensionException if the specified envelope doesn't
     *         have the expected dimension.
     * @throws AssertionError If assertions are enabled and the envelopes have mismatched CRS.
     */
    public void add(final Envelope envelope) throws MismatchedDimensionException {
        ensureNonNull("envelope", envelope);
        final int dim = ordinates.length >>> 1;
        AbstractDirectPosition.ensureDimensionMatch("envelope", envelope.getDimension(), dim);
        assert equalsIgnoreMetadata(crs, envelope.getCoordinateReferenceSystem(), true) : envelope;
        final DirectPosition lower = envelope.getLowerCorner();
        final DirectPosition upper = envelope.getUpperCorner();
        for (int i=0; i<dim; i++) {
            final double min0 = ordinates[i];
            final double max0 = ordinates[i+dim];
            final double min1 = lower.getOrdinate(i);
            final double max1 = upper.getOrdinate(i);
            final boolean sp0 = isNegative(max0 - min0);
            final boolean sp1 = isNegative(max1 - min1);
            if (sp0 == sp1) {
                /*
                 * Standard case (for rows in the above pictures), or case where both envelopes
                 * span the anti-meridian (which is almost the same with an additional post-add
                 * check).
                 *    ┌──────────┐          ┌──────────┐
                 *    │  ┌────┐  │    or    │  ┌───────┼──┐
                 *    │  └────┘  │          │  └───────┼──┘
                 *    └──────────┘          └──────────┘
                 *
                 *    ────┐  ┌────          ────┐  ┌────
                 *    ──┐ │  │ ┌──    or    ────┼──┼─┐┌─
                 *    ──┘ │  │ └──          ────┼──┼─┘└─
                 *    ────┘  └────          ────┘  └────
                 */
                if (min1 < min0) ordinates[i    ] = min1;
                if (max1 > max0) ordinates[i+dim] = max1;
                if (!sp0 || isNegativeUnsafe(ordinates[i+dim] - ordinates[i])) {
                    continue; // We are done, go to the next dimension.
                }
                // If we were spanning the anti-meridian before the union but
                // are not anymore after the union, we actually merged to two
                // sides, so the envelope is spanning to infinities. The code
                // close to the end of this loop will set an infinite range.
            } else if (sp0) {
                /*
                 * Only this envelope spans the anti-meridian; the given envelope is normal or
                 * has NaN values.  First we need to exclude the cases were the given envelope
                 * is fully included in this envelope:
                 *   ──────────┐  ┌─────
                 *     ┌────┐  │  │
                 *     └────┘  │  │
                 *   ──────────┘  └─────
                 */
                if (max1 <= max0) continue;  // This is the case of above picture.
                if (min1 >= min0) continue;  // Like above picture, but on the right side.
                /*
                 * At this point, the given envelope partially overlaps the "exclusion area"
                 * of this envelope or has NaN values. We will move at most one edge of this
                 * envelope, in order to leave as much free space as possible.
                 *    ─────┐      ┌─────
                 *       ┌─┼────┐ │
                 *       └─┼────┘ │
                 *    ─────┘      └─────
                 */
                final double left  = min1 - max0;
                final double right = min0 - max1;
                if (left > 0 || right > 0) {
                    // The < and > checks below are not completly redundant.
                    // The difference is when a value is NaN.
                    if (left > right) ordinates[i    ] = min1;
                    if (right > left) ordinates[i+dim] = max1; // This is the case illustrated above.
                    continue; // We are done, go to the next dimension.
                }
                // If we reach this point, the given envelope fills completly the "exclusion area"
                // of this envelope. As a consequence this envelope is now spanning to infinities.
                // We will set that fact close to the end of this loop.
            } else {
                /*
                 * Opposite of above case: this envelope is "normal" or has NaN values, and the
                 * given envelope spans to infinities.
                 */
                if (max0 <= max1 || min0 >= min1) {
                    ordinates[i]     = min1;
                    ordinates[i+dim] = max1;
                    continue;
                }
                final double left  = min0 - max1;
                final double right = min1 - max0;
                if (left > 0 || right > 0) {
                    if (left > right) ordinates[i+dim] = max1;
                    if (right > left) ordinates[i    ] = min1;
                    continue;
                }
            }
            /*
             * If we reach that point, we went in one of the many cases where the envelope
             * has been expanded to infinity.  Declares an infinite range while preserving
             * the "normal" / "anti-meridian spanning" state.
             */
            if (sp0) {
                ordinates[i    ] = +0.0;
                ordinates[i+dim] = -0.0;
            } else {
                ordinates[i    ] = Double.NEGATIVE_INFINITY;
                ordinates[i+dim] = Double.POSITIVE_INFINITY;
            }
        }
        assert contains(envelope, true) || isEmpty() || hasNaN(envelope) : this;
    }

    /**
     * Sets this envelope to the intersection if this envelope with the specified one.
     *
     * {@note This method assumes that the specified envelope uses the same CRS than this envelope.
     *        For performance raisons, it will no be verified unless Java assertions are enabled.}
     *
     * {@section Spanning the anti-meridian of a Geographic CRS}
     * This method supports envelopes spanning the anti-meridian.
     *
     * @param  envelope the {@code Envelope} to intersect to this envelope.
     * @throws MismatchedDimensionException if the specified envelope doesn't
     *         have the expected dimension.
     * @throws AssertionError If assertions are enabled and the envelopes have mismatched CRS.
     */
    public void intersect(final Envelope envelope) throws MismatchedDimensionException {
        ensureNonNull("envelope", envelope);
        final int dim = ordinates.length >>> 1;
        AbstractDirectPosition.ensureDimensionMatch("envelope", envelope.getDimension(), dim);
        assert equalsIgnoreMetadata(crs, envelope.getCoordinateReferenceSystem(), true) : envelope;
        final DirectPosition lower = envelope.getLowerCorner();
        final DirectPosition upper = envelope.getUpperCorner();
        for (int i=0; i<dim; i++) {
            final double min0  = ordinates[i];
            final double max0  = ordinates[i+dim];
            final double min1  = lower.getOrdinate(i);
            final double max1  = upper.getOrdinate(i);
            final double span0 = max0 - min0;
            final double span1 = max1 - min1;
            if (isSameSign(span0, span1)) { // Always 'false' if any value is NaN.
                /*
                 * First, verify that the two envelopes intersect.
                 *     ┌──────────┐             ┌─────────────┐
                 *     │  ┌───────┼──┐    or    │  ┌───────┐  │
                 *     │  └───────┼──┘          │  └───────┘  │
                 *     └──────────┘             └─────────────┘
                 */
                if ((min1 > max0 || max1 < min0) && !isNegativeUnsafe(span0)) {
                    /*
                     * The check for !isNegative(span0) is because if both envelopes span the
                     * anti-merdian, then there is always an intersection on both side no matter
                     * what envelope ordinates are because both envelopes extend toward infinities:
                     *     ────┐  ┌────            ────┐  ┌────
                     *     ──┐ │  │ ┌──     or     ────┼──┼─┐┌─
                     *     ──┘ │  │ └──            ────┼──┼─┘└─
                     *     ────┘  └────            ────┘  └────
                     * Since we excluded the above case, entering in this block means that the
                     * envelopes are "normal" and do not intersect, so we set ordinates to NaN.
                     *   ┌────┐
                     *   │    │     ┌────┐
                     *   │    │     └────┘
                     *   └────┘
                     */
                    ordinates[i] = ordinates[i+dim] = Double.NaN;
                    continue;
                }
            } else {
                int intersect = 0; // A bitmask of intersections (two bits).
                if (!Double.isNaN(span0) && !Double.isNaN(span1)) {
                    if (isNegativeUnsafe(span0)) {
                        /*
                         * The first line below checks for the case illustrated below. The second
                         * line does the same check, but with the small rectangle on the right side.
                         *    ─────┐      ┌─────              ──────────┐  ┌─────
                         *       ┌─┼────┐ │           or        ┌────┐  │  │
                         *       └─┼────┘ │                     └────┘  │  │
                         *    ─────┘      └─────              ──────────┘  └─────
                         */
                        if (min1 <= max0) {intersect  = 1; ordinates[i    ] = min1;}
                        if (max1 >= min0) {intersect |= 2; ordinates[i+dim] = max1;}
                    } else {
                        // Same than above, but with indices 0 and 1 interchanged.
                        // No need to set ordinate values since they would be the same.
                        if (min0 <= max1) {intersect  = 1;}
                        if (max0 >= min1) {intersect |= 2;}
                    }
                }
                /*
                 * Cases 0 and 3 are illustrated below. In case 1 and 2, we will set
                 * only the ordinate value which has not been set by the above code.
                 *
                 *                [intersect=0]          [intersect=3]
                 *              ─────┐     ┌─────      ─────┐     ┌─────
                 *  negative:    max0│ ┌─┐ │min0          ┌─┼─────┼─┐
                 *                   │ └─┘ │              └─┼─────┼─┘
                 *              ─────┘     └─────      ─────┘     └─────
                 *
                 *               max1  ┌─┐  min1          ┌─────────┐
                 * positive:    ─────┐ │ │ ┌─────      ───┼─┐     ┌─┼───
                 *              ─────┘ │ │ └─────      ───┼─┘     └─┼───
                 *                     └─┘                └─────────┘
                 */
                switch (intersect) {
                    default: throw new AssertionError(intersect);
                    case 1: if (max1 < max0) ordinates[i+dim] = max1; break;
                    case 2: if (min1 > min0) ordinates[i    ] = min1; break;
                    case 3: // Fall through
                    case 0: {
                        // Before to declare the intersection as invalid, verify if the envelope
                        // actually span the whole Earth. In such case, the intersection is a no-
                        // operation (or a copy operation).
                        final double min, max;
                        final double csSpan = getSpan(getAxis(crs, i));
                        if (span1 >= csSpan) {
                            min = min0;
                            max = max0;
                        } else if (span0 >= csSpan) {
                            min = min1;
                            max = max1;
                        } else {
                            min = Double.NaN;
                            max = Double.NaN;
                        }
                        ordinates[i]     = min;
                        ordinates[i+dim] = max;
                        break;
                    }
                }
                continue;
            }
            if (min1 > min0) ordinates[i    ] = min1;
            if (max1 < max0) ordinates[i+dim] = max1;
        }
        // Tests only if the interection result is non-empty.
        assert isEmpty() || AbstractEnvelope.castOrCopy(envelope).contains(this, true) : this;
    }

    /**
     * Returns a deep copy of this envelope.
     *
     * @return A clone of this envelope.
     */
    @Override
    public GeneralEnvelope clone() {
        try {
            Field field = ordinatesField;
            if (field == null) {
                field = ArrayEnvelope.class.getDeclaredField("ordinates");
                field.setAccessible(true);
                ordinatesField = field;
            }
            GeneralEnvelope e = (GeneralEnvelope) super.clone();
            field.set(e, ordinates.clone());
            return e;
        } catch (CloneNotSupportedException | ReflectiveOperationException exception) {
            // Should not happen, since we are cloneable, the
            // field is known to exist and we made it accessible.
            throw new AssertionError(exception);
        }
    }
}
