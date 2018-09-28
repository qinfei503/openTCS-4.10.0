package org.opentcs.kernel.extensions.controlcenter.vehicles;

import com.alibaba.fastjson.JSONObject;
import org.opentcs.access.to.LSVehicleServer;
import org.opentcs.constants.LSDefaultConstants;
import org.opentcs.data.model.Point;
import org.opentcs.data.model.Vehicle;
import org.opentcs.drivers.vehicle.VehicleController;
import org.opentcs.drivers.vehicle.VehicleProcessModel;
import org.opentcs.drivers.vehicle.commands.LSDefaultCommand;
import org.opentcs.drivers.vehicle.commands.VehicleInfoCommand;
import org.opentcs.kernel.vehicles.LocalVehicleControllerPool;
import org.opentcs.kernel.workingset.TCSObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Set;

public class LSDefaultVehicleProcessModel extends VehicleProcessModel implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(LSDefaultVehicleProcessModel.class);
    private Socket socket;
    private final TCSObjectPool objectPool;
    private final LocalVehicleControllerPool vehicleControllerPool;
    private VehicleController vehicleController;
    private Set<Point> points;

    /**
     * Creates a new instance.
     *
     * @param attachedVehicle       The vehicle attached to the new instance.
     * @param socket
     * @param objectPool
     * @param vehicleControllerPool
     */
    public LSDefaultVehicleProcessModel(@Nonnull Vehicle attachedVehicle, Socket socket, TCSObjectPool objectPool, LocalVehicleControllerPool vehicleControllerPool) {
        super(attachedVehicle);
        this.socket = socket;
        this.objectPool = objectPool;
        this.vehicleControllerPool = vehicleControllerPool;
        this.vehicleController = vehicleControllerPool.getVehicleController(attachedVehicle.getName());
        getPropertyChangeSupport().addPropertyChangeListener((PropertyChangeListener) vehicleController.getCommAdapter());
//        System.out.println("adapter in vehicleModel"+vehicleController.getCommAdapter());
//        this.points =objectPool.getObjects(Point.class);
    }

    @Override
    public void run() {
        BufferedReader bf = null;
        try {
            InputStream is = socket.getInputStream();
            bf = new BufferedReader(new InputStreamReader(is));
            String msg = "";
            while ((msg = bf.readLine()) != null) {
                try {
                    receiveMsg(msg.trim());
                } catch (Exception e) {
                    LOG.error(e.getMessage());
                }

            }
        } catch (IOException e) {
        } catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            synchronized (this) {
                LSVehicleServer.socketMap.remove(vehicle.getName());
            }
            if (bf != null) {
                try {
                    bf.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void receiveMsg(String msg) {
//        JSONObject json = JSONObject.parseObject(msg.trim());
        LSDefaultCommand cmd = JSONObject.parseObject(msg, LSDefaultCommand.class);
        String type = cmd.getType();
        switch (type) {
            case LSDefaultConstants.R_COMMAND_TYPE_UPDATE_PRECISEPOSITION: {
                VehicleInfoCommand vehicleInfoCommand = JSONObject.parseObject(cmd.getMessage().toString(), VehicleInfoCommand.class);
                vehicleController.updateVehiclePrecisePosition(vehicleInfoCommand.getPrecisePosition());
//                setVehiclePrecisePosition(new Triple(json.getLong("x-pos"),json.getLong("y-pos"),json.getLong("z-pos")));
                break;
            }
            case LSDefaultConstants.R_COMMAND_TYPE_UPDATE_POSITION:{
                VehicleInfoCommand vehicleInfoCommand = JSONObject.parseObject(cmd.getMessage().toString(), VehicleInfoCommand.class);
                vehicleController.updateVehiclePrecisePosition(null);
                vehicleController.setVehiclePosition(vehicleInfoCommand.getPosition());
                break;
            }
            case LSDefaultConstants.R_COMMAND_TYPE_UPDATE_STATE:{
                break;
            }
            case LSDefaultConstants.R_COMMAND_TYPE_CMD_EXCUTED:{
                getPropertyChangeSupport().firePropertyChange(LSDefaultConstants.EVENT_NAME_COMMANDFINISHED, null, true);
                break;
            }
        }
    }

}
