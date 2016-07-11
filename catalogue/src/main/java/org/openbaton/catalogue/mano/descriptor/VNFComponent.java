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

package org.openbaton.catalogue.mano.descriptor;

import org.openbaton.catalogue.util.IdGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by lto on 06/02/15.
 *
 * Based on ETSI GS NFV-MAN 001 V1.1.1 (2014-12)
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class VNFComponent implements Serializable {
    /**
     * Unique VNFC identification within the namespace of a specific VNF.
     */
    @Id
    protected String id;
    @Version
    protected int version = 0;

    /**
     * Describes network connectivity between a VNFC instance (based on this VDU) and an internal Virtual Link.
     */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("orderId ASC")
    protected List<VNFDConnectionPoint> connection_point;

    public VNFComponent() {
        this.connection_point = new ArrayList<>();
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @PrePersist
    public void ensureId() {
        id = IdGenerator.createUUID();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<VNFDConnectionPoint> getConnection_point() {
        return connection_point;
    }

    public void setConnection_point(List<VNFDConnectionPoint> connection_point) {
        int i = 1;
        for (VNFDConnectionPoint cp : connection_point){
            cp.setOrderId(i);
            i+=1;
        }
        this.connection_point = connection_point;
    }

    @Override
    public String toString() {
        return "VNFComponent{" +
                "connection_point=" + connection_point +
                ", id='" + id + '\'' +
                ", version=" + version +
                '}';
    }
}
