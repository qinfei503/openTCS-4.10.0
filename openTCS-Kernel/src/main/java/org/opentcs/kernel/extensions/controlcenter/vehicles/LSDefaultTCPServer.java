package org.opentcs.kernel.extensions.controlcenter.vehicles;

import com.alibaba.fastjson.JSONObject;
import org.opentcs.access.to.LSVehicleServer;
import org.opentcs.components.kernel.services.TCSObjectService;
import org.opentcs.constants.LSDefaultConstants;
import org.opentcs.data.model.Vehicle;
import org.opentcs.drivers.vehicle.commands.StateCommand;
import org.opentcs.drivers.vehicle.commands.LSDefaultCommand;
import org.opentcs.kernel.util.SocketUtil;
import org.opentcs.kernel.vehicles.LocalVehicleControllerPool;
import org.opentcs.kernel.workingset.TCSObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.utils.StringUtils;

import javax.inject.Inject;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.stream.Collectors;

public class LSDefaultTCPServer implements LSVehicleServer {

    private static final Logger LOG = LoggerFactory.getLogger(LSDefaultTCPServer.class);

    /**
     * A pool of vehicle controllers.
     */
    private final LocalVehicleControllerPool vehicleControllerPool;
    private boolean initialized;
    ServerSocket serverSocket = null;
    private final TCSObjectService objectService;
    private final TCSObjectPool objectPool;

    @Inject
    public LSDefaultTCPServer(LocalVehicleControllerPool vehicleControllerPool, TCSObjectService objectService, TCSObjectPool objectPool) {
        this.vehicleControllerPool = vehicleControllerPool;
        this.objectService = objectService;
        this.objectPool = objectPool;
    }

    @Override
    public void initialize() {
        if (isInitialized()) {
            return;
        }
        initialized = true;
        try {
            serverSocket = new ServerSocket(8088);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        new Thread(this::run).start();
    }

    private void run() {
        while (true) {
            Socket socket = null;
            StateCommand connectState = null;
            try {
                socket = serverSocket.accept();
                String vehicleIp = socket.getInetAddress().getHostAddress();
                Set<Vehicle> vehicles = objectPool.getObjects(Vehicle.class).stream().filter((vehicle) -> filterVehicleIp(vehicle, vehicleIp)).collect(Collectors.toSet());
                if (vehicles.isEmpty()) {
                    connectState = new StateCommand(StateCommand.State.NO_VEHICLE);
                    throw new RuntimeException("未配置IP为" + vehicleIp + "的车辆");
                }

                if (vehicles.size() > 1) {
                    String vname = "[";
                    for (Vehicle v : vehicles) {
                        vname += (v.getName() + "，");
                    }
                    vname = vname.substring(0, vname.length() - 1) + "]";
                    connectState = new StateCommand(StateCommand.State.DUP_VEHICLE);
                    throw new RuntimeException("车辆IP(" + vehicleIp + ")配置重复：" + vname);
                }

                Vehicle refVehicle = vehicles.stream().findFirst().get();
                synchronized (this) {
                    socketMap.put(refVehicle.getName(), socket);
                }
                connectState = new StateCommand(StateCommand.State.NORMAL);
                connectState.setDescription("车辆接入成功：" + refVehicle.getName());
                LOG.info("车辆接入：{}", refVehicle.getName());
                new Thread(new LSDefaultVehicleProcessModel(refVehicle, socket, objectPool, vehicleControllerPool)).start();
            } catch (IOException e) {
                LOG.error("车辆接入IO异常：", e);
            } catch (RuntimeException e) {
                LOG.error("车辆接入配置异常：", e);
                connectState.setDescription(e.getMessage());
            } catch (Exception e) {
                LOG.error("车辆接入异常：", e);
                connectState = new StateCommand(StateCommand.State.ERROR);
            } finally {
                if (socket != null) {
                    LSDefaultCommand<StateCommand> lsDefaultCommand = new LSDefaultCommand<StateCommand>(LSDefaultConstants.S_COMMAND_TYPE_CONNECT_STATE, connectState);
                    sendCommand(socket, lsDefaultCommand);
                }
                if (!StateCommand.State.NORMAL.getValue().equals(connectState.getCode())) {
                    try {
                        socket.close();
                    } catch (IOException e1) {
                    }
                }
            }
        }
    }


    private boolean filterVehicleIp(Vehicle vehicle, String vehicleIp) {
        return StringUtils.isNotBlank(vehicle.getProperty("IP")) && vehicle.getProperty("IP").trim().equals(vehicleIp.trim());
    }

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }

    @Override
    public void terminate() {
        if (!isInitialized()) {
            return;
        }
        initialized = false;
        socketMap.clear();
        try {
            serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void sendCommd(String vehicleName, LSDefaultCommand cmd) {
        LOG.info("向车辆{}发送指令:{}", vehicleName, JSONObject.toJSONString(cmd));
        Socket socket = socketMap.get(vehicleName);
        if (socket != null) {
            sendCommand(socket, cmd);
        }
    }

    private void sendCommand(Socket socket, LSDefaultCommand cmd) {
        try {
            String msg = JSONObject.toJSONString(cmd);
            OutputStream os = socket.getOutputStream();
            SocketUtil.sendPacketData(os,msg);
        } catch (Exception e) {
            LOG.error("向车辆发送指令异常！", e);
        }
    }

}
