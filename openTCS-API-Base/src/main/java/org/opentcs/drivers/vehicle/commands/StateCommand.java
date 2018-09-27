package org.opentcs.drivers.vehicle.commands;

import java.io.Serializable;

public class StateCommand implements Serializable {
    private static final long serialVersionUID=1L;
    private String code;
    private String description;

    public StateCommand(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public StateCommand(State state) {
        this(state.getValue(),state.getDescription());
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static enum State {
        NORMAL("0000", "正常"),
        ERROR("0000", "异常"),
        NO_VEHICLE("1001", "未配置车辆IP"),
        DUP_VEHICLE("1001", "车辆IP配置重复");
        private String value;
        private String description;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        State(String value, String description) {
            this.value = value;
            this.description = description;
        }

    }
}