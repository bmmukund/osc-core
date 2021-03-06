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
package org.osc.core.broker.service.request;

import java.util.ArrayList;
import java.util.List;

import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.VirtualSystemPolicyBindingDto;

public class BindSecurityGroupRequest extends BaseRequest<BaseDto> {

    /**
     * The virtualization connector ID this security group belongs to. This is to support
     * validation from REST API Calls. VC Id will not be used to load the vc.
     * Intended to be null for all cases except for the API
     */
    private Long vcId;
    private Long securityGroupId;
    private Long sfcId;
    private boolean isBindSfc;

    private List<VirtualSystemPolicyBindingDto> servicesToBindTo = new ArrayList<>();

    public boolean isUnBindSecurityGroup() {
        return this.servicesToBindTo == null || this.servicesToBindTo.isEmpty();
    }

    public Long getSecurityGroupId() {
        return this.securityGroupId;
    }

    public void setSecurityGroupId(Long securityGroupId) {
        this.securityGroupId = securityGroupId;
    }

    public List<VirtualSystemPolicyBindingDto> getServicesToBindTo() {
        return this.servicesToBindTo;
    }

    public void setServicesToBindTo(List<VirtualSystemPolicyBindingDto> servicesToBindTo) {
        this.servicesToBindTo = servicesToBindTo;
    }

    public void addServiceToBindTo(VirtualSystemPolicyBindingDto serviceToBindTo) {
        this.servicesToBindTo.add(serviceToBindTo);
    }

    public Long getVcId() {
        return this.vcId;
    }

    public void setVcId(Long vcId) {
        this.vcId = vcId;
    }

	public Long getSfcId() {
		return this.sfcId;
	}

	public void setSfcId(Long sfcId) {
		this.sfcId = sfcId;
	}

    public boolean isBindSfc() {
        return this.isBindSfc;
    }

    public void setBindSfc(boolean isBindSfc) {
        this.isBindSfc = isBindSfc;
    }


}
