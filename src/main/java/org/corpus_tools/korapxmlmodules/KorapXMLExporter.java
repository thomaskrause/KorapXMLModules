package org.corpus_tools.korapxmlmodules;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.common.PepperConfiguration;
import org.corpus_tools.pepper.impl.PepperExporterImpl;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.PepperExporter;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.PepperModule;
import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleNotReadyException;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.graph.Identifier;
import org.eclipse.emf.common.util.URI;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Thomas Krause
 */
//@formatter:off
@Component(name = "KorapXMLExporterComponent", factory = "PepperExporterComponentFactory")
//@formatter:on
public class KorapXMLExporter extends PepperExporterImpl implements PepperExporter
{

  private final static Logger log = LoggerFactory.getLogger(KorapXMLExporter.class);

  // =================================================== mandatory
  // ===================================================
  /**
   * <strong>OVERRIDE THIS METHOD FOR CUSTOMIZATION</strong>
   *
   * A constructor for your module. Set the coordinates, with which your module shall be registered. The coordinates
   * (modules name, version and supported formats) are a kind of a fingerprint, which should make your module unique.
   */
  public KorapXMLExporter()
  {
    super();
    setName("KorapXMLExporter");
    setSupplierContact(URI.createURI(PepperConfiguration.EMAIL));
    setSupplierHomepage(URI.createURI(PepperConfiguration.HOMEPAGE));
    setDesc("The exporter exports the corpus into a format named KorapXML as used by the Korap system "
      + "(see: http://github.org/korap/).");
    addSupportedFormat("KorapXML", "1.0", null);
    setDocumentEnding("xml");

    setExportMode(EXPORT_MODE.CORPORA_ONLY);
  }

  /**
   * This method creates a {@link PepperMapper}. <br/>
   * In this dummy implementation an instance of {@link KorapXMLMapper} is created and its location to where the
   * document-structure should be exported to is set.
   */
  @Override
  public PepperMapper createPepperMapper(Identifier Identifier)
  {
    PepperMapper mapper = new KorapXMLMapper();
    mapper.setResourceURI(getIdentifier2ResourceTable().get(Identifier));
    return (mapper);
  }

  public static class KorapXMLMapper extends PepperMapperImpl
  {

    public static final Namespace NS = Namespace.getNamespace("http://ids-mannheim.de/ns/KorAP");
    public static final String KORAP_VERSION = "KorAP-0.4";

    /**
     * Stores each document-structure to location given by {@link #getResourceURI()}.
     */
    @Override
    public DOCUMENT_STATUS mapSDocument()
    {
      // workaround to deal with a bug in Salt
      SCorpusGraph sCorpusGraph = getDocument().getGraph();

      File docDir = new File(getResourceURI().toFileString());

      mapText(docDir);
      mapToken(docDir);

      // workaround to deal with a bug in Salt
      if (getDocument().getGraph() == null)
      {
        getDocument().setGraph(sCorpusGraph);
      }

      addProgress(1.0);
      return (DOCUMENT_STATUS.COMPLETED);
    }

    private void mapText(File docDir)
    {
      try (FileWriter dataXMLWriter = new FileWriter(new File(docDir, "data.xml")))
      {
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());

        Document dataXML = new Document(new Element("raw_text", NS)
          .setAttribute("docid", getDocument().getId()));

        String rawText = "";
        if (getDocument().getDocumentGraph().getTextualDSs() != null
          && !getDocument().getDocumentGraph().getTextualDSs().isEmpty())
        {
          rawText = getDocument().getDocumentGraph().getTextualDSs().get(0).getText();
        }

        dataXML.getRootElement().addContent(new Element("text", NS).setContent(new Text(rawText)));

        outputter.output(dataXML, dataXMLWriter);
      }
      catch (IOException ex)
      {
        log.error("Could not create file \"data.xml\" for document " + getResourceURI(), ex);
      }
    }

    private void mapToken(File docDir)
    {
      File baseDir = new File(docDir, "base");
      if(!baseDir.exists())
      {
        if(!baseDir.exists() && !baseDir.mkdirs())
        {
          log.error("Can't create output folder for token file.");
          return;
          
        }
      }
      try (FileWriter tokenXMLWriter = new FileWriter(new File(baseDir, "token.xml")))
      {
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());

        Document tokenXML = new Document(new Element("layer", NS)
          .setAttribute("docid", getDocument().getId())
          .setAttribute("version", KORAP_VERSION));

        Element spanList = new Element("spanList", NS);
        tokenXML.getRootElement().addContent(spanList);

        getDocument().getDocumentGraph().getTextualRelations().forEach((textRel) ->
        {
          SToken tok = textRel.getSource();

          spanList.addContent(new Element("span", NS)
            .setAttribute("id", tok.getPath().fragment())
            .setAttribute("from", "" + textRel.getStart())
            .setAttribute("to", "" + textRel.getEnd()));
        });

        outputter.output(tokenXML, tokenXMLWriter);
      }
      catch (IOException ex)
      {
        log.error("Could not create file \"base/token.xml\" for document " + getResourceURI(), ex);
      }
    }

    /**
     * Storing the corpus-structure once
     */
    @Override
    public DOCUMENT_STATUS mapSCorpus()
    {
      List<SNode> roots = getCorpus().getGraph().getRoots();
      if ((roots != null) && (!roots.isEmpty()))
      {
        if (getCorpus().equals(roots.get(0)))
        {
//          SaltUtil.save_DOT(getCorpus().getGraph(), getResourceURI());
        }
      }

      return (DOCUMENT_STATUS.COMPLETED);
    }
  }

  @Override
  public void exportCorpusStructure()
  {
    // create the directory structure with a folder for each (sub-) corpus
    super.exportCorpusStructure();

    // add a folder for each document
    Collection<SCorpusGraph> corpGraphs = new LinkedList<>(this.getSaltProject().getCorpusGraphs());
    corpGraphs.forEach((cg) ->
    {
      cg.getCorpusDocumentRelations().forEach((rel) ->
      {
        URI parentLocation = getIdentifier2ResourceTable().get(rel.getSource().getIdentifier());
        File docFolder = new File(parentLocation.toFileString(), rel.getTarget().getName());
        if (docFolder.exists() || docFolder.mkdirs())
        {
          getIdentifier2ResourceTable().put(rel.getTarget().getIdentifier(),
            URI.createFileURI(docFolder.getAbsolutePath()));
        }
        else
        {
          logger.error("Could not create output directory {}", docFolder.getAbsolutePath());
        }
      });
    });
  }

  // =================================================== optional
  // ===================================================
  /**
   * <strong>OVERRIDE THIS METHOD FOR CUSTOMIZATION</strong>
   *
   * This method is called by the pepper framework after initializing this object and directly before start processing.
   * Initializing means setting properties {@link PepperModuleProperties}, setting temporary files, resources etc. .
   * returns false or throws an exception in case of {@link PepperModule} instance is not ready for any reason.
   *
   * @return false, {@link PepperModule} instance is not ready for any reason, true, else.
   */
  @Override
  public boolean isReadyToStart() throws PepperModuleNotReadyException
  {
    // TODO make some initializations if necessary
    return (super.isReadyToStart());
  }
}
