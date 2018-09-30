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
 * Home appliance model
 *
 * @author Jonas Brüstel - Initial contribution
 *
 */
public class HomeAppliance {
    private String name;
    private String brand;
    private String vib;
    private boolean connected;
    private String type;
    private String enumber;
    private String haId;

    public HomeAppliance(String haId, String name, String brand, String vib, boolean connected, String type,
            String enumber) {
        this.haId = haId;
        this.name = name;
        this.brand = brand;
        this.vib = vib;
        this.connected = connected;
        this.type = type;
        this.enumber = enumber;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public String getVib() {
        return vib;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getType() {
        return type;
    }

    public String getEnumber() {
        return enumber;
    }

    public String getHaId() {
        return haId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((haId == null) ? 0 : haId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        HomeAppliance other = (HomeAppliance) obj;
        if (haId == null) {
            if (other.haId != null) {
                return false;
            }
        } else if (!haId.equals(other.haId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "HomeAppliance [haId=" + haId + ", name=" + name + ", brand=" + brand + ", vib=" + vib + ", connected="
                + connected + ", type=" + type + ", enumber=" + enumber + "]";
    }

}
