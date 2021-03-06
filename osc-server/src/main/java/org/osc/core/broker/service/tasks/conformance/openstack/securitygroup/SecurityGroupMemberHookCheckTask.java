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
package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.sfc.SfcFlowClassifierCreateTask;
import org.osc.core.broker.service.tasks.conformance.openstack.sfc.SfcFlowClassifierDeleteTask;
import org.osc.core.broker.service.tasks.conformance.openstack.sfc.SfcFlowClassifierUpdateTask;
import org.osc.core.common.job.TaskGuard;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.InspectionHookElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This task is responsible for checking the conformance of the inspection appliances
 * assigned to a security group member. If the related SDN controller
 * does not support port group it will also create the inspection hook for each VM port
 * of the security group member.
 */
// TODO emanoel: Consider renaming the task to SecurityGroupMemberCheckAppliancesTask since
// it does more than checking the inspection hook.
@Component(service=SecurityGroupMemberHookCheckTask.class)
public class SecurityGroupMemberHookCheckTask extends TransactionalMetaTask {

    private final Logger log = LoggerFactory.getLogger(SecurityGroupMemberHookCheckTask.class);

    @Reference
    VmPortAllHooksRemoveTask vmPortAllHooksRemoveTask;

    @Reference
    VmPortDeleteFromDbTask vmPortDeleteFromDbTask;

    @Reference
    VmPortHookCheckTask vmPortHookCheckTask;

    @Reference
    SfcFlowClassifierCreateTask sfcFlowClassifierCreateTask;

    @Reference
    SfcFlowClassifierUpdateTask sfcFlowClassifierUpdateTask;

    @Reference
    SfcFlowClassifierDeleteTask sfcFlowClassifierDeleteTask;

    @Reference
    ApiFactoryService apiFactoryService;

    private TaskGraph tg;
    private SecurityGroupMember sgm;
    private VmDiscoveryCache vdc;

    public SecurityGroupMemberHookCheckTask create(SecurityGroupMember sgm, VmDiscoveryCache vdc) {
        SecurityGroupMemberHookCheckTask task = new SecurityGroupMemberHookCheckTask();
        task.sgm = sgm;
        task.vdc = vdc;
        task.vmPortAllHooksRemoveTask = this.vmPortAllHooksRemoveTask;
        task.vmPortDeleteFromDbTask = this.vmPortDeleteFromDbTask;
        task.vmPortHookCheckTask = this.vmPortHookCheckTask;

        task.sfcFlowClassifierCreateTask = this.sfcFlowClassifierCreateTask;
        task.sfcFlowClassifierUpdateTask = this.sfcFlowClassifierUpdateTask;
        task.sfcFlowClassifierDeleteTask = this.sfcFlowClassifierDeleteTask;

        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();
        this.sgm = em.find(SecurityGroupMember.class, this.sgm.getId());

        SecurityGroup sg = this.sgm.getSecurityGroup();

        this.log.info("Checking Inspection Hooks for Security group Member: " + this.sgm.getMemberName());

        Set<VMPort> ports = this.sgm.getVmPorts();
        try (SdnRedirectionApi redirApi = this.apiFactoryService
                .createNetworkRedirectionApi(sg.getVirtualizationConnector())) {

            boolean supportsNeutronSFC = this.apiFactoryService.supportsNeutronSFC(sg);
            for (VMPort port : ports) {
                if (port.getMarkedForDeletion()) {
                    handlePortDelete(sg, supportsNeutronSFC, port);
                } else if (supportsNeutronSFC) {
                    checkSfcHooks(sg, redirApi, port);
                } else {
                    for (SecurityGroupInterface sgi : sg.getSecurityGroupInterfaces()) {
                        if (!sgi.getMarkedForDeletion()) {
                            this.tg.appendTask(this.vmPortHookCheckTask.create(this.sgm, sgi, port, this.vdc),
                                    TaskGuard.ALL_PREDECESSORS_COMPLETED);
                        }
                    }
                }
            }
        }
    }

    private void handlePortDelete(SecurityGroup sg, boolean supportsNeutronSFC, VMPort port) {
        if (supportsNeutronSFC) {
            this.tg.appendTask(this.sfcFlowClassifierDeleteTask.create(sg, port));
        } else {
            this.tg.appendTask(this.vmPortAllHooksRemoveTask.create(this.sgm, port));
        }
        this.tg.appendTask(this.vmPortDeleteFromDbTask.create(this.sgm, port));
    }

    private void checkSfcHooks(SecurityGroup sg, SdnRedirectionApi redirApi, VMPort port) throws Exception {
        String sfcId = sg.getNetworkElementId();
        String flowClassifier = port.getInspectionHookId();

        if (sfcId == null && flowClassifier != null) {
            // Unbinded sg and inspection hook is present for the port
            this.tg.appendTask(this.sfcFlowClassifierDeleteTask.create(sg, port));
        } else if (sfcId != null) {
            InspectionHookElement hook = redirApi.getInspectionHook(flowClassifier);

            if (flowClassifier == null || hook == null) {
                this.tg.appendTask(this.sfcFlowClassifierCreateTask.create(sg, port));
            } else {
                if (!hook.getInspectionPort().getElementId().equals(sfcId)) {
                    // If hook needs to be updated, update it
                    this.tg.appendTask(this.sfcFlowClassifierUpdateTask.create(sg, port));
                }
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Checking Inspection hooks for %s Security Group Member '%s'", this.sgm.getType(),
                this.sgm.getMemberName());
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

}
