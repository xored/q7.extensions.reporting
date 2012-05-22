package com.xored.q7.reporting.example.renderers;

import static com.xored.q7.reporting.example.internal.SampleReportingPlugin.createErr;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.xored.q7.reporting.Q7Info;
import com.xored.q7.reporting.core.IQ7ReportConstants;
import com.xored.q7.reporting.core.IReportRenderer;
import com.xored.q7.reporting.core.Q7ReportIterator;
import com.xored.sherlock.core.model.sherlock.report.Report;

public class SampleReportRenderer implements IReportRenderer {

	public IStatus generateReport(IContentFactory factory, String reportName,
			Q7ReportIterator reportIterator) {
		// Creating folder based on report name
		// we are going to create a bunch of XML files based on feature name
		// Feature name is identified by tag nested under "features" tag.
		// If test case has several tags marked as "features", then only the
		// first
		// feature name is used.
		//
		// If test does not have a features/ tag, then it has "unspecified"
		// feature
		IContentFactory folderFactory = factory.createFolder(reportName);

		// Map with result containers
		Map<String, FeatureContainer> containers = new HashMap<String, FeatureContainer>();
		// Iterating through all reports in iterator and looking for
		reportIterator.reset();
		while (reportIterator.hasNext()) {
			Report report = reportIterator.next();
			if (!report.getRoot().getProperties()
					.containsKey(IQ7ReportConstants.ROOT)) {
				// Sanity check
				continue;

			}

			// To get ECL content:
			// String scriptContent = Scenarios.getScriptContent((Scenario)
			// scenario.getNamedElement());

			// To find test case by id:
			/*
			 * IProject[] q7Projects = Q7Core.getQ7Projects(); for (IProject
			 * iProject : q7Projects) { IQ7Project project =
			 * Q7Core.create(iProject); try { IQ7NamedElement[] tests =
			 * project.findNamedElement(id);
			 */
			Q7Info q7info = (Q7Info) report.getRoot().getProperties()
					.get(IQ7ReportConstants.ROOT);
			// report.
		}

		return Status.OK_STATUS;
	}

	public String[] getGeneratedFileNames(String reportName) {
		return new String[] { reportName };
	}

	private DocumentBuilder builder;

	private DocumentBuilder getBuilder() throws CoreException {
		if (builder == null) {
			try {
				builder = DocumentBuilderFactory.newInstance()
						.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				throw new CoreException(createErr(
						"Error creating document builder", e));
			}
		}
		return builder;
	}

	private Transformer transformer;

	private Transformer getTransformer() throws CoreException {
		if (transformer == null) {
			TransformerFactory tf = TransformerFactory.newInstance();
			try {
				transformer = tf.newTransformer();
			} catch (TransformerConfigurationException e) {
				throw new CoreException(createErr(
						"Error creating document writer", e));
			}
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		}
		return transformer;
	}

	private static final String ELEMENT_ROOT = "testCases";
	private static final String ELEMENT_TEST_CASE = "testCase";
	private static final String ATTR_TOTAL = "total";
	private static final String ATTR_PASSED = "passed";
	private static final String ATTR_FAILED = "failed";

	private class FeatureContainer {
		public FeatureContainer(String featureName, IContentFactory factory)
				throws CoreException {
			this.destination = factory.createFileStream(String.format("%s.xml",
					featureName));
			this.document = getBuilder().newDocument();
			this.root = document.createElement(ELEMENT_ROOT);
			document.appendChild(root);
		}

		private int total = 0;
		private int passed = 0;
		private final OutputStream destination;
		private final Document document;
		private final Element root;

		/**
		 * Saves accumulated document
		 * 
		 * @throws CoreException
		 *             when file cannot be written
		 */
		public void close() throws CoreException {
			root.setAttribute(ATTR_TOTAL, Integer.toString(total));
			root.setAttribute(ATTR_PASSED, Integer.toString(passed));
			root.setAttribute(ATTR_FAILED, Integer.toString(total - passed));

			try {
				StreamResult result = new StreamResult(destination);
				getTransformer().transform(new DOMSource(document), result);
				destination.close();
			} catch (Exception e) {
				throw new CoreException(createErr(
						"Error saving result xml file", e));
			}
		}
	}
}
