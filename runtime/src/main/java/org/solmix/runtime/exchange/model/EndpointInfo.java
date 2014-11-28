/*
 * Copyright 2014 The Solmix Project
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.gnu.org/licenses/ 
 * or see the FSF site: http://www.fsf.org. 
 */

package org.solmix.runtime.exchange.model;


/**
 * 
 * @author solmix.f@gmail.com
 * @version $Id$ 2014年10月15日
 */

public class EndpointInfo extends InfoPropertiesSupport {

    private ServiceInfo service;

    private ProtocolInfo protocol;

    private String address;

    private String transporterName;

    private InfoID iD;

    public EndpointInfo() {

    }

    public EndpointInfo(ServiceInfo service, String transporterName) {
        this.service = service;
        this.transporterName = transporterName;
    }

    /**   */
    public ServiceInfo getService() {
        return service;
    }

    public ProtocolInfo getProtocol() {
        return protocol;
    }

    /**   */
    public void setService(ServiceInfo serviceInfo) {
        this.service = serviceInfo;
    }

    public InterfaceInfo getInterface() {
        if (service == null) {
            return null;
        }
        return service.getInterface();
    }

    /**
     * 
     */
    public String getAddress() {
        return address;
    }

    /**
     * pipeline 类型.
     */
    public String getTransporterName() {
        return transporterName;
    }

    /**
     * @return
     */
    public InfoID getID() {
        return iD;
    }

    public void setID(InfoID iD) {
        this.iD = iD;
    }

    @Override
    public String toString() {
        return "ProtocolID="
            + (protocol == null ? ""
                : (protocol.getID() + ", ServiceID=" + (protocol.getService() == null ? ""
                    : protocol.getService().getID()))) + ", ID=" + iD;
    }

}
