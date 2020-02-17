package it.polito.helpenvironmentnow.Helper;

import androidx.annotation.Nullable;

import java.util.Objects;

public class BtDevice {

    private String name;
    private String address;

    public BtDevice() {}

    public BtDevice(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BtDevice btDevice = (BtDevice) o;
        return address.equals(btDevice.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }
}
