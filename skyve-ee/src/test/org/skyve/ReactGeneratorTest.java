package org.skyve;

import org.junit.Test;
import org.skyve.impl.generate.pwa.react.ReactGenerator;

import util.AbstractH2Test;

public class ReactGeneratorTest extends AbstractH2Test {
	@Test
	@SuppressWarnings("static-method")
	public void generate() throws Exception {
		ReactGenerator.main(new String[0]);
	}
}
