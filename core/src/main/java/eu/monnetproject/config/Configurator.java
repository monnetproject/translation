package eu.monnetproject.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Properties;


/**
 * A configuration loader
 *
 * @author John McCrae
 */
public class Configurator {

    private Configurator() {
    }

    /**
     * Get the configuration or an empty configuration if file is not found
     */
    public static Properties getConfig(String pid) {
        String sysProp = System.getProperty(pid);
        if (sysProp == null) {
            File f = new File(System.getProperty("user.dir") + System.getProperty("file.separator") + "load"
                    + System.getProperty("file.separator") + pid + ".cfg");
            Properties props = new Properties();
            if (f.exists()) {
                try {
                    props.load(new FileInputStream(f));
                } catch (IOException x) {
                    throw new RuntimeException(x);
                }
            } else {
                final URL res = Configurator.class.getResource("load/"+pid+".cfg");
                
                if (res != null) {
                    try {
                        props.load(res.openStream());
                    } catch (IOException x) {
                        throw new RuntimeException(x);
                    }
                } else {
                    final URL res2 = Configurator.class.getResource("/load/"+pid+".cfg");
                    if(res2 != null) {
                        try {
                            props.load(res2.openStream());
                        } catch(IOException x) {
                            throw new RuntimeException(x);
                        }
                    }
                }
            }
            return props;
        } else {
            Properties props = new Properties();
            try {
                props.load(new StringReader(sysProp));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            return props;
        }
    }

    /**
     * Set a configuration globally. Best used for debugging
     * @param pid The component ID
     * @param keyVals The key/value pairs, must be an even number of arguments
     */
    public static void setConfig(String pid, String... keyVals) {
        if (keyVals.length % 2 != 0) {
            throw new IllegalArgumentException("non-even number of key/val pairs passed to set config");
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keyVals.length; i += 2) {
            sb.append(keyVals[i]).append("=").append(keyVals[i + 1]).append("\n");
        }
        System.setProperty(pid, sb.toString());
    }
}
