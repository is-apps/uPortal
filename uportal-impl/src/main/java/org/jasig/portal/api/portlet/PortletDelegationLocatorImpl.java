/**
 * Copyright (c) 2000-2009, Jasig, Inc.
 * See license distributed with this file and available online at
 * https://www.ja-sig.org/svn/jasig-parent/tags/rel-10/license-header.txt
 */

package org.jasig.portal.api.portlet;

import javax.portlet.PortletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.pluto.internal.InternalPortletRequest;
import org.apache.pluto.internal.InternalPortletWindow;
import org.jasig.portal.IChannelRegistryStore;
import org.jasig.portal.channel.IChannelDefinition;
import org.jasig.portal.channels.portlet.IPortletRenderer;
import org.jasig.portal.layout.TransientUserLayoutManagerWrapper;
import org.jasig.portal.portlet.om.IPortletDefinition;
import org.jasig.portal.portlet.om.IPortletDefinitionId;
import org.jasig.portal.portlet.om.IPortletEntity;
import org.jasig.portal.portlet.om.IPortletEntityId;
import org.jasig.portal.portlet.om.IPortletWindow;
import org.jasig.portal.portlet.om.IPortletWindowId;
import org.jasig.portal.portlet.registry.IPortletEntityRegistry;
import org.jasig.portal.portlet.registry.IPortletWindowRegistry;
import org.jasig.portal.security.IPerson;
import org.jasig.portal.security.IPersonManager;
import org.jasig.portal.url.IPortalRequestUtils;

/**
 * @author Eric Dalquist
 * @version $Revision$
 */
public class PortletDelegationLocatorImpl implements PortletDelegationLocator {
    private IChannelRegistryStore channelRegistryStore;
    private IPortalRequestUtils portalRequestUtils;
    private IPersonManager personManager;
    private IPortletEntityRegistry portletEntityRegistry;
    private IPortletWindowRegistry portletWindowRegistry;
    private IPortletRenderer portletRenderer;
    

    public void setChannelRegistryStore(IChannelRegistryStore channelRegistryStore) {
        this.channelRegistryStore = channelRegistryStore;
    }

    public void setPortalRequestUtils(IPortalRequestUtils portalRequestUtils) {
        this.portalRequestUtils = portalRequestUtils;
    }

    public void setPersonManager(IPersonManager personManager) {
        this.personManager = personManager;
    }

    public void setPortletEntityRegistry(IPortletEntityRegistry portletEntityRegistry) {
        this.portletEntityRegistry = portletEntityRegistry;
    }

    public void setPortletWindowRegistry(IPortletWindowRegistry portletWindowRegistry) {
        this.portletWindowRegistry = portletWindowRegistry;
    }

    public void setPortletRenderer(IPortletRenderer portletRenderer) {
        this.portletRenderer = portletRenderer;
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.api.portlet.PortletDelegationLocator#createRequestDispatcher(java.lang.String)
     */
    @Override
    public PortletDelegationDispatcher createRequestDispatcher(PortletRequest portletRequest, String fName) {
        final IChannelDefinition channelDefinition = this.channelRegistryStore.getChannelDefinition(fName);
        if (channelDefinition == null || !channelDefinition.isPortlet()) {
            return null;
        }
        
        final IPortletDefinition portletDefinition = channelDefinition.getPortletDefinition();
        final IPortletDefinitionId portletDefinitionId = portletDefinition.getPortletDefinitionId();
        
        return this.createRequestDispatcher(portletRequest, portletDefinitionId);
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.api.portlet.PortletDelegationLocator#createRequestDispatcher(org.jasig.portal.portlet.om.IPortletDefinitionId)
     */
    @Override
    public PortletDelegationDispatcher createRequestDispatcher(PortletRequest portletRequest, IPortletDefinitionId portletDefinitionId) {
        final HttpServletRequest request = this.portalRequestUtils.getOriginalPortalRequest(portletRequest);
        final IPerson person = this.personManager.getPerson(request);
        
        final String transientChannelSubscribeId = TransientUserLayoutManagerWrapper.SUBSCRIBE_PREFIX + "." + portletDefinitionId;
        final IPortletEntity portletEntity = this.portletEntityRegistry.getOrCreatePortletEntity(portletDefinitionId, transientChannelSubscribeId, person.getID());
        
        final IPortletEntityId portletEntityId = portletEntity.getPortletEntityId();

        final InternalPortletWindow internalPortletWindow = ((InternalPortletRequest)portletRequest).getInternalPortletWindow();
        final IPortletWindow parentPortletWindow = this.portletWindowRegistry.convertPortletWindow(request, internalPortletWindow);
        final IPortletWindowId parentPortletWindowId = parentPortletWindow.getPortletWindowId();
        final IPortletWindow portletWindow = this.portletWindowRegistry.createDelegatePortletWindow(request, portletEntityId, parentPortletWindowId);
        
        return new PortletDelegationDispatcherImpl(portletWindow, person.getID(), this.portalRequestUtils, this.personManager, this.portletRenderer);
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.api.portlet.PortletDelegationLocator#getRequestDispatcher(org.jasig.portal.portlet.om.IPortletWindowId)
     */
    @Override
    public PortletDelegationDispatcher getRequestDispatcher(PortletRequest portletRequest, IPortletWindowId portletWindowId) {
        final HttpServletRequest request = this.portalRequestUtils.getOriginalPortalRequest(portletRequest);
        final IPerson person = this.personManager.getPerson(request);
        
        final IPortletWindow portletWindow = this.portletWindowRegistry.getPortletWindow(request, portletWindowId);
        
        return new PortletDelegationDispatcherImpl(portletWindow, person.getID(), this.portalRequestUtils, this.personManager, this.portletRenderer);
    }
}