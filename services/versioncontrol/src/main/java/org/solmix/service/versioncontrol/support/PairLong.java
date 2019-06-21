package org.solmix.service.versioncontrol.support;

import java.io.Serializable;

import org.solmix.service.versioncontrol.MapEntry;

public class PairLong extends MapEntry<Long, Long> implements Serializable {

	private static final long serialVersionUID = -4720282308915646994L;

	public PairLong(Long key, Long value) {
		super(key, value);
	}

	public PairLong() {
		setKey(0L);
		setValue(0L);
	}

}