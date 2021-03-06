/**
 * Copyright (c) 2014 The Solmix Project
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

package org.solmix.exchange.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solmix.exchange.Processor;
import org.solmix.exchange.Protocol;
import org.solmix.exchange.Transporter;
import org.solmix.exchange.model.EndpointInfo;
import org.solmix.runtime.Container;

/**
 * 
 * @author solmix.f@gmail.com
 * @version $Id$ 2014年12月17日
 */

public abstract class AbstractTransporter implements Transporter {

	protected Processor processor;

	protected Protocol protocol;

	protected String address;

	protected EndpointInfo endpointInfo;

	protected Container container;

	public AbstractTransporter(String address, EndpointInfo endpointInfo, Container container) {
		this.address = address;
		this.endpointInfo = endpointInfo;
		this.container = container;
	}

	@Override
	public Processor getProcessor() {
		return processor;
	}

	@Override
	public String getAddress() {
		return address;
	}

	@Override
	public void setProcessor(Processor processor) {
		if (this.processor != processor) {
			Processor old = this.processor;
			if (processor != null) {
				this.processor = processor;
				if (getLogger().isTraceEnabled()) {
					getLogger().trace("Register message Processor: " + processor);
				}
				if (old == null) {
					try {
						activate(processor);
					} catch (RuntimeException ex) {
						deactivate(processor);
						this.processor = null;
						throw ex;
					}
				} else {
					if (old != null) {
						getLogger().trace("unregistering incoming observer: " + old);
						deactivate(old);
					}
					this.processor = processor;
				}
			}
		}
	}

	@Override
	public Protocol getProtocol() {
		return protocol;
	}

	@Override
	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	protected abstract Logger getLogger();

	protected void activate(Processor p) {

	}

	protected void deactivate(Processor p) {

	}

	protected abstract class AbstractBackPipeline extends AbstractPipeline {

		public AbstractBackPipeline() {
			super("");
		}

		/**
		 * nothing todo.
		 */
		@Override
		public void setProcessor(Processor processor) {

		}

		@Override
		protected Logger getLogger() {
			return LoggerFactory.getLogger(AbstractBackPipeline.class);
		}
	}
}
