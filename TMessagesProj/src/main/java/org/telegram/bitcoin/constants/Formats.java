package org.telegram.bitcoin.constants;

import org.bitcoinj.utils.MonetaryFormat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kim on 23-3-15.
 */
public final class Formats
{
    public static final MonetaryFormat LOCAL_FORMAT = new MonetaryFormat().noCode().minDecimals(2).optionalDecimals();

    public static final String PREFIX_ALMOST_EQUAL_TO = Character.toString(CurrencyCharConstants.CHAR_ALMOST_EQUAL_TO.value()) + CurrencyCharConstants.CHAR_THIN_SPACE.value();

    public static final Pattern PATTERN_MONETARY_SPANNABLE = Pattern.compile("(?:([\\p{Alpha}\\p{Sc}]++)\\s?+)?" // prefix
            + "([\\+\\-" + CurrencyCharConstants.CURRENCY_PLUS_SIGN.value() + CurrencyCharConstants.CURRENCY_MINUS_SIGN.value() + "]?+(?:\\d*+\\.\\d{0,2}+|\\d++))" // significant
            + "(\\d++)?"); // insignificant

    public static int PATTERN_GROUP_PREFIX = 1; // optional
    public static int PATTERN_GROUP_SIGNIFICANT = 2; // mandatory
    public static int PATTERN_GROUP_INSIGNIFICANT = 3; // optional

    private static final Pattern PATTERN_OUTER_HTML_PARAGRAPH = Pattern.compile("<p[^>]*>(.*)</p>\n?", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public static String maybeRemoveOuterHtmlParagraph(final String html)
    {
        final Matcher m = PATTERN_OUTER_HTML_PARAGRAPH.matcher(html);
        if (m.matches())
            return m.group(1);
        else
            return html;
    }
}