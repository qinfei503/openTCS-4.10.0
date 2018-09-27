package org.opentcs.actualvehicle;

import com.google.inject.assistedinject.Assisted;
import org.opentcs.access.to.LSVehicleServer;
import org.opentcs.constants.LSDefaultConstants;
import org.opentcs.data.model.Vehicle;
import org.opentcs.drivers.vehicle.*;
import org.opentcs.drivers.vehicle.commands.LSDefaultCommand;
import org.opentcs.util.CyclicTask;
import org.opentcs.util.ExplainedBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class LSDefaultCommAdapter extends BasicVehicleCommAdapter  implements SimVehicleCommAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(LSDefaultCommAdapter.class);
    private boolean initialized;
    private Vehicle vehicle;
    private LSVehicleServer lsVehicleServer;
    private CyclicTask lsDefaultCommandDispatcherTask;
    /**
     * The boolean flag to check if execution of the next command is allowed.
     */
    private boolean singleStepExecutionAllowed;
    /**
     * The adapter components factory.
     */
    private final LSDefaultAdapterComponentsFactory componentsFactory;
    private volatile boolean commandFinished;

    @Inject
    public LSDefaultCommAdapter(@Assisted Vehicle vehicle, LSVehicleServer lsVehicleServer, LSDefaultAdapterComponentsFactory componentsFactory){
        super(new VehicleProcessModel(vehicle), 1, 1, "CHARGE");
        this.vehicle = vehicle;
        this.lsVehicleServer = lsVehicleServer;
        this.componentsFactory = componentsFactory;
    }

    @Override
    public void initialize() {
        if (isInitialized()) {
            return;
        }
        super.initialize();
        getProcessModel().setVehicleState(Vehicle.State.IDLE);
        t_enable();
        lsVehicleServer.sendCommd(vehicle.getName(), new LSDefaultCommand<>(LSDefaultConstants.S_COMMAND_TYPE_TERMINATE, new Object()));
        initialized = true;
    }
    @Override
    public synchronized void enable() {}
    private synchronized void t_enable() {
        if (isEnabled()) {
            return;
        }
        lsDefaultCommandDispatcherTask = new LSDefaultCommandDispatcherTask();
        Thread thread = new Thread(lsDefaultCommandDispatcherTask, getName() + "-actionTask");
        thread.start();
        super.enable();
    }
    @Override
    public synchronized void disable() {}
    private synchronized void t_disable() {
        if (!isEnabled()) {
            return;
        }
        // Disable vehicle simulation.
        lsDefaultCommandDispatcherTask.terminate();
        lsDefaultCommandDispatcherTask = null;
        super.disable();
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void terminate() {
        if (!isInitialized()) {
            return;
        }
        super.terminate();
        t_disable();
        initialized = false;
        lsVehicleServer.sendCommd(vehicle.getName(), new LSDefaultCommand<>(LSDefaultConstants.S_COMMAND_TYPE_TERMINATE, new Object()));
    }
    @Override
    public void sendCommand(MovementCommand cmd) throws IllegalArgumentException {
        requireNonNull(cmd, "cmd");
        // Reset the execution flag for single-step mode.
        singleStepExecutionAllowed = false;
        // Don't do anything else - the command will be put into the sentQueue
        // automatically, where it will be picked up by the simulation task.
    }


    @Override
    protected synchronized boolean canSendNextCommand() {
        return super.canSendNextCommand();
    }
    @Override
    protected void connectVehicle() {

    }

    @Override
    protected void disconnectVehicle() {

    }

    @Override
    protected boolean isVehicleConnected() {
        return true;
    }



    @Override
    public VehicleProcessModel getProcessModel() {
        return  super.getProcessModel();
    }
    @Override
    @Deprecated
    protected List<VehicleCommAdapterPanel> createAdapterPanels() {
        return new ArrayList<VehicleCommAdapterPanel>();
    }

    @Nonnull
    @Override
    public ExplainedBoolean canProcess(@Nonnull List<String> operations) {
        return new ExplainedBoolean(true,"");
    }

    @Override
    public void processMessage(@Nullable Object message) {

    }

    @Override
    public void initVehiclePosition(@Nullable String newPos) {

    }
    /**
     * Triggers a step in single step mode.
     */
    public synchronized void trigger() {
        singleStepExecutionAllowed = true;
    }
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if(evt.getPropertyName().equals(LSDefaultConstants.EVENT_NAME_COMMANDFINISHED)){
            synchronized (this){
//                System.out.println("adapter in commAdapter"+this);
                this.commandFinished=true;
            }
        }
    }

    @Override
    public void abortAssignedOrder() {
        LSDefaultCommand<String> lsDefaultCommand = new LSDefaultCommand<String>(LSDefaultConstants.S_COMMAND_TYPE_ABORT,null);
        lsVehicleServer.sendCommd(vehicle.getName(),lsDefaultCommand);
    }

    /**
     * A task do a vehicle's behaviour.
     */
    private class LSDefaultCommandDispatcherTask
            extends CyclicTask {

        /**
         * Creates a new LSDefaultCommandDispatcherTask.
         */
        private LSDefaultCommandDispatcherTask() {
            super(10);
        }

        @Override
        protected void runActualTask() {
            final MovementCommand curCommand;
            synchronized (LSDefaultCommAdapter.this) {
                curCommand = getSentQueue().peek();
            }
            if (curCommand != null) {
                // If we were told to move somewhere, simulate the journey.
                LOG.debug("Processing MovementCommand...");
                synchronized (LSDefaultCommAdapter.this){
                    LSDefaultCommAdapter.this.commandFinished=false;
                }
//                doMovement(stepList);
                while(!LSDefaultCommAdapter.this.commandFinished){
                 /*System.out.println("====wait for finished");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }*/
                }
                LOG.info("commandExecuted:{}",curCommand.getStep().getDestinationPoint());
                LOG.debug("Processed MovementCommand.");
                if (!isTerminated()) {
                    if (getSentQueue().size() <= 1 && getCommandQueue().isEmpty()) {
                        getProcessModel().setVehicleState(Vehicle.State.IDLE);
                    }
                    // Update GUI.
                    synchronized (LSDefaultCommAdapter.this) {
                        MovementCommand sentCmd = getSentQueue().poll();
                        // If the command queue was cleared in the meantime, the kernel
                        // might be surprised to hear we executed a command we shouldn't
                        // have, so we only peek() at the beginning of this method and
                        // poll() here. If sentCmd is null, the queue was probably cleared
                        // and we shouldn't report anything back.
                        if (sentCmd != null && sentCmd.equals(curCommand)) {
                            // Let the vehicle manager know we've finished this command.
                            getProcessModel().commandExecuted(curCommand);
                            LSDefaultCommAdapter.this.notify();
                        }
                    }
                }
            }
        }

        /**
         *
         * @param cmds A step
         * @throws InterruptedException If an exception occured while sumulating
         */
        private void doMovement(List<MovementCommand> cmds) {
            getProcessModel().setVehicleState(Vehicle.State.EXECUTING);
        }

        /**
         * Simulates an operation.
         *
         * @param operation A operation
         * @throws InterruptedException If an exception occured while simulating
         */
        private void doOperation(String operation) {}
    }
}
