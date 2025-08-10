/*
 * Copyright (c) 2022-2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.management.service.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.context.CarbonCoreInitializedEvent;
import org.wso2.carbon.identity.organization.management.service.OrganizationGroupResidentResolverService;
import org.wso2.carbon.identity.organization.management.service.OrganizationGroupResidentResolverServiceImpl;
import org.wso2.carbon.identity.organization.management.service.OrganizationManagementInitialize;
import org.wso2.carbon.identity.organization.management.service.OrganizationManagementInitializeImpl;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationManagerImpl;
import org.wso2.carbon.identity.organization.management.service.OrganizationUserResidentResolverService;
import org.wso2.carbon.identity.organization.management.service.OrganizationUserResidentResolverServiceImpl;
import org.wso2.carbon.identity.organization.management.service.listener.OrganizationManagerListener;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementConfigUtil;
import org.wso2.carbon.tenant.mgt.services.TenantMgtService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * OSGi service component for organization management core bundle.
 */
@Component(name = "identity.organization.management.component",
        immediate = true)
public class OrganizationManagementServiceComponent {

    private static final Log LOG = LogFactory.getLog(OrganizationManagementServiceComponent.class);

    /**
     * Register Organization Manager service in the OSGi context.
     *
     * @param componentContext OSGi service component context.
     */
    @Activate
    protected void activate(ComponentContext componentContext) {

        try {
            OrganizationManagementConfigUtil.loadOrgMgtConfigurations();
            BundleContext bundleContext = componentContext.getBundleContext();
            OrganizationManager organizationManager = new OrganizationManagerImpl();
            bundleContext.registerService(OrganizationManager.class.getName(), organizationManager, null);
            OrganizationManagementDataHolder.getInstance().setOrganizationManager(organizationManager);
            bundleContext.registerService(OrganizationUserResidentResolverService.class.getName(),
                    new OrganizationUserResidentResolverServiceImpl(), null);
            bundleContext.registerService(OrganizationGroupResidentResolverService.class.getName(),
                    new OrganizationGroupResidentResolverServiceImpl(), null);
            bundleContext.registerService(OrganizationManagementInitialize.class.getName(),
                    new OrganizationManagementInitializeImpl(), null);
            OrganizationManagementDataHolder.getInstance().initDataSource();
            LOG.debug("Organization Management component activated successfully.");
        } catch (Exception e) {
            LOG.error("Error while activating Organization Management module.", e);
        }
    }

    /**
     * Set realm service implementation.
     *
     * @param realmService RealmService
     */
    @Reference(
            name = "user.realmservice.default",
            service = RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService"
    )
    protected void setRealmService(RealmService realmService) {

        LOG.debug("Setting the Realm Service.");
        OrganizationManagementDataHolder.getInstance().setRealmService(realmService);
    }

    /**
     * Unset realm service implementation.
     *
     * @param realmService RealmService
     */
    protected void unsetRealmService(RealmService realmService) {

        LOG.debug("Unsetting the Realm Service.");
        OrganizationManagementDataHolder.getInstance().setRealmService(null);
    }

    @Reference(
            name = "org.wso2.carbon.tenant.mgt",
            service = org.wso2.carbon.tenant.mgt.services.TenantMgtService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetTenantMgtService")
    protected void setTenantMgtService(TenantMgtService tenantMgtService) {

        LOG.debug("Setting the Tenant Management Service.");
        OrganizationManagementDataHolder.getInstance().setTenantMgtService(tenantMgtService);
    }

    protected void unsetTenantMgtService(TenantMgtService tenantMgtService) {

        LOG.debug("Unsetting the Tenant Management Service.");
        OrganizationManagementDataHolder.getInstance().setTenantMgtService(null);
    }

    @Reference(
            name = "identity.org.mgt.listener",
            service = OrganizationManagerListener.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManagerListener"
    )
    protected void setOrganizationManagerListener(OrganizationManagerListener organizationManagerListener) {

        OrganizationManagementDataHolder.getInstance().setOrganizationManagerListener(organizationManagerListener);
    }

    protected void unsetOrganizationManagerListener(OrganizationManagerListener organizationManagerListener) {

        OrganizationManagementDataHolder.getInstance().setOrganizationManagerListener(null);
    }

    /**
     * This was added to ensure that the CarbonCoreInitializedEvent is set before the
     * organization management service is activated to avoid null pointer exceptions
     * due to the datasource not being initialized. This is needed after the OrganizationManagerListener reference is
     * made optional to fix a cyclic dependency issue.
     *
     * @param carbonCoreInitializedEvent CarbonCoreInitializedEvent
     */
    @Reference(
            name = "carbon.core.initialize.service",
            service = CarbonCoreInitializedEvent.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetCarbonCoreInitializeService"
    )
    protected void setCarbonCoreInitializeService(CarbonCoreInitializedEvent carbonCoreInitializedEvent) {

        LOG.debug("Setting the CarbonCoreInitializedEvent Service.");
    }

    protected void unsetCarbonCoreInitializeService(CarbonCoreInitializedEvent carbonContext) {

        LOG.debug("Unsetting the CarbonCoreInitializedEvent Service.");
    }
}
