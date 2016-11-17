package org.corpus_tools.korapxmlmodules;

import org.corpus_tools.korapxmlmodules.KorapXMLExporter;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.corpus_tools.pepper.common.CorpusDesc;
import org.corpus_tools.pepper.common.FormatDesc;
import org.corpus_tools.pepper.testFramework.PepperExporterTest;
import org.corpus_tools.salt.samples.SampleGenerator;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * This is a dummy implementation of a JUnit test for testing the
 * {@link KorapXMLExporter} class. Feel free to adapt and enhance this test class
 * for real tests to check the work of your importer. If you are not confirm
 * with JUnit, please have a look at <a
 * href="http://www.vogella.com/tutorials/JUnit/article.html">
 * http://www.vogella.com/tutorials/JUnit/article.html</a>. <br/>
 * Please note, that the test class is derived from {@link PepperExporterTest}.
 * The usage of this class should simplfy your work and allows you to test only
 * your single importer in the Pepper environment.
 * 
 * @author Thomas Krause
 */
public class KorapXMLExporterTest extends PepperExporterTest {
	/**
	 * This method is called by the JUnit environment each time before a test
	 * case starts. So each time a method annotated with @Test is called. This
	 * enables, that each method could run in its own environment being not
	 * influenced by before or after running test cases.
	 */
	@Before
	public void setUp() {
		setFixture(new KorapXMLExporter());

		FormatDesc formatDef = new FormatDesc();
		formatDef.setFormatName("KorapXML");
		formatDef.setFormatVersion("1.0");
		this.supportedFormatsCheck.add(formatDef);
	}
  
  @Test
  public void test_exportCorpusStructure()
  {
    // create a sample corpus, the class SampleGenerator provides a bunch of
		// helpful methods to create sample documents and corpora
		getFixture().setSaltProject(SampleGenerator.createSaltProject());

		// determine location, to where the corpus should be exported
		getFixture().setCorpusDesc(new CorpusDesc().setCorpusPath(URI.createFileURI(getTempPath("KorapXMLExporter").getAbsolutePath())));
  
    // starts the Pepper framework and the conversion process
		start();

		File superCorpus = new File(getTempPath("KorapXMLExporter").getAbsolutePath() + "/rootCorpus");
		assertTrue(superCorpus.exists());
    
		File subCorpus1 = new File(superCorpus.getAbsolutePath() + "/subCorpus1");
		assertTrue(subCorpus1.exists());
    assertTrue(subCorpus1.isDirectory());
		File subCorpus2 = new File(superCorpus.getAbsolutePath() + "/subCorpus2");
		assertTrue(subCorpus2.exists());
    assertTrue(subCorpus2.isDirectory());
    
    File doc1 = new File(subCorpus1, "doc1");
    assertTrue(doc1.isDirectory());
    
    File doc2 = new File(subCorpus1, "doc2");
    assertTrue(doc2.isDirectory());
    
    File doc3 = new File(subCorpus2, "doc3");
    assertTrue(doc3.isDirectory());
    
    File doc4 = new File(subCorpus2, "doc4");
    assertTrue(doc4.isDirectory());
    
  }

}
