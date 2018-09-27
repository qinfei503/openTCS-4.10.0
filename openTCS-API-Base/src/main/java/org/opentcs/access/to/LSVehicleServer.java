package org.opentcs.access.to;

import org.opentcs.components.Lifecycle;
import org.opentcs.drivers.vehicle.commands.LSDefaultCommand;
import java.net.Socket;
import java.util.HashMap;

public interface LSVehicleServer extends Lifecycle {
    final static HashMap<String, Socket> socketMap = new HashMap<String, Socket>();
    void sendCommd(String key, LSDefaultCommand cmd);
}
