package org.solmix.service.filetrack;

public enum EventActionsEnum {
	CREATE("create"),
	DELETE("delete"),
	MODIFY("modify"),
	RENAME("rename"),
	REGISTER("register"),
	REGISTER_COMPLETE("register_complete"),
	SKIPPED("skipped");
	
	private final String value;
	
	private EventActionsEnum(String value) {
		this.value = value;
	}
	
	public String protocolValue(){
		return value;
	}
	
	@Override
	public String toString(){
		return value;
	}
	
	public static EventActionsEnum getEnumValue(final String strVal){
		return EventActionsEnum.valueOf(strVal.toUpperCase());
	}
}

