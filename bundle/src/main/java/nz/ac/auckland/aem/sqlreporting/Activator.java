package nz.ac.auckland.aem.sqlreporting;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;

/**
 * @author Marnix Cook
 *
 * Activator for this bundle. If there are no services, bundles aren't loaded.
 */
@Service
public class Activator {

    @Activate
    public void activation(ComponentContext componentContext) {
        /* do nothing */
    }
}
