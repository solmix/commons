

package org.hyperic.sigar;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class SigarLog {

    //from sigar_log.h
    private static final int LOG_FATAL  = 0;
    private static final int LOG_ERROR  = 1;
    private static final int LOG_WARN   = 2;
    private static final int LOG_INFO   = 3;
    private static final int LOG_DEBUG  = 4;

    private static native void setLogger(Sigar sigar, Logger log);

    public static native void setLevel(Sigar sigar, int level);

    //XXX PAX-LOG not support getAllAppenders() method 
    //and pax in osgi not check configured.
/*    private static boolean isLogConfigured() {
        //funny, log4j has no api to determine if logging has been
        //configured? .. yet bitches at you like a mo-fo when logging
        //has not been configured.
        return Logger.getRootLogger().getAllAppenders().hasMoreElements();
    }*/

    private static Logger getLogger() {
        return getLogger("Sigar");
    }

    public static Logger getLogger(String name) {
        Logger log = Logger.getLogger(name);
        /*if (!isLogConfigured()) {
            BasicConfigurator.configure();
        }*/
        return log;
    }

    static void error(String msg, Throwable exc) {
        getLogger().error(msg, exc);
    }

    static void debug(String msg, Throwable exc) {
        getLogger().debug(msg, exc);
    }

    //XXX want to make this automatic, but also dont always
    //want to turn on logging, since most sigar logging will be DEBUG
    public static void enable(Sigar sigar) {
        Logger log = getLogger();

        Level level = log.getLevel();
        if (level == null) {
            level = Logger.getRootLogger().getLevel();
            if (level == null) {
                return;
            }
        }

        switch (level.toInt()) {
          case Level.FATAL_INT:
            setLevel(sigar, LOG_FATAL);
            break;
          case Level.ERROR_INT:
            setLevel(sigar, LOG_ERROR);
            break;
          case Level.WARN_INT:
            setLevel(sigar, LOG_WARN);
            break;
          case Level.INFO_INT:
            setLevel(sigar, LOG_INFO);
            break;
          case Level.DEBUG_INT:
            setLevel(sigar, LOG_DEBUG);
            break;
        }

        setLogger(sigar, log);
    }

    public static void disable(Sigar sigar) {
        setLogger(sigar, null);
    }
}
