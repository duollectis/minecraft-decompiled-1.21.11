package net.minecraft.test;

import com.google.common.base.Stopwatch;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Слушатель завершения тестов, записывающий результаты в XML-отчёт формата JUnit.
 * Отчёт сохраняется при вызове {@link #onStopped()} в файл, переданный в конструктор.
 */
public class XmlReportingTestCompletionListener implements TestCompletionListener {

	private final Document document;
	private final Element testSuiteElement;
	private final Stopwatch stopwatch;
	private final File file;

	public XmlReportingTestCompletionListener(File file) throws ParserConfigurationException {
		this.file = file;
		document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		testSuiteElement = document.createElement("testsuite");
		Element root = document.createElement("testsuite");
		root.appendChild(testSuiteElement);
		document.appendChild(root);
		testSuiteElement.setAttribute("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
		stopwatch = Stopwatch.createStarted();
	}

	private Element addTestCase(GameTestState test, String name) {
		Element testCase = document.createElement("testcase");
		testCase.setAttribute("name", name);
		testCase.setAttribute("classname", test.getStructure().toString());
		testCase.setAttribute("time", String.valueOf(test.getElapsedMilliseconds() / 1000.0));
		testSuiteElement.appendChild(testCase);
		return testCase;
	}

	@Override
	public void onTestFailed(GameTestState test) {
		String testId = test.getId().toString();
		String errorMessage = test.getThrowable().getMessage();
		Element failureElement = document.createElement(test.isRequired() ? "failure" : "skipped");
		failureElement.setAttribute("message", "(" + test.getPos().toShortString() + ") " + errorMessage);
		Element testCase = addTestCase(test, testId);
		testCase.appendChild(failureElement);
	}

	@Override
	public void onTestPassed(GameTestState test) {
		addTestCase(test, test.getId().toString());
	}

	@Override
	public void onStopped() {
		stopwatch.stop();
		testSuiteElement.setAttribute(
			"time",
			String.valueOf(stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0)
		);

		try {
			saveReport(file);
		} catch (TransformerException ex) {
			throw new Error("Couldn't save test report", ex);
		}
	}

	public void saveReport(File reportFile) throws TransformerException {
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.transform(new DOMSource(document), new StreamResult(reportFile));
	}
}
