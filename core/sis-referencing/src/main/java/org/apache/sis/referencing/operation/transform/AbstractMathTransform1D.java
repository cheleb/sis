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
package org.apache.sis.referencing.operation.transform;

import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrix1;

import static org.apache.sis.util.ArgumentChecks.ensureDimensionMatches;


/**
 * Base class for math transforms that are known to be one-dimensional in all cases.
 * One-dimensional math transforms are not required to extend this class,
 * however doing so may simplify their implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5 (derived from geotk-3.17)
 * @version 0.5
 * @module
 */
public abstract class AbstractMathTransform1D extends AbstractMathTransform implements MathTransform1D {
    /**
     * Constructs a default math transform.
     */
    protected AbstractMathTransform1D() {
    }

    /**
     * Returns the dimension of input points, which is always 1.
     */
    @Override
    public final int getSourceDimensions() {
        return 1;
    }

    /**
     * Returns the dimension of output points, which is always 1.
     */
    @Override
    public final int getTargetDimensions() {
        return 1;
    }

    /**
     * Transforms a single point in the given array and opportunistically computes its derivative if requested.
     * The default implementation delegates to {@link #transform(double)} and potentially to {@link #derivative(double)}.
     * Subclasses may override this method for performance reason.
     *
     * @return {@inheritDoc}
     * @throws TransformException {@inheritDoc}
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws TransformException
    {
        final double ordinate = srcPts[srcOff];
        if (dstPts != null) {
            dstPts[dstOff] = transform(ordinate);
        }
        return derivate ? new Matrix1(derivative(ordinate)) : null;
    }

    /**
     * Gets the derivative of this transform at a point. The default implementation ensures that
     * {@code point} is one-dimensional, then delegates to {@link #derivative(double)}.
     *
     * @param  point The coordinate point where to evaluate the derivative, or {@code null}.
     * @return The derivative at the specified point (never {@code null}).
     * @throws MismatchedDimensionException if {@code point} does not have the expected dimension.
     * @throws TransformException if the derivative can not be evaluated at the specified point.
     */
    @Override
    public Matrix derivative(final DirectPosition point) throws TransformException {
        final double ordinate;
        if (point == null) {
            ordinate = Double.NaN;
        } else {
            ensureDimensionMatches("point", 1, point);
            ordinate = point.getOrdinate(0);
        }
        return new Matrix1(derivative(ordinate));
    }

    /**
     * Returns the inverse transform of this object.
     * The default implementation returns {@code this} if this transform is an identity transform,
     * or throws an exception otherwise. Subclasses should override this method.
     */
    @Override
    public MathTransform1D inverse() throws NoninvertibleTransformException {
        return (MathTransform1D) super.inverse();
    }
}
