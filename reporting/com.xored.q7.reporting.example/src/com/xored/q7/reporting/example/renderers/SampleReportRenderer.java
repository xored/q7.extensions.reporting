package com.xored.q7.reporting.example.renderers;

import static com.xored.q7.reporting.example.internal.SampleReportingPlugin.createErr;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.xored.q7.reporting.ItemKind;
import com.xored.q7.reporting.Q7Info;
import com.xored.q7.reporting.ResultStatus;
import com.xored.q7.reporting.core.IQ7ReportConstants;
import com.xored.q7.reporting.core.IReportRenderer;
import com.xored.q7.reporting.core.Q7ReportIterator;
import com.xored.q7.reporting.example.internal.SampleReportingPlugin;
import com.xored.sherlock.core.model.sherlock.report.Node;
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
			String featureName = getFeatureName(report);
			if (!containers.containsKey(featureName)) {
				try {
					containers.put(featureName, new FeatureContainer(
							featureName, folderFactory));
				} catch (CoreException e) {
					return e.getStatus();
				}
			}

			containers.get(featureName).addReport(report);

		}

		for (FeatureContainer container : containers.values()) {
			List<IStatus> statusList = new ArrayList<IStatus>();
			try {
				container.close();
			} catch (CoreException e) {
				statusList.add(e.getStatus());
			}

			if (!statusList.isEmpty()) {
				return new MultiStatus(SampleReportingPlugin.PLUGIN_ID,
						IStatus.ERROR,
						statusList.toArray(new IStatus[statusList.size()]),
						"Errors saving reports", null);
			}
		}

		return Status.OK_STATUS;
	}

	private static final String FEATURE_TAG = "features";
	private static final String UNSPECIFIED_FEATURE = "unspecified";

	private static Q7Info getQ7Info(Node node) {
		if (!node.getProperties().containsKey(IQ7ReportConstants.ROOT)) {
			return null;
		}
		return (Q7Info) node.getProperties().get(IQ7ReportConstants.ROOT);

	}

	/**
	 * Returns time in seconds
	 * 
	 * @param node
	 * @return
	 */
	private static String getDurationString(Node node) {
		long duration = getLastEndTime(node) - node.getStartTime();
		return String.format("%d.%d", duration / 1000, duration % 1000);
	}

	/**
	 * Recursively searches for the last node which has correct end time.
	 * 
	 * @param node
	 * @return
	 */
	private static long getLastEndTime(Node node) {
		if (node.getEndTime() != -1) {
			return node.getEndTime();
		}

		List<Node> children = node.getChildren();
		if (node.getChildren().isEmpty()) {
			return -1;
		}

		return getLastEndTime(children.get(children.size() - 1));
	}

	private static Node[] getContextNodes(Node node) {
		List<Node> result = new ArrayList<Node>();
		for (Node child : node.getChildren()) {
			Q7Info info = getQ7Info(child);
			if (info != null && info.getType() == ItemKind.CONTEXT) {
				result.add(child);
			}
		}
		return result.toArray(new Node[result.size()]);
	}

	private static Node getScriptNode(Node node) {
		for (Node child : node.getChildren()) {
			Q7Info info = getQ7Info(child);
			if (info != null && info.getType() == ItemKind.SCRIPT) {
				return child;
			}
		}
		return null;
	}

	/**
	 * Find first child with failed status and non-empty message
	 * 
	 * @param node
	 * @return
	 */
	private static String getFailMessage(Node node) {
		Q7Info info = getQ7Info(node);
		if (info == null) {
			return null;
		}
		if (info.getResult() != ResultStatus.FAIL) {
			return null;
		}

		String message = info.getMessage();
		String childMessage = null;

		for (Node child : node.getChildren()) {
			String result = getFailMessage(child);
			if (result != null) {
				childMessage = result;
				break;
			}
		}

		if (childMessage != null && info.getType() == ItemKind.ECL_COMMAND) {
			// special case - when we have nested commands, we want a fail
			// message only
			// from the bottom command
			return childMessage;
		}

		if (message == null) {
			return childMessage;
		}

		if (childMessage == null) {
			return message;
		}

		return String.format("%s\nCaused by: %s", message, childMessage);
	}

	private static String getFeatureName(Report report) {
		Q7Info info = getQ7Info(report.getRoot());
		if (info.getTags() == null || info.getTags().length() == 0) {
			return UNSPECIFIED_FEATURE;
		}

		for (String tag : info.getTags().split("[,;\\:]")) {
			tag = tag.trim();
			// Split tag on components
			String[] tagPath = tag.split("/");
			if (tagPath.length < 2 || !tagPath[0].equals(FEATURE_TAG)) {
				continue; // tag is not hierarchical or does not start with
							// FEATURE_TAG
			}

			// join the rest of tag path with dot
			// so that features/ui/editor -> ui.editor
			StringBuilder sb = new StringBuilder();
			for (int i = 1; i < tagPath.length; i++) {
				sb.append(tagPath[i]);
				if (i != tagPath.length - 1) {
					sb.append(".");
				}
			}
			return sb.toString();
		}
		return UNSPECIFIED_FEATURE;
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
			tf.setAttribute("indent-number", 4);
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

	private static final String ELEMENT_ROOT = "testcases";
	private static final String ELEMENT_TEST_CASE = "testcase";
	private static final String ELEMENT_DESCRIPTION = "description";
	private static final String ELEMENT_TAGS = "tags";
	private static final String ELEMENT_TAG = "tag";
	private static final String ELEMENT_CONTEXTS = "contexts";
	private static final String ELEMENT_CONTEXT = "context";
	private static final String ELEMENT_SCRIPT = "script";
	private static final String ELEMENT_MESSAGE = "message";

	private static final String ATTR_TOTAL = "total";
	private static final String ATTR_PASSED = "passed";
	private static final String ATTR_FAILED = "failed";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_STATUS = "status";
	private static final String ATTR_DURATION = "duration";
	private static final String ATTR_START_DATE = "date";

	private class FeatureContainer {
		public FeatureContainer(String featureName, IContentFactory factory)
				throws CoreException {
			this.destination = factory.createFileStream(String.format("%s.xml",
					featureName));
			this.document = getBuilder().newDocument();
			this.root = document.createElement(ELEMENT_ROOT);
			document.appendChild(root);
		}

		public void addReport(Report report) {
			Element testcaseElement = document.createElement(ELEMENT_TEST_CASE);
			Node reportRoot = report.getRoot();
			Q7Info info = getQ7Info(reportRoot);
			root.appendChild(testcaseElement);
			testcaseElement.setAttribute(ATTR_NAME, reportRoot.getName());
			setResult(testcaseElement, reportRoot);
			setDuration(testcaseElement, reportRoot);
			addDescription(testcaseElement, info);
			addTags(testcaseElement, info);
			addContexts(testcaseElement, reportRoot);
			addScript(testcaseElement, reportRoot);
			setExecutionDate(testcaseElement, reportRoot);
			total++;
			if (info.getResult() == ResultStatus.PASS) {
				passed++;
			}
		}

		private void setExecutionDate(Element element, Node node) {
			element.setAttribute(ATTR_START_DATE,
					new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
							.format(new Date(node.getStartTime())));
		}

		private void setResult(Element element, Node node) {
			Q7Info q7info = getQ7Info(node);

			element.setAttribute(ATTR_STATUS, q7info.getResult().toString());
			if (q7info.getResult() == ResultStatus.FAIL) {
				Element me = element.getOwnerDocument().createElement(
						ELEMENT_MESSAGE);
				me.setTextContent(getFailMessage(node));
				element.appendChild(me);
			}
			setDuration(element, node);
		}

		private void addScript(Element tce, Node node) {
			Node scriptNode = getScriptNode(node);
			if (scriptNode == null) {
				return;
			}

			Element scriptElement = tce.getOwnerDocument().createElement(
					ELEMENT_SCRIPT);
			tce.appendChild(scriptElement);
			setResult(scriptElement, scriptNode);
		}

		private void addContexts(Element tce, Node node) {
			Node[] contextNodes = getContextNodes(node);
			if (contextNodes.length == 0) {
				return;
			}

			Element contexts = tce.getOwnerDocument().createElement(
					ELEMENT_CONTEXTS);
			tce.appendChild(contexts);
			for (Node context : contextNodes) {
				Element contextElement = tce.getOwnerDocument().createElement(
						ELEMENT_CONTEXT);
				setResult(contextElement, context);
				contextElement.setAttribute(ATTR_NAME, context.getName());
				contexts.appendChild(contextElement);
			}
		}

		private void setDuration(Element tce, Node node) {
			tce.setAttribute(ATTR_DURATION, getDurationString(node));
		}

		private void addDescription(Element tce, Q7Info info) {
			Element description = document.createElement(ELEMENT_DESCRIPTION);
			description.setTextContent(info.getDescription());
			tce.appendChild(description);
		}

		private void addTags(Element tce, Q7Info info) {
			Element tags = document.createElement(ELEMENT_TAGS);
			tce.appendChild(tags);
			if (info.getTags() == null || info.getTags().length() == 0) {
				return;
			}

			for (String tag : info.getTags().split("[,;\\:]")) {
				Element te = document.createElement(ELEMENT_TAG);
				te.setTextContent(tag.trim());
				tags.appendChild(te);
			}
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
				StreamResult result = new StreamResult(new OutputStreamWriter(
						destination, "UTF-8"));
				getTransformer().transform(new DOMSource(document), result);
				destination.close();
			} catch (Exception e) {
				throw new CoreException(createErr(
						"Error saving result xml file", e));
			}
		}
	}
}
