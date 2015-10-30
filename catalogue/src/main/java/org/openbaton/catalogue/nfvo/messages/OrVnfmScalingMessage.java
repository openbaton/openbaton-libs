package org.openbaton.catalogue.nfvo.messages;

import org.openbaton.catalogue.mano.descriptor.VNFComponent;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VNFRecordDependency;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.catalogue.nfvo.messages.Interfaces.OrVnfmMessage;

/**
 * Created by lto on 13/10/15.
 */
public class OrVnfmScalingMessage extends OrVnfmMessage {

    private VNFComponent component;
    private VNFCInstance vnfcInstance;
    private VirtualNetworkFunctionRecord virtualNetworkFunctionRecord;
    private VNFRecordDependency dependency;

    public VNFCInstance getVnfcInstance() {
        return vnfcInstance;
    }

    public void setVnfcInstance(VNFCInstance vnfcInstance) {
        this.vnfcInstance = vnfcInstance;
    }

    public VNFComponent getComponent() {
        return component;
    }

    public void setComponent(VNFComponent component) {
        this.component = component;
    }

    public VirtualNetworkFunctionRecord getVirtualNetworkFunctionRecord() {
        return virtualNetworkFunctionRecord;
    }

    public void setVirtualNetworkFunctionRecord(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        this.virtualNetworkFunctionRecord = virtualNetworkFunctionRecord;
    }

    @Override
    public String toString() {
        return "OrVnfmScalingMessage{" +
                "component=" + component +
                ", virtualNetworkFunctionRecord=" + virtualNetworkFunctionRecord +
                '}';
    }

    public VNFRecordDependency getDependency() {
        return dependency;
    }

    public void setDependency(VNFRecordDependency dependency) {
        this.dependency = dependency;
    }
}
