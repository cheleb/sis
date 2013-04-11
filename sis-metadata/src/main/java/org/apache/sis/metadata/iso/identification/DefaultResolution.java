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
package org.apache.sis.metadata.iso.identification;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.metadata.identification.RepresentativeFraction;
import org.opengis.metadata.identification.Resolution;
// import org.apache.sis.internal.jaxb.gco.GO_Distance; // TODO
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.measure.ValueRange;


/**
 * Level of detail expressed as a scale factor or a ground distance.
 *
 * {@section Relationship between properties}
 * ISO 19115 defines {@code Resolution} as an <cite>union</cite> (in the C/C++ sense):
 * only one of the properties in this class can be set to a non-empty value.
 * Setting any property to a non-empty value discard all the other ones.
 * See the {@linkplain #DefaultResolution(Resolution) constructor javadoc}
 * for information about which property has precedence on copy operations.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_Resolution_Type", propOrder = {
    "equivalentScale",
    "distance"
})
@XmlRootElement(name = "MD_Resolution")
public class DefaultResolution extends ISOMetadata implements Resolution {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 3856547985745400172L;

    /**
     * Either the scale as a {@link RepresentativeFraction} instance or the distance
     * as a {@code Double} instance.
     */
    private Object scaleOrDistance;

    /**
     * Constructs an initially empty resolution.
     */
    public DefaultResolution() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * <p>If both {@linkplain #getEquivalentScale() scale} and {@linkplain #getDistance() distance}
     * are specified, then the scale will have precedence and the distance is silently discarded.</p>
     *
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(Resolution)
     */
    public DefaultResolution(final Resolution object) {
        super(object);
        scaleOrDistance = object.getEquivalentScale();
        if (scaleOrDistance == null) {
            scaleOrDistance = object.getDistance();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultResolution}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultResolution} instance is created using the
     *       {@linkplain #DefaultResolution(Resolution) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultResolution castOrCopy(final Resolution object) {
        if (object == null || object instanceof DefaultResolution) {
            return (DefaultResolution) object;
        }
        return new DefaultResolution(object);
    }

    /**
     * Invoked every time the code needs to decide whether the provided information
     * is scale or distance. Defined as a method in order to have a single word to
     * search if we need to revisit the policy.
     */
    private boolean isDistance() {
        return (scaleOrDistance instanceof Double);
    }

    /**
     * Returns the level of detail expressed as the scale of a comparable hardcopy map or chart.
     * Only one of {@linkplain #getEquivalentScale() equivalent scale} and
     * {@linkplain #getDistance() ground sample distance} may be provided.
     */
    @Override
    @XmlElement(name = "equivalentScale")
    public synchronized RepresentativeFraction getEquivalentScale()  {
        return isDistance() ? null : (RepresentativeFraction) scaleOrDistance;
    }

    /**
     * Sets the level of detail expressed as the scale of a comparable hardcopy map or chart.
     *
     * {@section Effect on other properties}
     * If and only if the {@code newValue} is non-null, then this method automatically
     * discards the {@linkplain #setDistance distance}.
     *
     * @param newValue The new equivalent scale.
     */
    public synchronized void setEquivalentScale(final RepresentativeFraction newValue) {
        checkWritePermission();
        if (newValue != null || !isDistance()) {
            scaleOrDistance = newValue;
        }
    }

    /**
     * Returns the ground sample distance.
     * Only one of {@linkplain #getEquivalentScale equivalent scale} and
     * {@linkplain #getDistance ground sample distance} may be provided.
     */
    @Override
    @ValueRange(minimum=0, isMinIncluded=false)
//    @XmlJavaTypeAdapter(GO_Distance.class) // TODO
    @XmlElement(name = "distance")
    public synchronized Double getDistance() {
        return isDistance() ? (Double) scaleOrDistance : null;
    }

    /**
     * Sets the ground sample distance.
     *
     * @param newValue The new distance.
     */
    public synchronized void setDistance(final Double newValue) {
        checkWritePermission();
        if (newValue != null || isDistance()) {
            scaleOrDistance = newValue;
        }
    }
}