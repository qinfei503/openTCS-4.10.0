package org.opentcs.drivers.vehicle.commands;

import org.opentcs.data.model.Triple;

import java.io.Serializable;

public class VehicleInfoCommand implements Serializable {
    private static final long serialVersionUID = 1L;
    private String position;
    private Triple precisePosition;
    private String state;
    private String processState;

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Triple getPrecisePosition() {
        return precisePosition;
    }

    public void setPrecisePosition(Triple precisePosition) {
        this.precisePosition = precisePosition;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getProcessState() {
        return processState;
    }

    public void setProcessState(String processState) {
        this.processState = processState;
    }
}
