package org.solmix.service.versioncontrol;

import java.util.Map;

public class MapEntry<K, V>  implements Map.Entry<K, V> {

	protected K key;
	protected V value;
	
	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	public V setValue(V value) {
		this.value = value;
		return value;
	}
	
	public void setKey(K key){
		this.key = key;
	}
	
	public MapEntry(K key, V value) {
		this.key = key;
		this.value = value;
	}
	
	public MapEntry(){
		
	}

}
