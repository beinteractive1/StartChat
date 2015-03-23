package org.telegram.utils;

import java.util.Currency;

import javax.annotation.Nonnull;

/**
 * Created by Max van de Wiel on 9-3-15.
 *
 * Generic reusable and simple static utility methods
 * Groups of utilities in this class might candidate to refactor to a specialized utility class over time!
 */
public class GenericUtils
{
    /**
     * Utility to test whether a given string starts with a prefix, ignoring differentiation in capital- or lower case
     * variants
     *
     * @param string the string that is investigated for the startsWith argument to start with
     * @param prefix the prefix string looked for at the start of the given string
     * @return boolean will be true when given string starts with prefix
     */
    public static boolean startsWithIgnoreCase(@Nonnull final String string, @Nonnull final String prefix)
    {
        return string.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * Utility used to get a CurrencySymbol based on a ISO 4217 currencyCode String. This utility catches
     * any IllegalArgumentException thrown by java.util.currency and returns the given currencyCode string
     * argument when this this occurs.
     *
     * @param currencyCode a ISO 4217 currencyCode to resolve the currency symbol with
     * @return returns the resolved currency symbol or the currencyCode provided when no currency symbol could be resolved
     */
    public static String currencySymbol(@Nonnull final String currencyCode)
    {
        try
        {
            final Currency currency = Currency.getInstance(currencyCode);
            return currency.getSymbol();
        }
        catch (final IllegalArgumentException x)
        {
            return currencyCode;
        }
    }
}
