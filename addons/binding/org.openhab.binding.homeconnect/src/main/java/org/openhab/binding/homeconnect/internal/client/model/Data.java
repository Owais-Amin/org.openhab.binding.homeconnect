/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homeconnect.internal.client.model;

/**
 * Data model
 *
 * @author Jonas Brüstel - Initial contribution
 *
 */
public class Data {

    private String name;
    private String value;
    private String unit;

    public Data(String name, String value, String unit) {
        super();
        this.name = name;
        this.value = value;
        this.unit = unit;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public int getValueAsInt() {
        return value != null ? Integer.valueOf(getValue()).intValue() : 0;
    }

    public boolean getValueAsBoolean() {
        return value != null ? Boolean.valueOf(getValue()).booleanValue() : false;
    }

    @Override
    public String toString() {
        return "Data [name=" + name + ", value=" + value + ", unit=" + unit + "]";
    }

}
