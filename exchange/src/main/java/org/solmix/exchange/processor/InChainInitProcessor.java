/**
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

package org.solmix.exchange.processor;

import java.util.Collection;
import java.util.SortedSet;

import org.solmix.commons.util.ClassLoaderUtils;
import org.solmix.commons.util.ClassLoaderUtils.ClassLoaderHolder;
import org.solmix.exchange.Endpoint;
import org.solmix.exchange.Exchange;
import org.solmix.exchange.ExchangeRuntimeException;
import org.solmix.exchange.Message;
import org.solmix.exchange.Processor;
import org.solmix.exchange.Protocol;
import org.solmix.exchange.Service;
import org.solmix.exchange.Transporter;
import org.solmix.exchange.interceptor.Interceptor;
import org.solmix.exchange.interceptor.InterceptorChain;
import org.solmix.exchange.interceptor.InterceptorProvider;
import org.solmix.exchange.interceptor.phase.Phase;
import org.solmix.exchange.interceptor.phase.PhaseChainCache;
import org.solmix.exchange.support.DefaultExchange;
import org.solmix.runtime.Container;
import org.solmix.runtime.ContainerFactory;

/**
 * Chain初始化消息处理器
 * 
 * @author solmix.f@gmail.com
 * @version $Id$ 2014年10月20日
 */

public class InChainInitProcessor implements Processor {

    protected Endpoint endpoint;

    protected Container container;

    protected ClassLoader loader;

    private final PhaseChainCache chainCache = new PhaseChainCache();

    public InChainInitProcessor(Endpoint endpoint, Container container) {
        super();
        this.endpoint = endpoint;
        this.container = container;
        if (container != null) {
            loader = container.getExtension(ClassLoader.class);
        }
    }

    @Override
    public void process(Message message) throws ExchangeRuntimeException {
        assert message != null;
        Container orig = ContainerFactory.getAndSetThreadDefaultContainer(container);
        ClassLoaderHolder origLoader = null;
        try {
            if (loader != null) {
                origLoader = ClassLoaderUtils.setThreadContextClassloader(loader);
            }
            InterceptorChain phaseChain = null;

            if (message.getInterceptorChain() != null) {
                phaseChain = message.getInterceptorChain();
                // 单线程依次执行
                synchronized (phaseChain) {
                    if (phaseChain.getState() == InterceptorChain.State.PAUSED
                        || phaseChain.getState() == InterceptorChain.State.SUSPENDED) {
                        phaseChain.resume();
                        return;
                    }
                }
            }
            //重新组装
            Message m = getProtocol().createMessage(message);
            Exchange exchange = m.getExchange();
            if (exchange == null) {
                exchange = new DefaultExchange();
                message.setExchange(exchange);
            }
            exchange.setIn(m);
            setupExchange(exchange, m);
            
            InterceptorProvider serial = null;
            if (endpoint.getService().getDataProcessor() instanceof InterceptorProvider) {
                serial = (InterceptorProvider)endpoint.getService().getDataProcessor();
            }

            if (serial == null) {
                phaseChain = chainCache.get(getPhases(),
                                            endpoint.getService().getInInterceptors(),
                                            endpoint.getInInterceptors(),
                                            getProtocol().getInInterceptors());
            } else {
                phaseChain = chainCache.get(getPhases(),
                                            endpoint.getService().getInInterceptors(),
                                            endpoint.getInInterceptors(),
                                            getProtocol().getInInterceptors(),
                                            serial.getInInterceptors());
            }
            m.setInterceptorChain(phaseChain);
            phaseChain.setFaultProcessor(endpoint.getOutFaultProcessor());

            addToChain(phaseChain, m);

            phaseChain.doIntercept(m);
        } finally {
            if (orig != container) {
                ContainerFactory.setThreadDefaultContainer(orig);
            }
            if (origLoader != null) {
                origLoader.reset();
            }
        }
    }

    protected SortedSet<Phase> getPhases() {
        return endpoint.getPhasePolicy().getInPhases();
    }

    @SuppressWarnings("unchecked")
    private void addToChain(InterceptorChain chain, Message m) {

        Collection<InterceptorProvider> providers = (Collection<InterceptorProvider>) m.get(Message.INTERCEPTOR_PROVIDERS);
        if (providers != null) {
            for (InterceptorProvider p : providers) {
                chain.add(p.getInInterceptors());
            }
        }
        Interceptor<Message> is = (Interceptor<Message>) m.get(Message.IN_INTERCEPTORS);
        if (is != null) {
            chain.add(is);
        }
    }

    protected Protocol getProtocol() {
        return endpoint.getProtocol();
    }

    private void setupExchange(Exchange exchange, Message m) {
        exchange.put(Endpoint.class, endpoint);
        exchange.put(Protocol.class, getProtocol());
        exchange.put(Container.class, container);
        if (m.get(Transporter.class) != null) {
            exchange.put(Transporter.class, m.get(Transporter.class));
        }

        if (endpoint != null && endpoint.getService() != null) {
            exchange.put(Service.class, endpoint.getService());
        } else {
            exchange.put(Service.class, null);
        }

    }
}
