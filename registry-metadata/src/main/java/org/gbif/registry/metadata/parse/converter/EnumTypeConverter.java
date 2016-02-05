package org.gbif.registry.metadata.parse.converter;

import org.gbif.api.util.VocabularyUtils;

import java.util.Map;

import com.google.common.collect.Maps;
import org.apache.commons.beanutils.converters.AbstractConverter;

/**
 * {@link org.apache.commons.beanutils.Converter} implementation that handles conversion
 * to and from java enumerations.
 */
public class EnumTypeConverter<T extends Enum<?>> extends AbstractConverter {

    private final Class<T> clazz;
    private final T defaultValue;

    private final Map<String, T> lookup = Maps.newHashMap();

    public EnumTypeConverter(Class<T> clazz, T defaultValue) {
        this.clazz = clazz;
        this.defaultValue = defaultValue;
    }


    public void addMappings(Map<String, T> mapping) {
        for (Map.Entry<String, T> entry : mapping.entrySet()) {
            lookup.put(entry.getKey().toLowerCase(), entry.getValue());
        }
    }

    /**
     * Return the default type this <code>Converter</code> handles.
     *
     * @return The default type this <code>Converter</code> handles.
     */
    protected Class getDefaultType() {
        return clazz;
    }

    /**
     * <p>Convert a PreservationMethodType or object into a String.</p> Checks map with alternative values for each
     * PreservationMethodType before returning the default value.
     *
     * @param type  Data type to which this value should be converted
     * @param value The input value to be converted
     * @return The converted value.
     * @throws Throwable if an error occurs converting to the specified type
     */
    protected Object convertToType(Class type, Object value) throws Throwable {
        // never null, super class implements this as:
        // return value.toString();
        final String val = value.toString();
        if (lookup.containsKey(val)) {
            return lookup.get(val);
        }
        // try regular enum values
        try {
            T eVal = (T) VocabularyUtils.lookupEnum(val, clazz);
            return eVal == null ? defaultValue : eVal;
        } catch (IllegalArgumentException e) {
            // cant parse, return default
            return defaultValue;
        }
    }
}
