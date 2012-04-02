package com.xored.q7.reporting.example.tests;

import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.junit.Test;

import com.xored.q7.reporting.core.Q7ReportIterator;
import com.xored.q7.reporting.example.Activator;
import com.xored.q7.reporting.example.renderers.Sample1StatisticsReportRenderer;

public class SimpleReportTests {
	@Test
	public void testSimpleReport() throws Throwable {
		URL entry = Activator.getDefault().getBundle()
				.getEntry("/samples/sample.report");
		URL url = FileLocator.resolve(entry);
		File file = new File(url.getPath());
		Sample1StatisticsReportRenderer renderer = new Sample1StatisticsReportRenderer();
		StringBuilder builder = new StringBuilder();
		Q7ReportIterator iterator = new Q7ReportIterator(file);
		renderer.generateReport(new StringBuilderFactory(builder), "sample",
				iterator);
		System.out.println("Generated report:\n" + builder.toString());
	}
}
