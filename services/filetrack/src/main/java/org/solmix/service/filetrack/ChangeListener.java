package org.solmix.service.filetrack;

import org.solmix.service.filetrack.event.ChangeEvent;

public interface ChangeListener
{

    public void onChange(final ChangeEvent event);
}
