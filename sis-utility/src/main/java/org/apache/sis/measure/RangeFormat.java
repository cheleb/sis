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
package org.apache.sis.measure;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.Format;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.AttributedCharacterIterator;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.resources.Errors;


/**
 * Parses and formats {@linkplain Range ranges} of the given type. The kind of ranges created
 * by the {@code parse} method is determined by the class of range elements:
 *
 * <ul>
 *   <li>If the elements type is assignable to {@link Date}, then the {@code parse} method
 *       will create {@link DateRange} objects.</li>
 *   <li>If the elements type is assignable to {@link Number}, then:
 *     <ul>
 *       <li>If the text to parse contains a {@linkplain Unit unit} of measure, then
 *           the {@code parse} method will create {@link MeasurementRange} objects.</li>
 *       <li>Otherwise the {@code parse} method will create {@link NumberRange} objects.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.06)
 * @version 0.3
 * @module
 *
 * @see Range
 * @see DateRange
 * @see NumberRange
 * @see MeasurementRange
 */
public class RangeFormat extends Format {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6700474540675919894L;

    /**
     * The constant value for {@link FieldPosition} which designate the minimal value.
     *
     * @see Field#MIN_VALUE
     */
    private static final int MIN_VALUE_FIELD = 0;

    /**
     * The constant value for {@link FieldPosition} which designate the maximal value.
     *
     * @see Field#MAX_VALUE
     */
    private static final int MAX_VALUE_FIELD = 1;

    /**
     * The constant value for {@link FieldPosition} which designate the units of measurement.
     *
     * @see Field#UNIT
     */
    private static final int UNIT_FIELD = 2;

    /**
     * Defines constants that are used as attribute keys in the iterator returned from
     * {@link RangeFormat#formatToCharacterIterator(Object)}.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     */
    public static final class Field extends FormatField {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 2286464612919602208L;

        /**
         * Creates a new field of the given name. The given name shall
         * be identical to the name of the public static constant.
         */
        private Field(final String name, final int fieldID) {
            super(name, fieldID);
        }

        /**
         * Identifies the minimal value field in a range.
         * When formatting a string, this value may be specified to the {@link FieldPosition}
         * constructor in order to get the bounding index where the minimal value has been written.
         */
        public static final Field MIN_VALUE = new Field("MIN_VALUE", MIN_VALUE_FIELD);

        /**
         * Identifies the maximal value field in a range.
         * When formatting a string, this value may be specified to the {@link FieldPosition}
         * constructor in order to get the bounding index where the maximal value has been written.
         */
        public static final Field MAX_VALUE = new Field("MAX_VALUE", MAX_VALUE_FIELD);

        /**
         * Identifies the unit field in a range, if any.
         * When formatting a string, this value may be specified to the {@link FieldPosition}
         * constructor in order to get the bounding index where the unit has been written.
         */
        public static final Field UNIT = new Field("UNIT", UNIT_FIELD);

        /**
         * Returns the field constant for the given numeric identifier.
         */
        static Field forCode(final int field) {
            switch (field) {
                case MIN_VALUE_FIELD: return MIN_VALUE;
                case MAX_VALUE_FIELD: return MAX_VALUE;
                case UNIT_FIELD:      return UNIT;
                default: throw new AssertionError(field);
            }
        }
    }

    /**
     * The character opening a range in which the minimal value is inclusive.
     * The default value is {@code '['}.
     */
    private final char openInclusive;

    /**
     * The character opening a range in which the minimal value is exclusive.
     * The default value is {@code '('}. Note that the {@code ']'} character
     * is also sometime used.
     */
    private final char openExclusive;

    /**
     * An alternative character opening a range in which the minimal value is exclusive.
     * This character is not used for formatting (only {@link #openExclusive} is used),
     * but is accepted during parsing. The default value is {@code ']'}.
     */
    private final char openExclusiveAlt;

    /**
     * The character closing a range in which the maximal value is inclusive.
     * The default value is {@code ']'}.
     */
    private final char closeInclusive;

    /**
     * The character closing a range in which the maximal value is exclusive.
     * The default value is {@code ')'}. Note that the {@code '['} character
     * is also sometime used.
     */
    private final char closeExclusive;

    /**
     * An alternative character closing a range in which the maximal value is exclusive.
     * This character is not used for formatting (only {@link #closeExclusive} is used),
     * but is accepted during parsing. The default value is {@code '['}.
     */
    private final char closeExclusiveAlt;

    /**
     * The string to use as a separator between minimal and maximal value, not including
     * whitespaces. The default value is {@code "…"} (Unicode 2026).
     */
    private final String separator;

    /**
     * Symbols used by this format, inferred from {@link DecimalFormatSymbols}.
     */
    private final char minusSign;

    /**
     * Symbols used by this format, inferred from {@link DecimalFormatSymbols}.
     */
    private final String infinity;

    /**
     * The type of the range components. Valid types are {@link Number}, {@link Angle},
     * {@link Date} or a subclass of those types. This value determines the kind of range
     * to be created by the parse method:
     *
     * <ul>
     *   <li>{@link NumberRange} if the element type is assignable to {@link Number} or {@link Angle}.</li>
     *   <li>{@link DateRange}   if the element type is assignable to {@link Date}.</li>
     * </ul>
     *
     * @see Range#getElementType()
     */
    protected final Class<?> elementType;

    /**
     * The format to use for parsing and formatting the range components.
     * The format is determined from the {@linkplain #elementType element type}:
     *
     * <ul>
     *   <li>{@link AngleFormat}  if the element type is assignable to {@link Angle}.</li>
     *   <li>{@link NumberFormat} if the element type is assignable to {@link Number}.</li>
     *   <li>{@link DateFormat}   if the element type is assignable to {@link Date}.</li>
     * </ul>
     */
    protected final Format elementFormat;

    /**
     * The format for units of measurement, or {@code null} if none. This is non-null if and
     * only if {@link #elementType} is assignable to {@link Number} but not to {@link Angle}.
     */
    protected final UnitFormat unitFormat;

    /**
     * Creates a new format for parsing and formatting {@linkplain NumberRange number ranges}
     * using the {@linkplain Locale#getDefault() default locale}.
     */
    public RangeFormat() {
        this(Locale.getDefault(Locale.Category.FORMAT));
    }

    /**
     * Creates a new format for parsing and formatting {@linkplain NumberRange number ranges}
     * using the given locale.
     *
     * @param  locale The locale for parsing and formatting range components.
     */
    public RangeFormat(final Locale locale) {
        this(locale, Number.class);
    }

    /**
     * Creates a new format for parsing and formatting {@linkplain DateRange date ranges}
     * using the given locale and timezone.
     *
     * @param locale   The locale for parsing and formatting range components.
     * @param timezone The timezone for the date to be formatted.
     */
    public RangeFormat(final Locale locale, final TimeZone timezone) {
        this(locale, Date.class);
        ((DateFormat) elementFormat).setTimeZone(timezone);
    }

    /**
     * Creates a new format for parsing and formatting {@linkplain Range ranges} of
     * the given element type using the given locale. The element type is typically
     * {@code Date.class} or some subclass of {@code Number.class}.
     *
     * @param  locale The locale for parsing and formatting range components.
     * @param  elementType The type of range components.
     * @throws IllegalArgumentException If the given type is not recognized by this constructor.
     */
    public RangeFormat(final Locale locale, final Class<?> elementType) throws IllegalArgumentException {
        this.elementType = elementType;
        if (Angle.class.isAssignableFrom(elementType)) {
            elementFormat = AngleFormat.getInstance(locale);
            unitFormat    = null;
        } else if (Number.class.isAssignableFrom(elementType)) {
            elementFormat = NumberFormat.getNumberInstance(locale);
            unitFormat    = UnitFormat.getInstance(locale);
        } else if (Date.class.isAssignableFrom(elementType)) {
            elementFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
            unitFormat    = null;
        } else {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedType_1, elementType));
        }
        final DecimalFormatSymbols ds;
        if (elementFormat instanceof DecimalFormat) {
            ds = ((DecimalFormat) elementFormat).getDecimalFormatSymbols();
        } else {
            ds = DecimalFormatSymbols.getInstance(locale);
        }
        minusSign         = ds.getMinusSign();
        infinity          = ds.getInfinity();
        openInclusive     = '['; // Future SIS version may determine those characters from the locale.
        openExclusive     = '('; // We may also provide an 'applyPattern(String)' method for setting those char.
        openExclusiveAlt  = ']';
        closeInclusive    = ']';
        closeExclusive    = ')';
        closeExclusiveAlt = '[';
        separator         = "…";
    }

    /**
     * Returns {@code true} if the given character is any of the opening bracket characters.
     */
    private boolean isOpen(final int c) {
        return (c == openInclusive) || (c == openExclusive) || (c == openExclusiveAlt);
    }

    /**
     * Returns {@code true} if the given character is any of the closing bracket characters.
     */
    private boolean isClose(final int c) {
        return (c == closeInclusive) || (c == closeExclusive) || (c == closeExclusiveAlt);
    }

    /**
     * Returns the pattern used by {@link #elementFormat} for formatting the minimum and
     * maximum values. If the element format does not use pattern, returns {@code null}.
     *
     * @param  localized {@code true} for returning the localized pattern, or {@code false}
     *         for the unlocalized one.
     * @return The pattern, or {@code null} if the {@link #elementFormat} doesn't use pattern.
     *
     * @see DecimalFormat#toPattern()
     * @see SimpleDateFormat#toPattern()
     * @see AngleFormat#toPattern()
     */
    public String getElementPattern(final boolean localized) {
        final Format format = elementFormat;
        if (format instanceof DecimalFormat) {
            final DecimalFormat df = (DecimalFormat) format;
            return localized ? df.toLocalizedPattern() : df.toPattern();
        }
        if (format instanceof SimpleDateFormat) {
            final SimpleDateFormat df = (SimpleDateFormat) format;
            return localized ? df.toLocalizedPattern() : df.toPattern();
        }
        if (format instanceof AngleFormat) {
            return ((AngleFormat) format).toPattern();
        }
        return null;
    }

    /**
     * Sets the pattern to be used by {@link #elementFormat} for formatting the minimum and
     * maximum values.
     *
     * @param  pattern The new pattern.
     * @param  localized {@code true} if the given pattern is localized.
     * @throws IllegalStateException If the {@link #elementFormat} does not use pattern.
     *
     * @see DecimalFormat#applyPattern(String)
     * @see SimpleDateFormat#applyPattern(String)
     * @see AngleFormat#applyPattern(String)
     */
    public void setElementPattern(final String pattern, final boolean localized) {
        final Format format = elementFormat;
        if (format instanceof DecimalFormat) {
            final DecimalFormat df = (DecimalFormat) format;
            if (localized) {
                df.applyLocalizedPattern(pattern);
            } else {
                df.applyPattern(pattern);
            }
        } else if (format instanceof SimpleDateFormat) {
            final SimpleDateFormat df = (SimpleDateFormat) format;
            if (localized) {
                df.applyLocalizedPattern(pattern);
            } else {
                df.applyPattern(pattern);
            }
        } else if (format instanceof AngleFormat) {
            ((AngleFormat) format).applyPattern(pattern);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Returns the {@code *_FIELD} constant for the given field position, or -1 if none.
     */
    private static int getField(final FieldPosition position) {
        if (position != null) {
            final Format.Field field = position.getFieldAttribute();
            if (field instanceof Field) {
                return ((Field) field).field;
            }
            return position.getField();
        }
        return -1;
    }

    /**
     * Casts the given object to a {@code Range}, or throws an {@code IllegalArgumentException}
     * if the given object is not a {@code Range} instance.
     */
    private static Range<?> cast(final Object range) throws IllegalArgumentException {
        if (range instanceof Range<?>) {
            return (Range<?>) range;
        }
        final String message;
        if (range == null) {
            message = Errors.format(Errors.Keys.NullArgument_1, "range");
        } else {
            message = Errors.format(Errors.Keys.IllegalArgumentClass_3, "range", Range.class, range.getClass());
        }
        throw new IllegalArgumentException(message);
    }

    /**
     * Formats a {@link Range} and appends the resulting text to a given string buffer.
     * This method formats then given range as below:
     *
     * <ul>
     *   <li>If the range is empty, then this method appends {@code "[]"}.</li>
     *   <li>Otherwise if the minimal value is equals to the maximal value, then
     *       that value is formatted alone.</li>
     *   <li>Otherwise the minimal and maximal values are formatted like {@code [min … max]} for
     *       inclusive bounds or {@code (min … max)} for exclusive bounds, or a mix of both styles.
     *       The ∞ symbol is used in place of {@code min} or {@code max} for unbounded ranges.</li>
     * </ul>
     *
     * If the given range is a {@link MeasurementRange}, then the unit of measurement is
     * appended to the above string representation.
     *
     * @param  range      The {@link Range} object to format.
     * @param  toAppendTo Where the text is to be appended.
     * @param  pos        Identifies a field in the formatted text, or {@code null} if none.
     * @return The string buffer passed in as {@code toAppendTo}, with formatted text appended.
     * @throws IllegalArgumentException If this formatter can not format the given object.
     */
    @Override
    public StringBuffer format(final Object range, final StringBuffer toAppendTo, final FieldPosition pos) {
        format(cast(range), toAppendTo, pos, null);
        return toAppendTo;
    }

    /**
     * Implementation of the format methods.
     *
     * @param range      The range to format.
     * @param toAppendTo Where the text is to be appended.
     * @param pos        Identifies a field in the formatted text, or {@code null} if none.
     * @param characterIterator The character iterator for which the attributes need to be set,
     *        or null if none. This is actually an instance of {@link FormattedCharacterIterator},
     *        but we use the interface here for avoiding too early class loading.
     */
    @SuppressWarnings("fallthrough")
    private void format(final Range<?> range, final StringBuffer toAppendTo, final FieldPosition pos,
            final AttributedCharacterIterator characterIterator)
    {
        /*
         * Special case for an empty range. This is typically formatted as "[]". The field
         * position is unconditionally set to the empty substring inside the brackets.
         */
        int fieldPos = getField(pos);
        if (range.isEmpty()) {
            toAppendTo.append(openInclusive);
            if (fieldPos >= MIN_VALUE_FIELD && fieldPos <= UNIT_FIELD) {
                final int p = toAppendTo.length();
                pos.setBeginIndex(p); // First index, inclusive.
                pos.setEndIndex  (p); // Last index, exclusive
            }
            toAppendTo.append(closeInclusive);
            return;
        }
        /*
         * Format a non-empty range by looping over all possible fields.
         *
         * Secial case: if minimal and maximal values are the same,
         * formats only the maximal value.
         */
        final Comparable<?> minValue = range.getMinValue();
        final Comparable<?> maxValue = range.getMaxValue();
        final boolean isSingleton = (minValue != null) && minValue.equals(maxValue);
        int field;
        if (isSingleton) {
            if (fieldPos == MIN_VALUE_FIELD) {
                fieldPos = MAX_VALUE_FIELD;
            }
            field = MAX_VALUE_FIELD;
        } else {
            field = MIN_VALUE_FIELD;
            toAppendTo.append(range.isMinIncluded() ? openInclusive : openExclusive);
        }
        for (; field <= UNIT_FIELD; field++) {
            final Object value;
            switch (field) {
                case MIN_VALUE_FIELD: value = minValue; break;
                case MAX_VALUE_FIELD: value = maxValue; break;
                case UNIT_FIELD:      value = range.getUnits(); break;
                default: throw new AssertionError(field);
            }
            int startPosition = toAppendTo.length();
            if (value == null) {
                switch (field) {
                    case MIN_VALUE_FIELD: toAppendTo.append(minusSign); // Fall through
                    case MAX_VALUE_FIELD: toAppendTo.append(infinity); break;
                }
            } else {
                final Format format;
                if (field == UNIT_FIELD) {
                    startPosition = toAppendTo.append(' ').length();
                    format = unitFormat;
                } else {
                    format = elementFormat;
                }
                if (characterIterator != null) {
                    ((FormattedCharacterIterator) characterIterator)
                            .append(format.formatToCharacterIterator(value), toAppendTo);
                } else {
                    format.format(value, toAppendTo, new FieldPosition(-1));
                }
            }
            /*
             * At this point, the field has been formatted. Now store the field index,
             * then append the separator between this field and the next one.
             */
            if (characterIterator != null) {
                ((FormattedCharacterIterator) characterIterator)
                        .addFieldLimit(Field.forCode(field), value, startPosition);
            }
            if (field == fieldPos) {
                pos.setBeginIndex(startPosition);
                pos.setEndIndex(toAppendTo.length());
            }
            switch (field) {
                case MIN_VALUE_FIELD: {
                    toAppendTo.append(' ').append(separator).append(' ');
                    break;
                }
                case MAX_VALUE_FIELD: {
                    if (!isSingleton) {
                        toAppendTo.append(range.isMaxIncluded() ? closeInclusive : closeExclusive);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Formats a range as an attributed character iterator.
     * Callers can iterate and queries the attribute values as in the following example:
     *
     * {@preformat java
     *     AttributedCharacterIterator it = rangeFormat.formatToCharacterIterator(myRange);
     *     for (char c=it.first(); c!=AttributedCharacterIterator.DONE; c=c.next()) {
     *         // 'c' is a character from the formatted string.
     *         if (it.getAttribute(RangeFormat.Field.MIN_VALUE) != null) {
     *             // If we enter this block, then the character 'c' is part of the minimal value,
     *             // This field extends from it.getRunStart(MIN_VALUE) to it.getRunLimit(MIN_VALUE).
     *         }
     *     }
     * }
     *
     * Alternatively, if the current {@linkplain AttributedCharacterIterator#getIndex() iterator
     * index} is before the start of the minimum value field, then the starting position of that
     * field can be obtained directly by {@code it.getRunLimit(MIN_VALUE)}. If the current iterator
     * index is inside the minimum value field, then the above method call will rather returns the
     * end of that field. The same strategy works for other all fields too.
     *
     * <p>The returned character iterator contains all {@link java.text.NumberFormat.Field},
     * {@link java.text.DateFormat.Field} or {@link org.apache.sis.measure.AngleFormat.Field}
     * attributes in addition to the {@link Field} ones. Consequently the same character may
     * have more than one attribute.</p>
     *
     * <p>In Apache SIS implementation, the returned character iterator also implements the
     * {@link CharSequence} interface for convenience.</p>
     *
     * @param  range {@link Range} object to format.
     * @return A character iterator together with the attributes describing the formatted value.
     * @throws IllegalArgumentException if {@code value} if not an instance of {@link Range}.
     */
    @Override
    public AttributedCharacterIterator formatToCharacterIterator(final Object range) {
        final StringBuffer buffer = new StringBuffer();
        final FormattedCharacterIterator it = new FormattedCharacterIterator(buffer);
        format(cast(range), buffer, null, it);
        return it;
    }

    /**
     * Parses text from a string to produce a range. The default implementation delegates to
     * {@link #parse(String)} with no additional work.
     *
     * @param  source The text, part of which should be parsed.
     * @return A range parsed from the string, or {@code null} in case of error.
     * @throws ParseException If the given string can not be fully parsed.
     */
    @Override
    public Object parseObject(final String source) throws ParseException {
        return parse(source);
    }

    /**
     * Parses text from a string to produce a range. The default implementation delegates to
     * {@link #parse(String, ParsePosition)} with no additional work.
     *
     * @param  source The text, part of which should be parsed.
     * @param  pos    Index and error index information as described above.
     * @return A range parsed from the string, or {@code null} in case of error.
     */
    @Override
    public Object parseObject(final String source, final ParsePosition pos) {
        return parse(source, pos);
    }

    /**
     * Parses text from the given string to produce a range. This method use the full string.
     * If there is some unparsed characters after the parsed range, then this method thrown an
     * exception.
     *
     * @param  source The text to parse.
     * @return The parsed range (never {@code null}).
     * @throws ParseException If the given string can not be fully parsed.
     */
    public Range<?> parse(final String source) throws ParseException {
        final ParsePosition pos = new ParsePosition(0);
        UnconvertibleObjectException failure = null;
        try {
            final Range<?> range = tryParse(source, pos);
            if (range != null) {
                return range;
            }
        } catch (UnconvertibleObjectException e) {
            failure = e;
        }
        final int errorIndex = pos.getErrorIndex();
        final ParseException e = new ParseException(Errors.format(Errors.Keys.UnparsableStringForClass_3,
                elementType, source, CharSequences.token(source, errorIndex)), errorIndex);
        e.initCause(failure);
        throw e;
    }

    /**
     * Parses text from a string to produce a range. The method attempts to parse text starting
     * at the index given by {@code pos}. If parsing succeeds, then the index of {@code pos} is
     * updated to the index after the last character used, and the parsed range is returned. If
     * an error occurs, then the index of {@code pos} is not changed, the error index of {@code pos}
     * is set to the index of the character where the error occurred, and {@code null} is returned.
     *
     * @param  source The text, part of which should be parsed.
     * @param  pos    Index and error index information as described above.
     * @return A range parsed from the string, or {@code null} in case of error.
     */
    public Range<?> parse(final String source, final ParsePosition pos) {
        final int origin = pos.getIndex();
        Range<?> range;
        try {
            // Remainder: tryParse may return null.
            range = tryParse(source, pos);
        } catch (UnconvertibleObjectException e) {
            // Ignore - the error will be reported through the error index.
            range = null;
        }
        if (range != null) {
            pos.setErrorIndex(-1);
        } else {
            pos.setIndex(origin);
        }
        return range;
    }

    /**
     * Tries to parse the given text. In case of success, the error index is undetermined and
     * need to be reset to -1.  In case of failure (including an exception being thrown), the
     * parse index is undetermined and need to be reset to its initial value.
     */
    private Range<?> tryParse(final String source, final ParsePosition pos)
            throws UnconvertibleObjectException
    {
        final int length = source.length();
        /*
         * Skip leading whitespace and find the first non-blank character.  It is usually
         * an opening bracket, except if minimal and maximal values are the same in which
         * case the brackets may be omitted.
         */
        int index, c;
        for (index = pos.getIndex(); ; index += Character.charCount(c)) {
            if (index >= length) {
                pos.setErrorIndex(length);
                return null;
            }
            c = source.codePointAt(index);
            if (!Character.isWhitespace(c)) break;
        }
        /*
         * Get the minimal and maximal values, and whatever they are inclusive or exclusive.
         */
        final Object minValue, maxValue;
        final boolean isMinIncluded, isMaxIncluded;
        if (!isOpen(c)) {
            /*
             * No bracket. Assume that we have a single value for the range.
             */
            pos.setIndex(index);
            final Object value = elementFormat.parseObject(source, pos);
            if (value == null) {
                return null;
            }
            pos.setErrorIndex(index); // In case of failure during the conversion.
            minValue = maxValue = convert(value);
            isMinIncluded = isMaxIncluded = true;
            index = pos.getIndex();
        } else {
            /*
             * We found an opening bracket. Skip the whitespaces. If the next
             * character is a closing bracket, then we have an empty range.
             */
            isMinIncluded = (c == openInclusive);
            do {
                index += Character.charCount(c);
                if (index >= length) {
                    pos.setErrorIndex(length);
                    return null;
                }
                c = source.codePointAt(index);
            } while (Character.isWhitespace(c));
            if (isClose(c)) {
                pos.setErrorIndex(index);  // In case of failure during the conversion.
                minValue = maxValue = convert(0);
                isMaxIncluded = false;
                index += Character.charCount(c);
            } else {
                /*
                 * At this point, we have determined that the range is non-empty and there
                 * is at least one value to parse. First, parse the minimal value. If we
                 * fail to parse, check if it was the infinity value (note that infinity
                 * should have been parsed successfully if the format is DecimalFormat).
                 */
                pos.setIndex(index);
                int savedIndex = index;
                Object value = elementFormat.parseObject(source, pos);
                if (value == null) {
                    if (c == minusSign) {
                        index += Character.charCount(c);
                    }
                    if (!source.regionMatches(index, infinity, 0, infinity.length())) {
                        return null;
                    }
                    pos.setIndex(index += infinity.length());
                }
                pos.setErrorIndex(savedIndex); // In case of failure during the conversion.
                minValue = convert(value);
                /*
                 * Parsing of minimal value succeed and its type is valid. Now look for the
                 * separator. If it is not present, then assume that we have a single value
                 * for the range. The default RangeFormat implementation does not format
                 * brackets in such case (see the "No bracket" case above), but we make the
                 * parser tolerant to the case where the brackets are present.
                 */
                for (index = pos.getIndex(); ; index += Character.charCount(c)) {
                    if (index >= length) {
                        pos.setErrorIndex(length);
                        return null;
                    }
                    c = source.codePointAt(index);
                    if (!Character.isWhitespace(c)) break;
                }
                final String separator = this.separator;
                if (source.regionMatches(index, separator, 0, separator.length())) {
                    index += separator.length();
                    for (;; index += Character.charCount(c)) {
                        if (index >= length) {
                            pos.setErrorIndex(length);
                            return null;
                        }
                        c = source.codePointAt(index);
                        if (!Character.isWhitespace(c)) break;
                    }
                    pos.setIndex(index);
                    value = elementFormat.parseObject(source, pos);
                    if (value == null) {
                        if (!source.regionMatches(index, infinity, 0, infinity.length())) {
                            return null;
                        }
                        pos.setIndex(index += infinity.length());
                    }
                    pos.setErrorIndex(index); // In case of failure during the conversion.
                    maxValue = convert(value);
                    /*
                     * Skip one last time the whitespaces. The check for the closing bracket
                     * (which is mandatory) is performed outside the "if" block since it is
                     * common to the two "if ... else" cases.
                     */
                    for (index = pos.getIndex(); ; index += Character.charCount(c)) {
                        if (index >= length) {
                           pos.setErrorIndex(length);
                            return null;
                        }
                        c = source.charAt(index);
                        if (!Character.isWhitespace(c)) break;
                    }
                } else {
                    maxValue = minValue;
                }
                if (!isClose(c)) {
                    pos.setErrorIndex(index);
                    return null;
                }
                index += Character.charCount(c);
                isMaxIncluded = (c == closeInclusive);
            }
            pos.setIndex(index);
        }
        /*
         * Parses the unit, if any. The units are always optional: if we can not parse
         * them, then we will consider that the parsing stopped before the units.
         */
        Unit<?> unit = null;
        if (unitFormat != null) {
            while (index < length) {
                c = source.codePointAt(index);
                if (Character.isWhitespace(c)) {
                    index += Character.charCount(c);
                    continue;
                }
                // At this point we found a character that could be
                // the beginning of a unit symbol. Try to parse that.
                pos.setIndex(index);
// TODO: Uncomment when we have upgrated JSR-275 dependency.
//              unit = unitFormat.parse(source, pos);
                break;
            }
        }
        /*
         * At this point, all required informations are available. Now build the range.
         * In the special case were the target type is the generic Number type instead
         * than a more specialized type, the finest suitable type will be determined.
         */
        if (Number.class.isAssignableFrom(elementType)) {
            @SuppressWarnings({"unchecked","rawtypes"})
            Class<? extends Number> type = (Class) elementType;
            Number min = (Number) minValue;
            Number max = (Number) maxValue;
            if (type == Number.class) {
                type = Numbers.widestClass(Numbers.narrowestClass(min), Numbers.narrowestClass(max));
                min  = Numbers.cast(min, type);
                max  = Numbers.cast(max, type);
            }
            if (min.doubleValue() == Double.NEGATIVE_INFINITY) min = null;
            if (max.doubleValue() == Double.POSITIVE_INFINITY) max = null;
            if (unit != null) {
                @SuppressWarnings({"unchecked","rawtypes"})
                final MeasurementRange<?> range = new MeasurementRange(type, min, isMinIncluded, max, isMaxIncluded, unit);
                return range;
            }
            @SuppressWarnings({"unchecked","rawtypes"})
            final NumberRange<?> range = new NumberRange(type, min, isMinIncluded, max, isMaxIncluded);
            return range;
        } else if (Date.class.isAssignableFrom(elementType)) {
            final Date min = (Date) minValue;
            final Date max = (Date) maxValue;
            return new DateRange(min, isMinIncluded, max, isMaxIncluded);
        } else {
            @SuppressWarnings({"unchecked","rawtypes"})
            final Class<? extends Comparable<?>> type = (Class) elementType;
            final Comparable<?> min = (Comparable<?>) minValue;
            final Comparable<?> max = (Comparable<?>) maxValue;
            @SuppressWarnings({"unchecked","rawtypes"})
            final Range<?> range = new Range(type, min, isMinIncluded, max, isMaxIncluded);
            return range;
        }
    }

    /**
     * Converts the given value to the a {@link #elementType} type.
     */
    @SuppressWarnings("unchecked")
    private Object convert(final Object value) throws UnconvertibleObjectException {
        if (value == null || elementType.isInstance(value)) {
            return value;
        }
        if (value instanceof Number && Number.class.isAssignableFrom(elementType)) {
            return Numbers.cast((Number) value, (Class<? extends Number>) elementType);
        }
        throw new UnconvertibleObjectException(Errors.format(
                Errors.Keys.IllegalClass_2, elementType, value.getClass()));
    }
}