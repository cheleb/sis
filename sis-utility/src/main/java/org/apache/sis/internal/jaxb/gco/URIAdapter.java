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
package org.apache.sis.internal.jaxb.gco;

import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.sis.internal.jaxb.MarshalContext;


/**
 * JAXB adapter wrapping a URI value with a {@code <gco:CharacterString>} element,
 * for ISO-19139 compliance.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
 * @module
 */
public final class URIAdapter extends XmlAdapter<GO_CharacterString, URI> {
    /**
     * Empty constructor for JAXB.
     */
    private URIAdapter() {
    }

    /**
     * Converts a URI read from a XML stream to the object containing the value.
     * JAXB calls automatically this method at unmarshalling time.
     *
     * @param  value The wrapper for the URI value, or {@code null}.
     * @return An {@link URI} which represents the URI value, or {@code null}.
     * @throws URISyntaxException If the string is not a valid URI.
     */
    @Override
    public URI unmarshal(final GO_CharacterString value) throws URISyntaxException {
        final String text = StringAdapter.toString(value);
        if (text != null) {
            final MarshalContext context = MarshalContext.current();
            return MarshalContext.converter(context).toURI(context, text);
        }
        return null;
    }

    /**
     * Converts a {@link URI} to the object to be marshalled in a XML file or stream.
     * JAXB calls automatically this method at marshalling time.
     *
     * @param  value The URI value, or {@code null}.
     * @return The wrapper for the given URI, or {@code null}.
     */
    @Override
    public GO_CharacterString marshal(final URI value) {
        return (value != null) ? CharSequenceAdapter.wrap(value, value.toString()) : null;
    }
}