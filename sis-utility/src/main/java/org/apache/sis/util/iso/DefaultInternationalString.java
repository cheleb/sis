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
package org.apache.sis.util.iso;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import net.jcip.annotations.ThreadSafe;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Locales;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.collection.Collections.isNullOrEmpty;

// Related to JDK7
import java.util.Objects;


/**
 * An international string using a {@linkplain Map map} of strings for different locales.
 * Strings for new locales can be {@linkplain #add(Locale, String) added},
 * but existing strings can not be removed or modified.
 * This behavior is a compromise between making constructions easier, and being suitable for
 * use in immutable objects.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@ThreadSafe
public class DefaultInternationalString extends AbstractInternationalString implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5760033376627376938L;

    /**
     * The string values in different locales (never {@code null}).
     */
    private Map<Locale,String> localeMap;

    /**
     * An unmodifiable view of the entry set in {@link #localeMap}. This is the set of locales
     * defined in this international string. Will be constructed only when first requested.
     */
    private transient Set<Locale> localeSet;

    /**
     * Creates an initially empty international string. Localized strings can been added
     * using one of {@link #add add(…)} methods.
     */
    public DefaultInternationalString() {
        localeMap = Collections.emptyMap();
    }

    /**
     * Creates an international string initialized with the given string.
     * Additional localized strings can been added using one of {@link #add add(…)} methods.
     * The string specified to this constructor is the one that will be returned if no localized
     * string is found for the {@code Locale} argument in a call to {@link #toString(Locale)}.
     *
     * @param string The string in no specific locale, or {@code null} if none.
     */
    public DefaultInternationalString(final String string) {
        if (string != null) {
            localeMap = Collections.singletonMap(null, string);
        } else {
            localeMap = Collections.emptyMap();
        }
    }

    /**
     * Creates an international string initialized with the given localized strings.
     * The content of the given map is copied, so changes to that map after construction
     * will not be reflected into this international string.
     *
     * @param strings The strings in various locales, or {@code null} if none.
     */
    public DefaultInternationalString(final Map<Locale,String> strings) {
        if (isNullOrEmpty(strings)) {
            localeMap = Collections.emptyMap();
        } else {
            final Iterator<Map.Entry<Locale,String>> it = strings.entrySet().iterator();
            final Map.Entry<Locale,String> entry = it.next();
            if (!it.hasNext()) {
                localeMap = Collections.singletonMap(entry.getKey(), entry.getValue());
            } else {
                localeMap = new LinkedHashMap<>(strings);
                // If HashMap is replaced by an other type, please revisit 'getLocales()'.
            }
        }
    }

    /**
     * Adds a string for the given locale.
     *
     * @param  locale The locale for the {@code string} value, or {@code null}.
     * @param  string The localized string.
     * @throws IllegalArgumentException if a different string value was already set for
     *         the given locale.
     */
    public synchronized void add(final Locale locale, final String string) throws IllegalArgumentException {
        if (string != null) {
            switch (localeMap.size()) {
                case 0: {
                    localeMap = Collections.singletonMap(locale, string);
                    localeSet = null;
                    defaultValue = null; // Will be recomputed when first needed.
                    return;
                }
                case 1: {
                    // If HashMap is replaced by an other type, please revisit 'getLocales()'.
                    localeMap = new LinkedHashMap<>(localeMap);
                    localeSet = null;
                    break;
                }
            }
            final String old = localeMap.put(locale, string);
            if (old != null) {
                localeMap.put(locale, old);
                if (string.equals(old)) {
                    return;
                }
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.ValueAlreadyDefined_1, locale));
            }
            defaultValue = null; // Will be recomputed when first needed.
        }
    }

    /**
     * Adds a string for the given property key. This is a convenience method for constructing an
     * {@code DefaultInternationalString} during iteration through the entries in a {@link Map}.
     * It infers the {@code Locale} from the property {@code key}, using the following steps:
     *
     * <ul>
     *   <li>If the {@code key} does not start with the specified {@code prefix}, then
     *       this method does nothing and returns {@code false}.</li>
     *   <li>Otherwise, the characters after the {@code prefix} are parsed as an ISO language
     *       and country code, and the {@link #add(Locale, String)} method is invoked.</li>
     * </ul>
     *
     * For example if the prefix is {@code "remarks"}, then the {@code "remarks_fr"} property key
     * stands for remarks in {@linkplain Locale#FRENCH French} while the {@code "remarks_fr_CA"}
     * property key stands for remarks in {@linkplain Locale#CANADA_FRENCH French Canadian}.
     *
     * @param  prefix The prefix to skip at the beginning of the {@code key}.
     * @param  key    The property key which will determined the locale.
     * @param  value  The localized string for the specified {@code key}.
     * @return {@code true} if the key has been recognized, or {@code false} otherwise.
     * @throws IllegalArgumentException if the locale after the prefix is an illegal code,
     *         or a different string value was already set for the given locale.
     */
    public boolean add(final String prefix, final String key, final String value)
            throws IllegalArgumentException
    {
        if (key.startsWith(prefix)) {
            Locale locale = null;
            final int offset = prefix.length();
            if (key.length() != offset) {
                if (key.charAt(offset) == '_') {
                    locale = Locales.parse(key.substring(offset + 1));
                } else {
                    return false;
                }
            }
            add(locale, value);
            return true;
        }
        return false;
    }

    /**
     * Returns the set of locales defined in this international string.
     *
     * @return The set of locales.
     *
     * @todo Current implementation does not return a synchronized set. We should synchronize
     *       on the same lock than the one used for accessing the internal locale map.
     */
    public synchronized Set<Locale> getLocales() {
        Set<Locale> locales = localeSet;
        if (locales == null) {
            locales = localeMap.keySet();
            if (localeMap instanceof HashMap<?,?>) {
                locales = Collections.unmodifiableSet(locales);
            }
            localeSet = locales;
        }
        return locales;
    }

    /**
     * Returns a string in the specified locale. If there is no string for that {@code locale},
     * then this method search for a locale without the {@linkplain Locale#getVariant() variant}
     * part. If no string are found, then this method search for a locale without the
     * {@linkplain Locale#getCountry() country} part. If none are found, then this method returns
     * {@code null}.
     *
     * @param  locale The locale to look for, or {@code null}.
     * @return The string in the specified locale, or {@code null} if none was found.
     */
    private String getString(Locale locale) {
        while (locale != null) {
            final String text = localeMap.get(locale);
            if (text != null) {
                return text;
            }
            final String language = locale.getLanguage();
            final String country  = locale.getCountry ();
            final String variant  = locale.getVariant ();
            if (!variant.isEmpty()) {
                locale = new Locale(language, country);
                continue;
            }
            if (!country.isEmpty()) {
                locale = new Locale(language);
                continue;
            }
            break;
        }
        return null;
    }

    /**
     * Returns a string in the specified locale. If there is no string for that {@code locale},
     * then this method searches for a locale without the {@linkplain Locale#getVariant() variant}
     * part. If no string are found, then this method searches for a locale without the
     * {@linkplain Locale#getCountry() country} part. For example if the {@code "fr_CA"} locale
     * was requested but not found, then this method looks for the {@code "fr"} locale.
     * The {@code null} locale (which stand for unlocalized message) is tried last.
     *
     * {@section Handling of <code>null</code> argument value}
     * A {@code null} argument value can be given to this method for requesting a "unlocalized" string,
     * typically some programmatic strings like {@linkplain org.opengis.annotation.UML#identifier() UML
     * identifiers}. While such identifiers often look like English words, the {@code null} locale is
     * not considered synonymous to the {@linkplain Locale#ENGLISH English locale} since the values may
     * differ in the way numbers and dates are formatted (e.g. using the ISO 8601 standard for dates
     * instead than English conventions). In order to produce a value close to the common practice,
     * this method handles {@code null} argument value as below:
     *
     * <ul>
     *   <li>If a string has been explicitly {@linkplain #add(Locale, String) added} for the
     *       {@code null} locale, then that string is returned.</li>
     *   <li>Otherwise, acknowledging that UML identifiers in OGC/ISO specifications are primarily
     *       expressed in the English language, this method looks for an English string as an
     *       approximation of a "unlocalized" string.</li>
     *   <li>If no English string was found, this method looks for a string for the
     *       {@linkplain Locale#getDefault() system default locale}.</li>
     *   <li>If none of the above steps found a string, then this method returns
     *       an arbitrary string.</li>
     * </ul>
     *
     * @param  locale The locale to look for, or {@code null}.
     * @return The string in the specified locale, or in a default locale.
     */
    @Override
    public synchronized String toString(final Locale locale) {
        String text = getString(locale);
        if (text == null) {
            /*
             * No string for the requested locale. Try the string in the 'null' locale first, then
             * the string in the system-default last.  Note: in a previous version we were looking
             * for the system default first, but it produced unexpected results in many cases. The
             * i18n string is often constructed with an English sentence for the "null" locale (the
             * unlocalized text) without explicit entry for the English locale since the "null" one
             * is supposed to be the default according javadoc. If we were looking for the default
             * locale on a system having French as the default, the effect would be to return a
             * sentence in French when the user asked for a sentence in English (or any language
             * not explicitly declared). Generally the "unlocalized" text is in English, so it is
             * a better bet as a fallback.
             */
            text = localeMap.get(null);
            if (text == null) {
                Locale def = Locale.US; // The default language for "unlocalized" string.
                if (locale != def) { // Avoid requesting the same locale twice (optimization).
                    text = getString(def);
                    if (text != null) {
                        return text;
                    }
                }
                def = Locale.getDefault();
                if (locale != def && def != Locale.US) {
                    text = getString(def);
                    if (text != null) {
                        return text;
                    }
                }
                // Every else failed; pickup a random string.
                // This behavior may change in future versions.
                final Iterator<String> it = localeMap.values().iterator();
                if (it.hasNext()) {
                    text = it.next();
                }
            }
        }
        return text;
    }

    /**
     * Returns {@code true} if all localized texts stored in this international string are
     * contained in the specified object. More specifically:
     *
     * <ul>
     *   <li>If {@code candidate} is an instance of {@link InternationalString}, then this method
     *       returns {@code true} if, for all <var>{@linkplain Locale locale}</var>-<var>{@linkplain
     *       String string}</var> pairs contained in {@code this}, <code>candidate.{@linkplain
     *       InternationalString#toString(Locale) toString}(locale)</code> returns a string
     *       {@linkplain String#equals equals} to {@code string}.</li>
     *
     *   <li>If {@code candidate} is an instance of {@link CharSequence}, then this method
     *       returns {@code true} if {@link #toString(Locale)} returns a string {@linkplain
     *       String#equals equals} to <code>candidate.{@linkplain CharSequence#toString()
     *       toString()}</code> for all locales.</li>
     *
     *   <li>If {@code candidate} is an instance of {@link Map}, then this methods returns
     *       {@code true} if all <var>{@linkplain Locale locale}</var>-<var>{@linkplain String
     *       string}</var> pairs are contained into {@code candidate}.</li>
     *
     *   <li>Otherwise, this method returns {@code false}.</li>
     * </ul>
     *
     * @param  candidate The object which may contains this international string.
     * @return {@code true} if the given object contains all localized strings found in this
     *         international string.
     */
    public synchronized boolean isSubsetOf(final Object candidate) {
        if (candidate instanceof InternationalString) {
            final InternationalString string = (InternationalString) candidate;
            for (final Map.Entry<Locale,String> entry : localeMap.entrySet()) {
                final Locale locale = entry.getKey();
                final String text   = entry.getValue();
                if (!text.equals(string.toString(locale))) {
                    return false;
                }
            }
        } else if (candidate instanceof CharSequence) {
            final String string = candidate.toString();
            for (final String text : localeMap.values()) {
                if (!text.equals(string)) {
                    return false;
                }
            }
        } else if (candidate instanceof Map<?,?>) {
            final Map<?,?> map = (Map<?,?>) candidate;
            return map.entrySet().containsAll(localeMap.entrySet());
        } else {
            return false;
        }
        return true;
    }

    /**
     * Compares this international string with the specified object for equality.
     *
     * @param object The object to compare with this international string.
     * @return {@code true} if the given object is equal to this string.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final DefaultInternationalString that = (DefaultInternationalString) object;
            return Objects.equals(this.localeMap, that.localeMap);
        }
        return false;
    }

    /**
     * Returns a hash code value for this international text.
     *
     * @return A hash code value for this international text.
     */
    @Override
    public synchronized int hashCode() {
        return localeMap.hashCode() ^ (int) serialVersionUID;
    }

    /**
     * Canonicalize the locales after deserialization.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        final int size = localeMap.size();
        if (size == 0) {
            return;
        }
        @SuppressWarnings({"unchecked","rawtypes"}) // Generic array creation.
        Map.Entry<Locale,String>[] entries = new Map.Entry[size];
        entries = localeMap.entrySet().toArray(entries);
        if (size == 1) {
            final Map.Entry<Locale,String> entry = entries[0];
            localeMap = Collections.singletonMap(Locales.unique(entry.getKey()), entry.getValue());
        } else {
            localeMap.clear();
            for (int i=0; i<entries.length; i++) {
                final Map.Entry<Locale,String> entry = entries[i];
                localeMap.put(Locales.unique(entry.getKey()), entry.getValue());
            }
        }
    }
}