package org.solmix.service.watch;

import org.solmix.service.watch.event.ChangeEvent;

public interface ChangeListener
{

    public void onChange(final ChangeEvent event);
}
