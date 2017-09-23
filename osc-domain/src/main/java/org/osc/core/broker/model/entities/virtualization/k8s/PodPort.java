/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.model.entities.virtualization.k8s;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;

@SuppressWarnings("serial")
@Entity
@Table(name = "POD_PORT")
public class PodPort extends BaseEntity {
    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(name = "mac_address", nullable = false, unique = true)
    private String macAddress;

    @ElementCollection(fetch = FetchType.LAZY)
    @Column(name = "ip_address")
    @CollectionTable(name = "POD_PORT_IP_ADDRESS", joinColumns = @JoinColumn(name = "pod_port_fk"),
    foreignKey=@ForeignKey(name = "FK_POD_PORT_IP_ADDRESS"))
    private List<String> ipAddresses = new ArrayList<String>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pod_fk", foreignKey = @ForeignKey(name = "FK_PODP_POD"))
    private Pod pod;

    @Column(name = "parent_id", nullable = true, unique = false)
    private String parentId;
}