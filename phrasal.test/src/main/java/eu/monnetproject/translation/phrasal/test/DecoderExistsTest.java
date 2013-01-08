package eu.monnetproject.translation.phrasal.test;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import eu.monnetproject.framework.test.TestMonitor;
import eu.monnetproject.framework.test.annotation.TestCase;
import eu.monnetproject.framework.test.annotation.TestSuite;
import eu.monnetproject.translation.Decoder;
import eu.monnetproject.translation.DecoderFactory;
import eu.monnetproject.translation.monitor.Messages;

@TestSuite(label="DecoderExists")
public class DecoderExistsTest {
	
	
	@TestCase(identifier="1",label="decoderExists")
	public void test(TestMonitor monitor) {
		Bundle bundle = FrameworkUtil.getBundle(this.getClass());
		BundleContext bundleContext = bundle.getBundleContext();
		monitor.assertion(bundleContext != null, "Bundle context does not exist (OSGi not stable)");
		ServiceReference sr = bundleContext.getServiceReference(DecoderFactory.class.getName());
        final DecoderFactory service = (DecoderFactory)bundleContext.getService(sr);
        Messages.info("Got decoder factory " + service.getClass().getName());
        monitor.assertion(service != null, "No service");
        final Decoder decoder = service.getDecoder(null, null);
        Messages.info("Got decoder instance " + decoder.getClass().getName());
        monitor.assertion(decoder != null, "No decoder");
	}
	
}
