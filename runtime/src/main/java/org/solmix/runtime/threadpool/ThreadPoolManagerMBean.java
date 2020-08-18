
package org.solmix.runtime.threadpool;

import javax.management.JMException;
import javax.management.ObjectName;

import org.solmix.runtime.Container;
import org.solmix.runtime.management.ManagedComponent;
import org.solmix.runtime.management.ManagementConstants;
import org.solmix.runtime.management.annotation.ManagedOperation;
import org.solmix.runtime.management.annotation.ManagedResource;

@ManagedResource(componentName = "ThreadPoolManager", description = "The Solmix manangement of thread pool ", currencyTimeLimit = 15, persistPolicy = "OnUpdate", persistPeriod = 200)

public class ThreadPoolManagerMBean implements ManagedComponent
{

    static final String NAME_VALUE = "DefaultManager";

    static final String TYPE_VALUE = "ThreadPoolManager";

    private ThreadPoolManager manager;

    private Container container;

    public ThreadPoolManagerMBean(DefaultThreadPoolManager c)
    {
        manager = c;
        container = c.getContainer();
    }

    @ManagedOperation(currencyTimeLimit = 30)
    public void shutdown(boolean processRemainingWorkItems) {
        manager.shutdown(processRemainingWorkItems);
    }

    @Override
    public ObjectName getObjectName() throws JMException {
        StringBuilder buffer = new StringBuilder();

        buffer.append(ManagementConstants.DEFAULT_DOMAIN_NAME).append(':');
        buffer.append(ManagementConstants.TYPE_PROP)        .append('=').append(TYPE_VALUE)           .append(',');
        buffer.append(ManagementConstants.CONTAINER_ID_PROP).append('=').append(container.getId())    .append(',');
        buffer.append(ManagementConstants.NAME_PROP)        .append('=').append(NAME_VALUE)           ;
        return new ObjectName(buffer.toString());
    }
}
