/*
 * Copyright (c) 2015 Fraunhofer FOKUS
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openbaton.common.vnfm_sdk;

import org.openbaton.catalogue.mano.descriptor.InternalVirtualLink;
import org.openbaton.catalogue.mano.descriptor.VNFComponent;
import org.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.openbaton.catalogue.mano.descriptor.VirtualNetworkFunctionDescriptor;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VNFRecordDependency;
import org.openbaton.catalogue.mano.record.VirtualLinkRecord;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.catalogue.nfvo.Action;
import org.openbaton.catalogue.nfvo.EndpointType;
import org.openbaton.catalogue.nfvo.VnfmManagerEndpoint;
import org.openbaton.catalogue.nfvo.messages.Interfaces.NFVMessage;
import org.openbaton.catalogue.nfvo.messages.OrVnfmErrorMessage;
import org.openbaton.catalogue.nfvo.messages.OrVnfmGenericMessage;
import org.openbaton.catalogue.nfvo.messages.OrVnfmInstantiateMessage;
import org.openbaton.catalogue.nfvo.messages.OrVnfmScalingMessage;
import org.openbaton.common.vnfm_sdk.exception.BadFormatException;
import org.openbaton.common.vnfm_sdk.exception.NotFoundException;
import org.openbaton.common.vnfm_sdk.exception.VnfmSdkException;
import org.openbaton.common.vnfm_sdk.interfaces.VNFLifecycleChangeNotification;
import org.openbaton.common.vnfm_sdk.interfaces.VNFLifecycleManagement;
import org.openbaton.common.vnfm_sdk.utils.VNFRUtils;
import org.openbaton.common.vnfm_sdk.utils.VnfmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by lto on 08/07/15.
 */
public abstract class AbstractVnfm implements VNFLifecycleManagement, VNFLifecycleChangeNotification {

    protected VnfmHelper vnfmHelper;
    protected static String type;
    protected static String endpoint;
    protected static String endpointType;
    protected static Properties properties;
    protected static Logger log = LoggerFactory.getLogger(AbstractVnfm.class);
    protected VnfmManagerEndpoint vnfmManagerEndpoint;
    private ExecutorService executor;

    @PreDestroy
    private void shutdown() {
        this.unregister();
    }

    @PostConstruct
    private void init() {
        setVnfmHelper();
        setup();
        executor = Executors.newFixedThreadPool(Integer.parseInt(properties.getProperty("concurrency", "15")));
    }

    protected abstract void setVnfmHelper();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public abstract void query();

    @Override
    public abstract VirtualNetworkFunctionRecord scale(Action scaleInOrOut, VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFCInstance component, Object scripts, VNFRecordDependency dependency) throws Exception;

    @Override
    public abstract void checkInstantiationFeasibility();

    @Override
    public abstract void heal();

    @Override
    public abstract void updateSoftware();

    @Override
    public abstract VirtualNetworkFunctionRecord modify(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFRecordDependency dependency) throws Exception;

    @Override
    public abstract void upgradeSoftware();

    @Override
    public abstract VirtualNetworkFunctionRecord terminate(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws Exception;

    public abstract void handleError(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord);

    protected void loadProperties() {
        properties = new Properties();
        try {
            properties.load(ClassLoader.getSystemResourceAsStream("conf.properties"));
        } catch (IOException e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage());
        }
        endpoint = (String) properties.get("endpoint");
        type = (String) properties.get("type");
        endpointType = properties.getProperty("endpoint-type", "JMS");
    }

    protected void onAction(NFVMessage message) throws NotFoundException, BadFormatException {
        VirtualNetworkFunctionRecord virtualNetworkFunctionRecord = null;
        try {

            log.debug("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" + message.getAction() + "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            log.trace("VNFM: Received Message: " + message.getAction());
            NFVMessage nfvMessage = null;
            OrVnfmGenericMessage orVnfmGenericMessage = null;
            switch (message.getAction()) {
                case SCALE_OUT:
                    OrVnfmScalingMessage scalingMessage = (OrVnfmScalingMessage) message;
                    virtualNetworkFunctionRecord = scalingMessage.getVirtualNetworkFunctionRecord();
                    VNFCInstance vnfcInstanceToRemove = scalingMessage.getVnfcInstance();

                    virtualNetworkFunctionRecord = this.scale(Action.SCALE_OUT,virtualNetworkFunctionRecord,vnfcInstanceToRemove,null,null);
                    nfvMessage = null;
                    break;
                case SCALE_IN:
                    scalingMessage = (OrVnfmScalingMessage) message;
                    virtualNetworkFunctionRecord = scalingMessage.getVirtualNetworkFunctionRecord();
                    VNFRecordDependency dependency = scalingMessage.getDependency();
                    VNFComponent component = scalingMessage.getComponent();

                    log.trace("HB_VERSION == " + virtualNetworkFunctionRecord.getHb_version());
                    log.info("Adding VNFComponent: " + component);

                    OrVnfmGenericMessage message1 = (OrVnfmGenericMessage) vnfmHelper.sendAndReceive(VnfmUtils.getNfvMessage(Action.SCALING, virtualNetworkFunctionRecord));
                    virtualNetworkFunctionRecord = message1.getVnfr();

                    log.trace("HB_VERSION == " + virtualNetworkFunctionRecord.getHb_version());

                    boolean found = false;
                    VNFCInstance vnfcInstance_new = null;
                    for (VirtualDeploymentUnit virtualDeploymentUnit : virtualNetworkFunctionRecord.getVdu()) {
                        for (VNFCInstance vnfcInstance : virtualDeploymentUnit.getVnfc_instance()) {
                            if (vnfcInstance.getVnfComponent().getId().equals(component.getId())) {
                                vnfcInstance_new = vnfcInstance;
                                fillProvidesVNFC(virtualNetworkFunctionRecord, vnfcInstance);
                                found = true;
                                log.debug("VNFComponentInstance FOUND : " + vnfcInstance_new.getVnfComponent());
                                break;
                            }
                        }
                        if (found)
                            break;
                    }
                    if (vnfcInstance_new == null){
                        throw new RuntimeException("no new VNFCInstance found. This should not happen...");
                    }
                    checkEMS(vnfcInstance_new.getHostname());
                    Object scripts;
                    if (virtualNetworkFunctionRecord.getVnfPackage().getScriptsLink() != null)
                        scripts = virtualNetworkFunctionRecord.getVnfPackage().getScriptsLink();
                    else
                        scripts = virtualNetworkFunctionRecord.getVnfPackage().getScripts();
                    nfvMessage = VnfmUtils.getNfvMessageScaled(Action.SCALED, this.scale(Action.SCALE_IN, virtualNetworkFunctionRecord, vnfcInstance_new, scripts, dependency), vnfcInstance_new);
                    break;
                case SCALING:
                    break;
                case ERROR:
                    OrVnfmErrorMessage errorMessage = (OrVnfmErrorMessage) message;
                    log.error("ERROR Received: " + errorMessage.getMessage());
                    handleError(errorMessage.getVnfr());

                    nfvMessage = null;
                    break;
                case MODIFY:
                    orVnfmGenericMessage = (OrVnfmGenericMessage) message;
                    nfvMessage = VnfmUtils.getNfvMessage(Action.MODIFY, this.modify(orVnfmGenericMessage.getVnfr(), orVnfmGenericMessage.getVnfrd()));
                    break;
                case RELEASE_RESOURCES:
                    orVnfmGenericMessage = (OrVnfmGenericMessage) message;
                    nfvMessage = VnfmUtils.getNfvMessage(Action.RELEASE_RESOURCES, this.terminate(orVnfmGenericMessage.getVnfr()));
                    break;
                case INSTANTIATE:
                    OrVnfmInstantiateMessage orVnfmInstantiateMessage = (OrVnfmInstantiateMessage) message;
                    virtualNetworkFunctionRecord = createVirtualNetworkFunctionRecord(orVnfmInstantiateMessage.getVnfd(), orVnfmInstantiateMessage.getVnfdf().getFlavour_key(), orVnfmInstantiateMessage.getVnfd().getName(), orVnfmInstantiateMessage.getVlrs(), orVnfmInstantiateMessage.getExtention());
                    GrantOperation grantOperation = new GrantOperation();
                    grantOperation.setVirtualNetworkFunctionRecord(virtualNetworkFunctionRecord);
                    Future<VirtualNetworkFunctionRecord> result = executor.submit(grantOperation);
                    virtualNetworkFunctionRecord = result.get();


                    if (properties.getProperty("allocate", "true").equalsIgnoreCase("true")) {
                        AllocateResources allocateResources = new AllocateResources();
                        allocateResources.setVirtualNetworkFunctionRecord(virtualNetworkFunctionRecord);
                        virtualNetworkFunctionRecord = executor.submit(allocateResources).get();
                    }
                    setupProvides(virtualNetworkFunctionRecord);

                    for (VirtualDeploymentUnit virtualDeploymentUnit : virtualNetworkFunctionRecord.getVdu())
                        for (VNFCInstance vnfcInstance : virtualDeploymentUnit.getVnfc_instance())
                            checkEMS(vnfcInstance.getHostname());

                    if (orVnfmInstantiateMessage.getScriptsLink() != null)
                        virtualNetworkFunctionRecord = instantiate(virtualNetworkFunctionRecord, orVnfmInstantiateMessage.getScriptsLink());
                    else
                        virtualNetworkFunctionRecord = instantiate(virtualNetworkFunctionRecord, orVnfmInstantiateMessage.getScripts());
                    nfvMessage = VnfmUtils.getNfvMessage(Action.INSTANTIATE, virtualNetworkFunctionRecord);
                    break;
                case RELEASE_RESOURCES_FINISH:
                    break;
                case INSTANTIATE_FINISH:
                    break;
                case CONFIGURE:
                    orVnfmGenericMessage = (OrVnfmGenericMessage) message;
                    nfvMessage = VnfmUtils.getNfvMessage(Action.CONFIGURE, configure(orVnfmGenericMessage.getVnfr()));
                    break;
                case START:
                    orVnfmGenericMessage = (OrVnfmGenericMessage) message;
                    nfvMessage = VnfmUtils.getNfvMessage(Action.START, start(orVnfmGenericMessage.getVnfr()));
                    break;
            }

            log.debug("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            if (nfvMessage != null) {
                log.debug("send to NFVO");
                vnfmHelper.sendToNfvo(nfvMessage);
            }
        } catch (Exception e) {
            log.error("ERROR: ", e);
            if (e instanceof VnfmSdkException){
                VnfmSdkException vnfmSdkException = (VnfmSdkException) e;
                if (vnfmSdkException.getVnfr() != null){
                    log.debug("sending vnfr with version: " + vnfmSdkException.getVnfr().getHb_version());
                    vnfmHelper.sendToNfvo(VnfmUtils.getNfvMessage(Action.ERROR, vnfmSdkException.getVnfr()));
                    return;
                }
            }
            vnfmHelper.sendToNfvo(VnfmUtils.getNfvMessage(Action.ERROR, virtualNetworkFunctionRecord));
        }
    }

    class GrantOperation implements Callable<VirtualNetworkFunctionRecord> {
        public VirtualNetworkFunctionRecord getVirtualNetworkFunctionRecord() {
            return virtualNetworkFunctionRecord;
        }

        public void setVirtualNetworkFunctionRecord(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
            this.virtualNetworkFunctionRecord = virtualNetworkFunctionRecord;
        }

        private VirtualNetworkFunctionRecord virtualNetworkFunctionRecord;

        private VirtualNetworkFunctionRecord grantLifecycleOperation() throws VnfmSdkException {
            NFVMessage response;
            try {
                response = vnfmHelper.sendAndReceive(VnfmUtils.getNfvMessage(Action.GRANT_OPERATION, virtualNetworkFunctionRecord));
            } catch (Exception e) {
                throw new VnfmSdkException("Not able to grant operation", e);
            }
            log.debug("" + response);
            if (response.getAction().ordinal() == Action.ERROR.ordinal()) {
                throw new VnfmSdkException("Not able to grant operation because: " + ((OrVnfmErrorMessage) response).getMessage(), ((OrVnfmErrorMessage) response).getVnfr());
            }
            OrVnfmGenericMessage orVnfmGenericMessage = (OrVnfmGenericMessage) response;
            return orVnfmGenericMessage.getVnfr();
        }

        @Override
        public VirtualNetworkFunctionRecord call() throws Exception {
            return this.grantLifecycleOperation();
        }
    }

    class AllocateResources implements Callable<VirtualNetworkFunctionRecord> {
        private VirtualNetworkFunctionRecord virtualNetworkFunctionRecord;
        public VirtualNetworkFunctionRecord getVirtualNetworkFunctionRecord() {
            return virtualNetworkFunctionRecord;
        }

        public void setVirtualNetworkFunctionRecord(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
            this.virtualNetworkFunctionRecord = virtualNetworkFunctionRecord;
        }


        public VirtualNetworkFunctionRecord allocateResources() throws VnfmSdkException {
            NFVMessage response;
            try {
                response = vnfmHelper.sendAndReceive(VnfmUtils.getNfvMessage(Action.ALLOCATE_RESOURCES, virtualNetworkFunctionRecord));
            } catch (Exception e) {
                log.error("" + e.getMessage());
                throw new VnfmSdkException("Not able to allocate Resources", e);
            }
            if (response.getAction().ordinal() == Action.ERROR.ordinal()) {
                OrVnfmErrorMessage errorMessage = (OrVnfmErrorMessage) response;
                log.error(errorMessage.getMessage());
                throw new VnfmSdkException("Not able to allocate Resources because: " + errorMessage.getMessage(), errorMessage.getVnfr());
            }
            OrVnfmGenericMessage orVnfmGenericMessage = (OrVnfmGenericMessage) response;
            log.debug("Received from ALLOCATE: " + orVnfmGenericMessage.getVnfr());
            return orVnfmGenericMessage.getVnfr();
        }

        @Override
        public VirtualNetworkFunctionRecord call() throws Exception {
            return this.allocateResources();
        }
    }

    private void checkEMS(String vduHostname) {
        int i = 0;
        while (true) {
            log.debug("Waiting for ems to be started... (" + i * 5 + " secs)");
            i++;
            try {
                checkEmsStarted(vduHostname);
                break;
            } catch (RuntimeException e) {
                if (i == 100) {
                    throw e;
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    protected abstract void checkEmsStarted(String vduHostname);

    private void setupProvides(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
    }

    private void fillProvidesVNFC(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFCInstance vnfcInstance) {
    }

    /**
     * This method needs to set all the parameter specified in the VNFDependency.parameters
     *
     * @param virtualNetworkFunctionRecord
     */
    protected void fillSpecificProvides(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
    }

    /**
     * This method can be overwritten in case you want a specific initialization of the VirtualNetworkFunctionRecord from the VirtualNetworkFunctionDescriptor
     *
     * @param virtualNetworkFunctionDescriptor
     * @param extension
     * @return The new VirtualNetworkFunctionRecord
     * @throws BadFormatException
     * @throws NotFoundException
     */
    protected VirtualNetworkFunctionRecord createVirtualNetworkFunctionRecord(VirtualNetworkFunctionDescriptor virtualNetworkFunctionDescriptor, String flavourId, String vnfInstanceName, Set<VirtualLinkRecord> virtualLinkRecords, Map<String, String> extension) throws BadFormatException, NotFoundException {
        try {
            VirtualNetworkFunctionRecord virtualNetworkFunctionRecord = VNFRUtils.createVirtualNetworkFunctionRecord(virtualNetworkFunctionDescriptor, flavourId, extension.get("nsr-id"), virtualLinkRecords);
            for (InternalVirtualLink internalVirtualLink : virtualNetworkFunctionRecord.getVirtual_link()) {
                for (VirtualLinkRecord virtualLinkRecord : virtualLinkRecords) {
                    if (internalVirtualLink.getName().equals(virtualLinkRecord.getName())) {
                        internalVirtualLink.setExtId(virtualLinkRecord.getExtId());
                        internalVirtualLink.setConnectivity_type(virtualLinkRecord.getConnectivity_type());
                    }
                }
            }
            log.debug("Created VirtualNetworkFunctionRecord: " + virtualNetworkFunctionRecord);
            return virtualNetworkFunctionRecord;
        } catch (NotFoundException e) {
            e.printStackTrace();
            vnfmHelper.sendToNfvo(VnfmUtils.getNfvMessage(Action.ERROR, null));
            throw e;
        } catch (BadFormatException e) {
            e.printStackTrace();
            vnfmHelper.sendToNfvo(VnfmUtils.getNfvMessage(Action.ERROR, null));
            throw e;
        }
    }

    public abstract VirtualNetworkFunctionRecord start(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws Exception;

    public abstract VirtualNetworkFunctionRecord configure(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws Exception;

    /**
     * This method unregister the VNFM in the NFVO
     */
    protected abstract void unregister();

    /**
     * This method register the VNFM to the NFVO sending the right endpoint
     */
    protected abstract void register();

    /**
     * This method setups the VNFM and then register it to the NFVO. We recommend to not change this method or at least
     * to override calling super()
     */
    protected void setup() {
        loadProperties();
        vnfmManagerEndpoint = new VnfmManagerEndpoint();
        vnfmManagerEndpoint.setType(this.type);
        vnfmManagerEndpoint.setEndpoint(this.endpoint);
        log.debug("creating VnfmManagerEndpoint for vnfm endpointType: " + this.endpointType);
        vnfmManagerEndpoint.setEndpointType(EndpointType.valueOf(this.endpointType));
        register();
    }
}
