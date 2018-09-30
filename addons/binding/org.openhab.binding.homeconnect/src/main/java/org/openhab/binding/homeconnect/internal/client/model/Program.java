/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homeconnect.internal.client.model;

import java.util.List;

/**
 * Program model
 *
 * @author Jonas Brüstel - Initial contribution
 *
 */
public class Program {
    private String key;
    private List<Option> options;

    public Program(String key, List<Option> options) {
        super();
        this.key = key;
        this.options = options;
    }

    public String getKey() {
        return key;
    }

    public List<Option> getOptions() {
        return options;
    }

    @Override
    public String toString() {
        return "Program [key=" + key + ", options=" + options + "]";
    }
}
