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
package org.osc.core.broker.service.servicefunctionchain;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.UpdateServiceFunctionChainServiceApi;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.request.AddOrUpdateServiceFunctionChainRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.validator.RequestValidator;
import org.osc.core.broker.service.validator.ServiceFunctionChainRequestValidator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class UpdateServiceFunctionChainService
		extends ServiceDispatcher<AddOrUpdateServiceFunctionChainRequest, BaseJobResponse>
		implements UpdateServiceFunctionChainServiceApi {

	RequestValidator<AddOrUpdateServiceFunctionChainRequest, ServiceFunctionChain> validator;

	@Reference
	private ServiceFunctionChainRequestValidator validatorFactory;

	@Override
	public BaseJobResponse exec(AddOrUpdateServiceFunctionChainRequest request, EntityManager em) throws Exception {

		if (this.validator == null) {
			this.validator = this.validatorFactory.create(em);
		}

		//TODO: karimull Check for SFC in use before update ; after binding and installinspectionhook task are done

		ServiceFunctionChain sfc = this.validator.validateAndLoad(request);

		// remove old/existing list

        if (CollectionUtils.emptyIfNull(sfc.getVirtualSystems()) != null) {
            sfc.getVirtualSystems().clear();
        }

		//update the entity to remove all the primary key association in case of a shuffle in the vsid list
		OSCEntityManager.update(em, sfc, this.txBroadcastUtil);
		em.flush();

		// add the new vsid list to entity
		for (Long vsId : CollectionUtils.emptyIfNull(request.getVirtualSystemIds())) {
			sfc.addVirtualSystem(VirtualSystemEntityMgr.findById(em, vsId));
		}
		OSCEntityManager.update(em, sfc, this.txBroadcastUtil);

		BaseJobResponse response = new BaseJobResponse();
		response.setId(sfc.getId());

		return response;
	}

}