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
package org.apache.sis.internal.converter;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.test.PlatformDependentTest;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the various {@link URLConverter} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.01)
 * @version 0.3
 * @module
 */
public final strictfp class URLConverterTest extends TestCase {
    /**
     * Asserts that conversion of the given {@code source} value produces
     * the given {@code target} value, and tests the inverse conversion.
     */
    private static <T> void runInvertibleConversion(final ObjectConverter<URL,T> c,
            final URL source, final T target) throws UnconvertibleObjectException
    {
        assertEquals("Forward conversion.", target, c.convert(source));
        assertEquals("Inverse conversion.", source, c.inverse().convert(target));
        assertSame("Inconsistent inverse.", c, c.inverse().inverse());
        assertTrue("Invertible converters shall declare this capability.",
                c.properties().contains(FunctionProperty.INVERTIBLE));
    }

    /**
     * Tests conversions to string values.
     *
     * @throws MalformedURLException Should never happen.
     */
    @Test
    public void testString() throws MalformedURLException {
        final ObjectConverter<URL,String> c = ObjectToString.URL;
        runInvertibleConversion(c, new URL("file:/home/user/index.txt"), "file:/home/user/index.txt");
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to URI values.
     *
     * @throws MalformedURLException Should never happen.
     * @throws URISyntaxException Should never happen.
     */
    @Test
    public void testURI() throws MalformedURLException, URISyntaxException {
        final ObjectConverter<URL,URI> c = URLConverter.URI.INSTANCE;
        runInvertibleConversion(c, new URL("file:/home/user/index.txt"), new URI("file:/home/user/index.txt"));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to File values.
     *
     * @throws MalformedURLException Should never happen.
     */
    @Test
    @PlatformDependentTest
    public void testFile() throws MalformedURLException {
        FileConverterTest.assumeUnixRoot();
        final ObjectConverter<URL,File> c = URLConverter.File.INSTANCE;
        runInvertibleConversion(c, new URL("file:/home/user/index.txt"), new File("/home/user/index.txt"));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }
}