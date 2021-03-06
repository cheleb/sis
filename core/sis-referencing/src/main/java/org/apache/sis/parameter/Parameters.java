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
package org.apache.sis.parameter;

import java.util.Map;
import java.util.HashMap;
import javax.measure.unit.Unit;
import org.opengis.util.MemberName;
import org.opengis.parameter.*; // We use almost all types from this package.
import org.apache.sis.internal.jaxb.metadata.replace.ServiceParameter;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Static;


/**
 * Static methods working on parameters and their descriptors.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.5
 * @module
 */
public final class Parameters extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private Parameters() {
    }

    /**
     * Gets the parameter name as an instance of {@code MemberName}.
     * This method performs the following checks:
     *
     * <ul>
     *   <li>If the {@linkplain DefaultParameterDescriptor#getName() primary name} is an instance of {@code MemberName},
     *       returns that primary name.</li>
     *   <li>Otherwise this method searches for the first {@linkplain DefaultParameterDescriptor#getAlias() alias}
     *       which is an instance of {@code MemberName}. If found, that alias is returned.</li>
     *   <li>If no alias is found, then this method tries to build a member name from the primary name and the
     *       {@linkplain DefaultParameterDescriptor#getValueClass() value class}, using the mapping defined in
     *       {@link org.apache.sis.util.iso.DefaultTypeName} javadoc.</li>
     * </ul>
     *
     * This method can be used as a bridge between the parameter object
     * defined by ISO 19111 (namely {@code CC_OperationParameter}) and the one
     * defined by ISO 19115 (namely {@code SV_Parameter}).
     *
     * @param  parameter The parameter from which to get the name (may be {@code null}).
     * @return The member name, or {@code null} if none.
     *
     * @see org.apache.sis.util.iso.Names#createMemberName(CharSequence, String, CharSequence, Class)
     *
     * @since 0.5
     */
    public static MemberName getMemberName(final ParameterDescriptor<?> parameter) {
        return ServiceParameter.getMemberName(parameter);
    }

    /**
     * Returns the domain of valid values defined by the given descriptor, or {@code null} if none.
     * This method builds the range from the {@linkplain DefaultParameterDescriptor#getMinimumValue() minimum value},
     * {@linkplain DefaultParameterDescriptor#getMaximumValue() maximum value} and, if the values are numeric, from
     * the {@linkplain DefaultParameterDescriptor#getUnit() unit}.
     *
     * @param  descriptor The parameter descriptor, or {@code null}.
     * @return The domain of valid values, or {@code null} if none.
     *
     * @see DefaultParameterDescriptor#getValueDomain()
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Range<?> getValueDomain(final ParameterDescriptor<?> descriptor) {
        if (descriptor != null) {
            if (descriptor instanceof DefaultParameterDescriptor<?>) {
                return ((DefaultParameterDescriptor<?>) descriptor).getValueDomain();
            }
            final Class<?> valueClass = descriptor.getValueClass();
            final Comparable<?> minimumValue = descriptor.getMinimumValue();
            final Comparable<?> maximumValue = descriptor.getMaximumValue();
            if ((minimumValue == null || valueClass.isInstance(minimumValue)) &&
                (maximumValue == null || valueClass.isInstance(maximumValue)))
            {
                if (Number.class.isAssignableFrom(valueClass)) {
                    final Unit<?> unit = descriptor.getUnit();
                    if (unit != null) {
                        return new MeasurementRange((Class) valueClass,
                                (Number) minimumValue, true, (Number) maximumValue, true, unit);
                    } else if (minimumValue != null || maximumValue != null) {
                        return new NumberRange((Class) valueClass,
                                (Number) minimumValue, true, (Number) maximumValue, true);
                    }
                } else if (minimumValue != null || maximumValue != null) {
                    return new Range(valueClass, minimumValue, true, maximumValue, true);
                }
            }
        }
        return null;
    }

    /**
     * Casts the given parameter descriptor to the given type.
     * An exception is thrown immediately if the parameter does not have the expected
     * {@linkplain DefaultParameterDescriptor#getValueClass() value class}.
     *
     * @param  <T>        The expected value class.
     * @param  descriptor The descriptor to cast, or {@code null}.
     * @param  valueClass The expected value class.
     * @return The descriptor casted to the given value class, or {@code null} if the given descriptor was null.
     * @throws ClassCastException if the given descriptor does not have the expected value class.
     *
     * @category verification
     */
    @SuppressWarnings("unchecked")
    public static <T> ParameterDescriptor<T> cast(final ParameterDescriptor<?> descriptor, final Class<T> valueClass)
            throws ClassCastException
    {
        if (descriptor != null) {
            final Class<?> actual = descriptor.getValueClass();
            // We require a strict equality - not type.isAssignableFrom(actual) - because in
            // the later case we could have (to be strict) to return a <? extends T> type.
            if (!valueClass.equals(actual)) {
                throw new ClassCastException(Errors.format(Errors.Keys.IllegalParameterType_2,
                        descriptor.getName().getCode(), actual));
            }
        }
        return (ParameterDescriptor<T>) descriptor;
    }

    /**
     * Casts the given parameter value to the given type.
     * An exception is thrown immediately if the parameter does not have the expected value class.
     *
     * @param  <T>   The expected value class.
     * @param  value The value to cast, or {@code null}.
     * @param  type  The expected value class.
     * @return The value casted to the given type, or {@code null} if the given value was null.
     * @throws ClassCastException if the given value doesn't have the expected value class.
     *
     * @category verification
     */
    @SuppressWarnings("unchecked")
    public static <T> ParameterValue<T> cast(final ParameterValue<?> value, final Class<T> type)
            throws ClassCastException
    {
        if (value != null) {
            final ParameterDescriptor<?> descriptor = value.getDescriptor();
            final Class<?> actual = descriptor.getValueClass();
            if (!type.equals(actual)) { // Same comment than cast(ParameterDescriptor)...
                throw new ClassCastException(Errors.format(Errors.Keys.IllegalParameterType_2,
                        descriptor.getName().getCode(), actual));
            }
        }
        return (ParameterValue<T>) value;
    }

    /**
     * Copies the values of a parameter group into another parameter group.
     * All values in the {@code source} group shall be valid for the {@code destination} group,
     * but the {@code destination} may have more parameters.
     * Sub-groups are copied recursively.
     *
     * <p>A typical usage of this method is for transferring values from an arbitrary implementation
     * to some specific implementation, or to a parameter group using a different but compatible
     * {@linkplain DefaultParameterValueGroup#getDescriptor() descriptor}.</p>
     *
     * @param  values The parameters values to copy.
     * @param  destination Where to copy the values.
     * @throws InvalidParameterNameException if a {@code source} parameter name is unknown to the {@code destination}.
     * @throws InvalidParameterValueException if the value of a {@code source} parameter is invalid for the {@code destination}.
     *
     * @since 0.5
     */
    public static void copy(final ParameterValueGroup values, final ParameterValueGroup destination)
            throws InvalidParameterNameException, InvalidParameterValueException
    {
        final Integer ONE = 1;
        final Map<String,Integer> occurrences = new HashMap<>();
        for (final GeneralParameterValue value : values.values()) {
            final String name = value.getDescriptor().getName().getCode();
            if (value instanceof ParameterValueGroup) {
                /*
                 * Contains sub-group - invokes 'copy' recursively.
                 */
                final GeneralParameterDescriptor descriptor;
                descriptor = destination.getDescriptor().descriptor(name);
                if (descriptor instanceof ParameterDescriptorGroup) {
                    final ParameterValueGroup groups = (ParameterValueGroup) descriptor.createValue();
                    copy((ParameterValueGroup) value, groups);
                    values.groups(name).add(groups);
                } else {
                    throw new InvalidParameterNameException(Errors.format(
                            Errors.Keys.UnexpectedParameter_1, name), name);
                }
            } else {
                /*
                 * Single parameter - copy the value, with special care for value with units
                 * and for multi-occurrences. Not that the later is not allowed by ISO 19111
                 * but supported by SIS implementation.
                 */
                final ParameterValue<?> source = (ParameterValue<?>) value;
                final ParameterValue<?> target;
                Integer occurrence = occurrences.get(name);
                if (occurrence == null) {
                    occurrence = ONE;
                    try {
                        target = destination.parameter(name);
                    } catch (ParameterNotFoundException cause) {
                        throw new InvalidParameterNameException(Errors.format(
                                    Errors.Keys.UnexpectedParameter_1, name), cause, name);
                    }
                } else {
                    target = (ParameterValue<?>) getOrCreate(destination, name, occurrence);
                    occurrence++;
                }
                occurrences.put(name, occurrence);
                final Object  v    = source.getValue();
                final Unit<?> unit = source.getUnit();
                if (unit == null) {
                    target.setValue(v);
                } else if (v instanceof Number) {
                    target.setValue(((Number) v).doubleValue(), unit);
                } else if (v instanceof double[]) {
                    target.setValue((double[]) v, unit);
                } else {
                    throw new InvalidParameterValueException(Errors.format(
                            Errors.Keys.IllegalArgumentValue_2, name, v), name, v);
                }
            }
        }
    }

    /**
     * Returns the <var>n</var>th occurrence of the parameter of the given name.
     * This method is not public because ISO 19111 does not allow multi-occurrences of parameter values
     * (this is a SIS-specific flexibility). Current implementation is not very efficient, but it should
     * not be an issue if this method is rarely invoked.
     *
     * @param  values The group from which to get or create a value
     * @param  name   The name of the parameter to fetch. An exact match will be required.
     * @param  n      Number of occurrences to skip before to return or create the parameter.
     * @return The <var>n</var>th occurrence (zero-based) of the parameter of the given name.
     * @throws IndexOutOfBoundsException if {@code n} is greater than the current number of
     *         parameters of the given name.
     */
    private static GeneralParameterValue getOrCreate(final ParameterValueGroup values, final String name, int n) {
        for (final GeneralParameterValue value : values.values()) {
            if (name.equals(value.getDescriptor().getName().getCode())) {
                if (--n < 0) {
                    return value;
                }
            }
        }
        if (n == 0) {
            final GeneralParameterValue value = values.getDescriptor().descriptor(name).createValue();
            values.values().add(value);
            return value;
        } else {
            // We do not botter formatting a good error message for now, because
            // this method is currently invoked only with increasing index values.
            throw new IndexOutOfBoundsException(name);
        }
    }
}
