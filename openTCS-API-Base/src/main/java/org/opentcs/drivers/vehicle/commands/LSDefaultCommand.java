package org.opentcs.drivers.vehicle.commands;

import java.io.Serializable;

public class LSDefaultCommand<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private String type;
    private T message;

    public LSDefaultCommand(String type, T message) {
        this.type = type;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public T getMessage() {
        return message;
    }

    public void setMessage(T message) {
        this.message = message;
    }
}
