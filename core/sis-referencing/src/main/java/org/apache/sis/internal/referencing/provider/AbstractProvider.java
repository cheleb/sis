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
package org.apache.sis.internal.referencing.provider;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import org.opengis.util.GenericName;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.util.ArgumentChecks;


/**
 * Base class for all providers defined in this package.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
abstract class AbstractProvider extends DefaultOperationMethod implements MathTransformProvider {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2239172887926695217L;

    /**
     * Constructs a math transform provider from the given properties and a set of parameters.
     *
     * @param properties      Set of properties. Shall contain at least {@code "name"}.
     * @param sourceDimension Number of dimensions in the source CRS of this operation method.
     * @param targetDimension Number of dimensions in the target CRS of this operation method.
     * @param parameters      The set of parameters (never {@code null}).
     */
    AbstractProvider(final Map<String,?> properties,
                     final int sourceDimension,
                     final int targetDimension,
                     final ParameterDescriptorGroup parameters)
    {
        super(properties, sourceDimension, targetDimension, parameters);
    }

    /**
     * Constructs a math transform provider from a set of parameters. The provider name and
     * {@linkplain #getIdentifiers() identifiers} will be the same than the parameter ones.
     *
     * @param sourceDimension Number of dimensions in the source CRS of this operation method.
     * @param targetDimension Number of dimensions in the target CRS of this operation method.
     * @param parameters      The set of parameters (never {@code null}).
     */
    AbstractProvider(final int sourceDimension,
                     final int targetDimension,
                     final ParameterDescriptorGroup parameters)
    {
        super(toMap(parameters), sourceDimension, targetDimension, parameters);
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    private static Map<String,Object> toMap(final IdentifiedObject parameters) {
        ArgumentChecks.ensureNonNull("parameters", parameters);
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(NAME_KEY, parameters.getName());
        final Collection<Identifier> identifiers = parameters.getIdentifiers();
        int size = identifiers.size();
        if (size != 0) {
            properties.put(IDENTIFIERS_KEY, identifiers.toArray(new Identifier[size]));
        }
        final Collection<GenericName> aliases = parameters.getAlias();
        size = aliases.size();
        if (size != 0) {
            properties.put(ALIAS_KEY, aliases.toArray(new GenericName[size]));
        }
        return properties;
    }
}
