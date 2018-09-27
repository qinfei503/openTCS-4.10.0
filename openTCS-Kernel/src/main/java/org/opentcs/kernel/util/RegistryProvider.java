/**
 * Copyright (c) The openTCS Authors.
 *
 * This program is free software and subject to the MIT license. (For details,
 * see the licensing information (LICENSE.txt) you should have received with
 * this copy of the software.)
 */
package org.opentcs.kernel.util;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.opentcs.access.rmi.factories.SocketFactoryProvider;
import org.opentcs.components.Lifecycle;
import org.opentcs.constants.LSDefaultConstants;
import org.opentcs.kernel.extensions.rmi.RmiKernelInterfaceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the one {@link Registry} instance used for RMI communication.
 *
 * @author Martin Grzenia (Fraunhofer IML)
 */
public class RegistryProvider
    implements Lifecycle {

  /**
   * This class' logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(RegistryProvider.class);
  /**
   * Provides socket factories used to create RMI registries.
   */
  private final SocketFactoryProvider socketFactoryProvider;
  /**
   * This class' configuration.
   */
  private final RmiKernelInterfaceConfiguration configuration;
  /**
   * The actual registry instance.
   */
  private Registry registry;
  /**
   * Whether this provider is initialized or not.
   */
  private boolean initialized;

  private UDPServer udpServer = new UDPServer();
    private static final ResourceBundle BUNDLE
            = ResourceBundle.getBundle("org/opentcs/util/Bundle");
  /**
   * Creates a new instance.
   *
   * @param socketFactoryProvider The socket factory provider used for RMI.
   * @param configuration This class' configuration.
   */
  @Inject
  public RegistryProvider(@Nonnull SocketFactoryProvider socketFactoryProvider,
                          @Nonnull RmiKernelInterfaceConfiguration configuration) {
    this.socketFactoryProvider = requireNonNull(socketFactoryProvider, "socketFactoryProvider");
    this.configuration = requireNonNull(configuration, "configuration");
  }

  @Override
  public void initialize() {
    if (isInitialized()) {
      LOG.debug("Already initialized.");
      return;
    }

    installRegistry();
    new Thread(udpServer::initUDPServer).start();
    initialized = true;
  }

  @Override
  public boolean isInitialized() {
    return initialized;
  }

  @Override
  public void terminate() {
    if (!isInitialized()) {
      LOG.debug("Not initialized.");
      return;
    }

    registry = null;
    udpServer.close();
    initialized = false;
  }

  @Nonnull
  public Registry get() {
    return registry;
  }

  private void installRegistry() {
    try {
      LOG.debug("Trying to create a local registry...");
      registry = LocateRegistry.createRegistry(configuration.registryPort(),
                                               socketFactoryProvider.getClientSocketFactory(),
                                               socketFactoryProvider.getServerSocketFactory());
      // Make sure the registry is running
      registry.list();
    }
    catch (RemoteException ex) {
      LOG.error("Couldn't create a working local registry.");
      registry = null;
      throw new RuntimeException(ex);
    }
  }


  class UDPServer {
    volatile DatagramSocket serverSocket =null;
    public  void initUDPServer()  {
      try{
        ResourceBundle bundle
                = ResourceBundle.getBundle("org/opentcs/kernel/distribution/config/opentcs-kernel-defaults-baseline");
        serverSocket = new DatagramSocket(Integer.valueOf(bundle.getString("selectConnectServerDialog.UDPServer.port")));
        byte[] rcv_arr = new byte[1024];
        DatagramPacket packet = new DatagramPacket(rcv_arr, rcv_arr.length);
        while(serverSocket!=null && !serverSocket.isClosed()){
          serverSocket.receive(packet);
          packet.getData();
          if(LSDefaultConstants.BROADCAST_TYPE_CHECKRMISERVER.equals(new String(rcv_arr).trim())){
              InetAddress address =packet.getAddress();
//              System.out.println(new String(rcv_arr));
              byte[] send_arr=LSDefaultConstants.BROADCAST_RESULT_RMISERVER.getBytes();
              DatagramPacket packet1 = new DatagramPacket(send_arr, send_arr.length, address, Integer.valueOf(bundle.getString("selectConnectServerDialog.UDPClient.port")));
              serverSocket.send(packet1);
          }
        }
      }catch (Exception e){
        throw new RuntimeException(e);
      }

    }

    private void close(){
      if(this.serverSocket!=null){
        this.serverSocket.close();
      }
    }

  }
}
