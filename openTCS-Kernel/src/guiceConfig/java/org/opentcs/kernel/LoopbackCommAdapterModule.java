/**
 * Copyright (c) The openTCS Authors.
 *
 * This program is free software and subject to the MIT license. (For details,
 * see the licensing information (LICENSE.txt) you should have received with
 * this copy of the software.)
 */
package org.opentcs.kernel;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.opentcs.access.to.LSVehicleServer;
import org.opentcs.actualvehicle.LSDefaultAdapterComponentsFactory;
import org.opentcs.actualvehicle.LSDefaultCommunicationAdapterFactory;
import org.opentcs.customizations.kernel.KernelInjectionModule;
import org.opentcs.kernel.extensions.controlcenter.vehicles.LSDefaultTCPServer;
import org.opentcs.virtualvehicle.LoopbackAdapterComponentsFactory;
import org.opentcs.virtualvehicle.LoopbackCommunicationAdapterFactory;
import org.opentcs.virtualvehicle.VirtualVehicleConfiguration;

import javax.inject.Singleton;

/**
 * Configures/binds the loopback communication adapters of the openTCS kernel.
 *
 * @author Stefan Walter (Fraunhofer IML)
 */
public class LoopbackCommAdapterModule
    extends KernelInjectionModule {

  // tag::documentation_createCommAdapterModule[]
  @Override
  protected void configure() {
    configureLoopbackAdapterDependencies();
    vehicleCommAdaptersBinder().addBinding().to(LoopbackCommunicationAdapterFactory.class);
    vehicleCommAdaptersBinder().addBinding().to(LSDefaultCommunicationAdapterFactory.class);
  }
  // end::documentation_createCommAdapterModule[]

  private void configureLoopbackAdapterDependencies() {
    install(new FactoryModuleBuilder().build(LoopbackAdapterComponentsFactory.class));
    install(new FactoryModuleBuilder().build(LSDefaultAdapterComponentsFactory.class));

    bind(VirtualVehicleConfiguration.class)
        .toInstance(getConfigBindingProvider().get(VirtualVehicleConfiguration.PREFIX,
                                                   VirtualVehicleConfiguration.class));
    bind(LSVehicleServer.class).to(LSDefaultTCPServer.class).in(Singleton.class);
  }
}
