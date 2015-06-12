package org.project.neutrino.nfvo.core.core;

import org.project.neutrino.nfvo.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.project.neutrino.nfvo.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.neutrino.nfvo.catalogue.nfvo.Server;
import org.project.neutrino.nfvo.catalogue.nfvo.VimInstance;
import org.project.neutrino.nfvo.vim_interfaces.VimBroker;
import org.project.neutrino.nfvo.common.exceptions.VimException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by lto on 11/06/15.
 */
@Service
@Scope
public class ResourceManagement implements org.project.neutrino.nfvo.core.interfaces.ResourceManagement {
    @Autowired
    private VimBroker<org.project.neutrino.nfvo.vim_interfaces.ResourceManagement> vimBroker;

    @Override
    public Future<String> allocate(VirtualDeploymentUnit virtualDeploymentUnit, VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws VimException {
        org.project.neutrino.nfvo.vim_interfaces.ResourceManagement vim;
        vim = vimBroker.getVim(virtualDeploymentUnit.getVimInstance().getType());
        return vim.allocate(virtualDeploymentUnit,virtualNetworkFunctionRecord);
    }

    @Override
    public List<Server> query(VimInstance vimInstance) throws VimException {
        return vimBroker.getVim(vimInstance.getType()).queryResources(vimInstance);
    }

    @Override
    public void update(VirtualDeploymentUnit vdu) {

    }

    @Override
    public void scale(VirtualDeploymentUnit vdu) {

    }

    @Override
    public void migrate(VirtualDeploymentUnit vdu) {

    }

    @Override
    public void operate(VirtualDeploymentUnit vdu, String operation) {

    }

    @Override
    public void release(VirtualDeploymentUnit virtualDeploymentUnit) throws VimException {
        org.project.neutrino.nfvo.vim_interfaces.ResourceManagement vim = vimBroker.getVim(virtualDeploymentUnit.getVimInstance().getType());
        vim.release(virtualDeploymentUnit);
    }

    @Override
    public void createReservation(VirtualDeploymentUnit vdu) {

    }

    @Override
    public void queryReservation() {

    }

    @Override
    public void updateReservation(VirtualDeploymentUnit vdu) {

    }

    @Override
    public void releaseReservation(VirtualDeploymentUnit vdu) {

    }
}
