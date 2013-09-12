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
package org.apache.sis.referencing.operation.matrix;

import java.util.Arrays;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.math.MathFunctions;


/**
 * A matrix of fixed {@value #SIZE}×{@value #SIZE} size.
 * The matrix members are:
 *
 * <blockquote><pre> ┌             ┐
 * │ {@link #m00} {@link #m01} {@link #m02} │
 * │ {@link #m10} {@link #m11} {@link #m12} │
 * │ {@link #m20} {@link #m21} {@link #m22} │
 * └             ┘</pre></blockquote>
 *
 * In some situations, this specialized matrix implementation may provide better accuracy than the
 * generic implementation for matrix inversions and multiplications.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
 *
 * @see Matrix1
 * @see Matrix2
 * @see Matrix4
 */
public final class Matrix3 extends MatrixSIS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8902061778871586611L;

    /**
     * The matrix size, which is {@value}.
     */
    public static final int SIZE = 3;

    /** The first matrix element in the first row.   */ public double m00;
    /** The second matrix element in the first row.  */ public double m01;
    /** The third matrix element in the first row.   */ public double m02;
    /** The first matrix element in the second row.  */ public double m10;
    /** The second matrix element in the second row. */ public double m11;
    /** The third matrix element in the second row.  */ public double m12;
    /** The first matrix element in the third row.   */ public double m20;
    /** The second matrix element in the third row.  */ public double m21;
    /** The third matrix element in the third row.   */ public double m22;

    /**
     * Creates a new identity matrix.
     */
    public Matrix3() {
        m00 = m11 = m22 = 1;
    }

    /**
     * Creates a new matrix filled with only zero values.
     *
     * @param ignore Shall always be {@code false} in current version.
     */
    Matrix3(final boolean ignore) {
    }

    /**
     * Creates a new matrix initialized to the specified values.
     *
     * @param m00 The first matrix element in the first row.
     * @param m01 The second matrix element in the first row.
     * @param m02 The third matrix element in the first row.
     * @param m10 The first matrix element in the second row.
     * @param m11 The second matrix element in the second row.
     * @param m12 The third matrix element in the second row.
     * @param m20 The first matrix element in the third row.
     * @param m21 The second matrix element in the third row.
     * @param m22 The third matrix element in the third row.
     */
    public Matrix3(double m00, double m01, double m02,
                   double m10, double m11, double m12,
                   double m20, double m21, double m22)
    {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
    }

    /**
     * Creates a new matrix initialized to the specified values.
     * The length of the given array must be 9 and the values in the same order than the above constructor.
     *
     * @param elements Elements of the matrix. Column indices vary fastest.
     * @throws IllegalArgumentException If the given array does not have the expected length.
     */
    public Matrix3(final double[] elements) throws IllegalArgumentException {
        setElements(elements);
    }

    /**
     * Creates a new matrix initialized to the same value than the specified one.
     * The specified matrix size must be {@value #SIZE}×{@value #SIZE}.
     * This is not verified by this constructor, since it shall be verified by {@link Matrices}.
     *
     * @param matrix The matrix to copy.
     */
    Matrix3(final Matrix matrix) {
        for (int j=0; j<SIZE; j++) {
            for (int i=0; i<SIZE; i++) {
                setElement(j,i, matrix.getElement(j,i));
            }
        }
    }

    /*
     * The 'final' modifier in following method declarations is redundant with the 'final' modifier
     * in this class declaration, but we keep them as a reminder of which methods should stay final
     * if this class was modified to a non-final class. Some methods should stay final because:
     *
     *  - returning a different value would make no-sense for this class (e.g. 'getNumRow()');
     *  - they are invoked by a constructor or by an other method expecting this exact semantic.
     */

    /**
     * Returns the number of rows in this matrix, which is always {@value #SIZE} in this implementation.
     *
     * @return Always {@value SIZE}.
     */
    @Override
    public final int getNumRow() {
        return SIZE;
    }

    /**
     * Returns the number of columns in this matrix, which is always {@value #SIZE} in this implementation.
     *
     * @return Always {@value SIZE}.
     */
    @Override
    public final int getNumCol() {
        return SIZE;
    }

    /**
     * Retrieves the value at the specified row and column of this matrix.
     * This method can be invoked when the matrix size or type is unknown.
     * If the matrix is known to be an instance of {@code Matrix3},
     * then the {@link #m00} … {@link #m22} fields can be read directly for efficiency.
     *
     * @param row    The row index, from 0 inclusive to {@value #SIZE} exclusive.
     * @param column The column index, from 0 inclusive to {@value #SIZE} exclusive.
     * @return       The current value at the given row and column.
     */
    @Override
    public final double getElement(final int row, final int column) {
        if (row >= 0 && row < SIZE && column >= 0 && column < SIZE) {
            switch (row*SIZE + column) {
                case 0: return m00;
                case 1: return m01;
                case 2: return m02;
                case 3: return m10;
                case 4: return m11;
                case 5: return m12;
                case 6: return m20;
                case 7: return m21;
                case 8: return m22;
            }
        }
        throw indexOutOfBounds(row, column);
    }

    /**
     * Modifies the value at the specified row and column of this matrix.
     * This method can be invoked when the matrix size or type is unknown.
     * If the matrix is known to be an instance of {@code Matrix3},
     * then the {@link #m00} … {@link #m22} fields can be set directly for efficiency.
     *
     * @param row    The row index, from 0 inclusive to {@value #SIZE} exclusive.
     * @param column The column index, from 0 inclusive to {@value #SIZE} exclusive.
     * @param value  The new value to set at the given row and column.
     */
    @Override
    public final void setElement(final int row, final int column, final double value) {
        if (row >= 0 && row < SIZE && column >= 0 && column < SIZE) {
            switch (row*SIZE + column) {
                case 0: m00 = value; return;
                case 1: m01 = value; return;
                case 2: m02 = value; return;
                case 3: m10 = value; return;
                case 4: m11 = value; return;
                case 5: m12 = value; return;
                case 6: m20 = value; return;
                case 7: m21 = value; return;
                case 8: m22 = value; return;
            }
        }
        throw indexOutOfBounds(row, column);
    }

    /**
     * Returns all matrix elements in a flat, row-major (column indices vary fastest) array.
     * The array length is 9.
     */
    @Override
    public final double[] getElements() {
        return new double[] {
            m00, m01, m02,
            m10, m11, m12,
            m20, m21, m22
        };
    }

    /**
     * Sets all matrix elements from a flat, row-major (column indices vary fastest) array.
     * The array length shall be 9.
     */
    @Override
    public final void setElements(final double[] elements) {
        ensureLengthMatch(SIZE*SIZE, elements);
        m00 = elements[0];
        m01 = elements[1];
        m02 = elements[2];
        m10 = elements[3];
        m11 = elements[4];
        m12 = elements[5];
        m20 = elements[6];
        m21 = elements[7];
        m22 = elements[8];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isAffine() {
        return m20 == 0 && m21 == 0 && m22 == 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isIdentity() {
        return m00 == 1 && m01 == 0 && m02 == 0 &&
               m10 == 0 && m11 == 1 && m12 == 0 &&
               isAffine();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void transpose() {
        double swap;
        swap = m01; m01 = m10; m10 = swap;
        swap = m02; m02 = m20; m20 = swap;
        swap = m12; m12 = m21; m21 = swap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void normalizeColumns() {
        double m;
        final double[] v = new double[3];
        v[0]=m00; v[1]=m10; v[2]=m20; m = MathFunctions.magnitude(v); m00 /= m; m10 /= m; m20 /= m;
        v[0]=m01; v[1]=m11; v[2]=m21; m = MathFunctions.magnitude(v); m01 /= m; m11 /= m; m21 /= m;
        v[0]=m02; v[1]=m12; v[2]=m22; m = MathFunctions.magnitude(v); m02 /= m; m12 /= m; m22 /= m;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MatrixSIS inverse() throws SingularMatrixException {
        throw new UnsupportedOperationException(); // TODO
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MatrixSIS multiply(final Matrix matrix) {
        final int nc = matrix.getNumCol();
        ensureNumRowMatch(SIZE, matrix, nc);
        if (nc != SIZE) {
            return new NonSquareMatrix(this, matrix);
        }
        final Matrix3 k = (matrix instanceof Matrix3) ? (Matrix3) matrix : new Matrix3(matrix);
        return new Matrix3(m00 * k.m00  +  m01 * k.m10  +  m02 * k.m20,
                           m00 * k.m01  +  m01 * k.m11  +  m02 * k.m21,
                           m00 * k.m02  +  m01 * k.m12  +  m02 * k.m22,
                           m10 * k.m00  +  m11 * k.m10  +  m12 * k.m20,
                           m10 * k.m01  +  m11 * k.m11  +  m12 * k.m21,
                           m10 * k.m02  +  m11 * k.m12  +  m12 * k.m22,
                           m20 * k.m00  +  m21 * k.m10  +  m22 * k.m20,
                           m20 * k.m01  +  m21 * k.m11  +  m22 * k.m21,
                           m20 * k.m02  +  m21 * k.m12  +  m22 * k.m22);
    }

    /**
     * Returns {@code true} if the specified object is of type {@code Matrix3} and
     * all of the data members are equal to the corresponding data members in this matrix.
     *
     * @param object The object to compare with this matrix for equality.
     * @return {@code true} if the given object is equal to this matrix.
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof Matrix3) {
            final Matrix3 that = (Matrix3) object;
            return Numerics.equals(this.m00, that.m00) &&
                   Numerics.equals(this.m01, that.m01) &&
                   Numerics.equals(this.m02, that.m02) &&
                   Numerics.equals(this.m10, that.m10) &&
                   Numerics.equals(this.m11, that.m11) &&
                   Numerics.equals(this.m12, that.m12) &&
                   Numerics.equals(this.m20, that.m20) &&
                   Numerics.equals(this.m21, that.m21) &&
                   Numerics.equals(this.m22, that.m22);
        }
        return false;
    }

    /**
     * Returns a hash code value based on the data values in this object.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(getElements()) ^ (int) serialVersionUID;
    }
}