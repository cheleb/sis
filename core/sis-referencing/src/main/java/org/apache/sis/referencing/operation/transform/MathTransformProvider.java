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

import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.InvalidParameterNameException;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.operation.MathTransform;


/**
 * An object capable to create {@link MathTransform} instances from given parameter values.
 * This interface is the Apache SIS mechanism by which
 * {@linkplain org.apache.sis.referencing.operation.DefaultFormula formula} are concretized as Java code.
 *
 * <p>Implementations of this interface usually extend {@link org.apache.sis.referencing.operation.DefaultOperationMethod},
 * but this is not mandatory. This interface can also be used alone since {@link MathTransform} instances can be created
 * for other purpose than coordinate operations.</p>
 *
 * <p>This interface is generally not used directly. The recommended way to get a {@code MathTransform}
 * is to {@linkplain org.apache.sis.referencing.CRS#findOperation find the coordinate operation}
 * (generally from a pair of <var>source</var> and <var>target</var> CRS), then to invoke
 * {@link org.opengis.referencing.operation.CoordinateOperation#getMathTransform()}.
 * Alternative, one can also use a {@linkplain DefaultMathTransformFactory math transform factory}</p>
 *
 *
 * {@section How to add custom coordinate operations to Apache SIS}
 * {@link DefaultMathTransformFactory} can discover automatically new coordinate operations
 * (including map projections) by scanning the classpath. To define a custom coordinate operation,
 * one needs to define a <strong>thread-safe</strong> class implementing <strong>both</strong> this
 * {@code MathTransformProvider} interface and the {@link org.opengis.referencing.operation.OperationMethod} one.
 * While not mandatory, we suggest to extend {@link org.apache.sis.referencing.operation.DefaultOperationMethod}.
 * Example:
 *
 * <div class="note">{@preformat java
 *     public class MyProjectionProvider extends DefaultOperationMethod implements MathTransformProvider {
 *         public MyProjectionProvider() {
 *             super(Collections.singletonMap(NAME_KEY, "My projection"),
 *                     2, // Number of source dimensions
 *                     2, // Number of target dimensions
 *                     parameters);
 *         }
 *
 *         &#64;Override
 *         public MathTransform createMathTransform(ParameterValueGroup values) {
 *             double semiMajor = values.parameter("semi_major").doubleValue(SI.METRE);
 *             double semiMinor = values.parameter("semi_minor").doubleValue(SI.METRE);
 *             // etc...
 *             return new MyProjection(semiMajor, semiMinor, ...);
 *         }
 *     }
 * }</div>
 *
 * Then the fully-qualified class name of that implementation should be listed in a file reachable on the classpath
 * with this exact name:
 *
 * {@preformat text
 *     META-INF/services/org.opengis.referencing.operation.OperationMethod
 * }
 *
 * @author  Martin Desruisseaux (Geomatys, IRD)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see org.apache.sis.referencing.operation.DefaultOperationMethod
 * @see DefaultMathTransformFactory
 * @see AbstractMathTransform
 */
public interface MathTransformProvider {
    /**
     * Creates a math transform from the specified group of parameter values.
     *
     * <div class="note"><b>Implementation example:</b>
     * The following example shows how parameter values can be extracted
     * before to instantiate the transform:
     *
     * {@preformat java
     *     public MathTransform createMathTransform(ParameterValueGroup values) {
     *         double semiMajor = values.parameter("semi_major").doubleValue(SI.METRE);
     *         double semiMinor = values.parameter("semi_minor").doubleValue(SI.METRE);
     *         // etc...
     *         return new MyProjection(semiMajor, semiMinor, ...);
     *     }
     * }
     * </div>
     *
     * @param  values The group of parameter values.
     * @return The created math transform.
     * @throws InvalidParameterNameException if the values contains an unknown parameter.
     * @throws ParameterNotFoundException if a required parameter was not found.
     * @throws InvalidParameterValueException if a parameter has an invalid value.
     * @throws FactoryException if the math transform can not be created for some other reason
     *         (for example a required file was not found).
     */
    MathTransform createMathTransform(ParameterValueGroup values)
            throws InvalidParameterNameException, ParameterNotFoundException,
                   InvalidParameterValueException, FactoryException;
}
