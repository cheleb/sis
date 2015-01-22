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
package org.apache.sis.referencing.operation.builder;

import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.apache.sis.math.Plane;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * Creates a linear (usually affine) transform which will map approximatively the given source points to
 * the given target points. The transform coefficients are determined using a <cite>least squares</cite>
 * estimation method.
 *
 * <div class="note"><b>Implementation note:</b>
 * The quantity that current implementation tries to minimize is not strictly the squared Euclidian distance.
 * The current implementation rather processes each target dimension independently, which may not give the same
 * result than if we tried to minimize the squared Euclidian distances by taking all dimensions in account together.
 * This algorithm may change in future SIS versions.
 * </div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public class LinearTransformBuilder {
    /**
     * The arrays of source ordinate values, for example (x[], y[], z[]).
     * This is {@code null} if not yet specified.
     */
    private double[][] sources;

    /**
     * The arrays of target ordinate values, for example (x[], y[], z[]).
     * This is {@code null} if not yet specified.
     */
    private double[][] targets;

    /**
     * An estimation of the Pearson correlation coefficient for each target dimension.
     * This is {@code null} if not yet specified.
     */
    private double[] correlation;

    /**
     * Creates a new linear transform builder.
     */
    public LinearTransformBuilder() {
    }

    /**
     * Extracts the ordinate values of the given points into separated arrays, one for each dimension.
     *
     * @param points The points from which to extract the ordinate values.
     * @param dimension The expected number of dimensions.
     */
    private static double[][] toArrays(final DirectPosition[] points, final int dimension) {
        final int length = points.length;
        final double[][] ordinates = new double[dimension][length];
        for (int j=0; j<length; j++) {
            final DirectPosition p = points[j];
            final int d = p.getDimension();
            if (d != dimension) {
                throw new MismatchedDimensionException(Errors.format(
                        Errors.Keys.MismatchedDimension_3, "points[" + j + ']', dimension, d));
            }
            for (int i=0; i<dimension; i++) {
                ordinates[i][j] = p.getOrdinate(i);
            }
        }
        return ordinates;
    }

    /**
     * Sets the source points. The number of points shall be the same than the number of target points.
     *
     * <p><b>Limitation:</b> in current implementation, the source points must be two-dimensional.
     * But this restriction may be removed in a future SIS version.</p>
     *
     * @param  points The source points.
     * @throws MismatchedDimensionException if at least one point does not have the expected number of dimensions.
     */
    public void setSourcePoints(final DirectPosition... points) throws MismatchedDimensionException {
        ArgumentChecks.ensureNonNull("points", points);
        sources = toArrays(points, 2);
        correlation = null;
    }

    /**
     * Sets the target points. The number of points shall be the same than the number of source points.
     * Target points can have any number of dimensions (not necessarily 2), but all points shall have
     * the same number of dimensions.
     *
     * @param  points The target points.
     * @throws MismatchedDimensionException if not all points have the same number of dimensions.
     */
    public void setTargetPoints(final DirectPosition... points) throws MismatchedDimensionException {
        ArgumentChecks.ensureNonNull("points", points);
        if (points.length != 0) {
            targets = toArrays(points, points[0].getDimension());
        } else {
            targets = null;
        }
        correlation = null;
    }

    /**
     * Creates a linear transform from the source and target points.
     *
     * @return The fitted linear transform.
     */
    public LinearTransform create() {
        final double[][] sources = this.sources;  // Protect from changes.
        final double[][] targets = this.targets;
        if (sources == null || targets == null) {
            throw new IllegalStateException(Errors.format(
                    Errors.Keys.MissingValueForProperty_1, (sources == null) ? "sources" : "targets"));
        }
        final int sourceDim = sources.length;
        final int targetDim = targets.length;
        final MatrixSIS matrix = Matrices.createZero(targetDim + 1, sourceDim + 1);
        final Plane plan = new Plane();
        correlation = new double[targetDim];
        for (int j=0; j<targets.length; j++) {
            correlation[j] = plan.fit(sources[0], sources[1], targets[j]);
            matrix.setElement(j, 0, plan.cx);
            matrix.setElement(j, 1, plan.cy);
            matrix.setElement(j, 2, plan.c);
        }
        matrix.setElement(targetDim, sourceDim, 1);
        return MathTransforms.linear(matrix);
    }
}