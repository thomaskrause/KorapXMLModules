package org.corpus_tools.korapxmlmodules;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.commons.lang3.tuple.Pair;
import org.corpus_tools.korapxmlmodules.foundries.Foundry;

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.common.PepperConfiguration;
import org.corpus_tools.pepper.exceptions.PepperConvertException;
import org.corpus_tools.pepper.impl.PepperExporterImpl;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.PepperExporter;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.PepperModule;
import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleNotReadyException;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SSpanningRelation;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STextualRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.graph.Identifier;
import org.corpus_tools.salt.util.DataSourceSequence;
import org.corpus_tools.salt.util.SaltUtil;
import org.eclipse.emf.common.util.URI;
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
public class KorapXMLExporter extends PepperExporterImpl implements PepperExporter {

	private final static Logger log = LoggerFactory.getLogger(KorapXMLExporter.class);

	public static final XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

	// =================================================== mandatory
	// ===================================================
	/**
	 * <strong>OVERRIDE THIS METHOD FOR CUSTOMIZATION</strong>
	 *
	 * A constructor for your module. Set the coordinates, with which your module shall be registered. The coordinates
	 * (modules name, version and supported formats) are a kind of a fingerprint, which should make your module unique.
	 */
	public KorapXMLExporter() {
		super();
		setName("KorapXMLExporter");
		setSupplierContact(URI.createURI(PepperConfiguration.EMAIL));
		setSupplierHomepage(URI.createURI(PepperConfiguration.HOMEPAGE));
		setDesc("The exporter exports the corpus into a format named KorapXML as used by the Korap system "
				+ "(see: http://github.org/korap/).");
		addSupportedFormat("KorapXML", "1.0", null);
		setDocumentEnding("xml");

		setExportMode(EXPORT_MODE.CORPORA_ONLY);

		setProperties(new KorapXMLExporterProperties());

		outputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, "true");

	}

	/**
	 * This method creates a {@link PepperMapper}. <br/>
	 * In this dummy implementation an instance of {@link KorapXMLMapper} is created and its location to where the
	 * document-structure should be exported to is set.
	 */
	@Override
	public PepperMapper createPepperMapper(Identifier Identifier) {
		PepperMapper mapper = new KorapXMLMapper();
		mapper.setResourceURI(getIdentifier2ResourceTable().get(Identifier));
		return (mapper);
	}

	public static class KorapXMLMapper extends PepperMapperImpl {

		public static final String NS_URI = "http://ids-mannheim.de/ns/KorAP";
		public static final String KORAP_VERSION = "KorAP-0.4";

		private static class AnnoSpanPair {

			final String anno;
			final SSpan span;

			public AnnoSpanPair(String anno, SSpan span) {
				this.anno = anno;
				this.span = span;
			}
		}

		/**
		 * Stores each document-structure to location given by {@link #getResourceURI()}.
		 */
		@Override
		public DOCUMENT_STATUS mapSDocument() {
			// workaround to deal with a bug in Salt
			SCorpusGraph sCorpusGraph = getDocument().getGraph();

			File docDir = new File(getResourceURI().toFileString());

			Map<String, Foundry> foundryMapping = getProperties().getFoundryMapping();

			getDocument().getDocumentGraph().getTextualDSs().forEach((text)
					-> {
				File textDir = new File(docDir, text.getName().replaceAll("[._]", ""));
				if (!textDir.exists() && !textDir.mkdirs()) {
					throw new PepperConvertException("Can't create directory " + textDir.getAbsolutePath());
				}
				mapText(textDir, text);
				mapToken(textDir, text);

				foundryMapping.forEach((layerName, foundry) -> {
					List<SLayer> layerList = getDocument().getDocumentGraph().getLayerByName(layerName);
					if(layerList != null) {
						for(SLayer layer : layerList) {
							foundry.map(textDir, layer, text, getProperties());
						}
					}
				});

			});

			// workaround to deal with a bug in Salt
			if (getDocument().getGraph() == null) {
				getDocument().setGraph(sCorpusGraph);
			}

			addProgress(1.0);
			return (DOCUMENT_STATUS.COMPLETED);
		}

		private String getDocID(STextualDS text) {
			String textName = text.getName();
			String[] docPath = getDocument().getPath().segments();

			if (docPath.length >= 2) {
				return docPath[0].replaceAll("[._]", "") + "_" + docPath[docPath.length - 1].replaceAll("[._]", "") + "." + textName.replaceAll("[._]", "");
			} else {
				throw new PepperConvertException("Can't generate a valid document ID because the corpus path is invalid.");
			}
		}

		private void mapText(File textDir, STextualDS text) {

			try (
					FileOutputStream dataXMLStream = new FileOutputStream(new File(textDir, "data.xml"))) {
				XMLStreamWriter xml = outputFactory.createXMLStreamWriter(dataXMLStream, "UTF-8");

				xml.writeStartDocument("UTF-8", "1.0");
				xml.setDefaultNamespace(NS_URI);

				xml.writeStartElement(NS_URI, "raw_text");

				xml.writeAttribute("docid", getDocID(text));

				String textContent = text.getText();

				xml.writeStartElement(NS_URI, "text");
				xml.writeCharacters(textContent);
				xml.writeEndElement(); // end "text"
				xml.writeEndElement(); // end "raw_text"
				xml.writeEndDocument();

				xml.flush();
				xml.close();

			} catch (IOException | XMLStreamException ex) {
				log.error("Could not create file \"data.xml\" for document " + getResourceURI(), ex);
			}
		}

		private void mapToken(File textDir, STextualDS text) {
			File baseDir = new File(textDir, "base");
			if (!baseDir.exists()) {
				if (!baseDir.exists() && !baseDir.mkdirs()) {
					log.error("Can't create output folder for token file.");
					return;

				}
			}
			try (FileOutputStream tokenXMLStream = new FileOutputStream(new File(baseDir, "token.xml"))) {
				XMLStreamWriter xml = outputFactory.createXMLStreamWriter(tokenXMLStream, "UTF-8");
				xml.writeStartDocument("UTF-8", "1.0");
				xml.setDefaultNamespace(NS_URI);

				xml.writeStartElement(NS_URI, "layer");
				xml.writeAttribute("docid", getDocID(text));
				xml.writeAttribute("version", KORAP_VERSION);

				xml.writeStartElement(NS_URI, "spanList");

				getDocument().getDocumentGraph().getTextualRelations().forEach((textRel)
						-> {
					if (textRel.getTarget() == text) {
						SToken tok = textRel.getSource();

						try {
							xml.writeStartElement(NS_URI, "span");
							xml.writeAttribute("id", tok.getPath().fragment());
							xml.writeAttribute("from", "" + textRel.getStart());
							xml.writeAttribute("to", "" + textRel.getEnd());
							xml.writeEndElement(); // end span
						} catch (XMLStreamException ex) {
							log.error("Could not map token " + tok.getId(), ex);
						}
					}
				});

				xml.writeEndElement(); // end "spanList"
				xml.writeEndElement(); // end "layer"
				xml.writeEndDocument();

				xml.flush();
				xml.close();

			} catch (IOException | XMLStreamException ex) {
				log.error("Could not create file \"base/token.xml\" for document " + getResourceURI(), ex);
			}
		}

		/**
		 * Storing the corpus-structure once
		 */
		@Override
		public DOCUMENT_STATUS mapSCorpus() {
			List<SNode> roots = getCorpus().getGraph().getRoots();
			if ((roots != null) && (!roots.isEmpty())) {
				if (getCorpus().equals(roots.get(0))) {
//          SaltUtil.save_DOT(getCorpus().getGraph(), getResourceURI());
				}
			}

			return (DOCUMENT_STATUS.COMPLETED);
		}

		@Override
		public KorapXMLExporterProperties getProperties() {
			return (KorapXMLExporterProperties) super.getProperties();
		}

	}

	@Override
	public void exportCorpusStructure() {
		// create the directory structure with a folder for each (sub-) corpus
		super.exportCorpusStructure();

		// add a folder for each document
		Collection<SCorpusGraph> corpGraphs = new LinkedList<>(this.getSaltProject().getCorpusGraphs());
		corpGraphs.forEach((cg)
				-> {
			cg.getCorpusDocumentRelations().forEach((rel)
					-> {
				URI parentLocation = getIdentifier2ResourceTable().get(rel.getSource().getIdentifier());
				File docFolder = new File(parentLocation.toFileString(), rel.getTarget().getName().replaceAll("[._]", ""));
				if (docFolder.exists() || docFolder.mkdirs()) {
					getIdentifier2ResourceTable().put(rel.getTarget().getIdentifier(),
							URI.createFileURI(docFolder.getAbsolutePath()));
				} else {
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
	 * This method is called by the pepper framework after initializing this object and directly before start
	 * processing. Initializing means setting properties {@link PepperModuleProperties}, setting temporary files,
	 * resources etc. . returns false or throws an exception in case of {@link PepperModule} instance is not ready for
	 * any reason.
	 *
	 * @return false, {@link PepperModule} instance is not ready for any reason, true, else.
	 */
	@Override
	public boolean isReadyToStart() throws PepperModuleNotReadyException {
		// TODO make some initializations if necessary
		return (super.isReadyToStart());
	}

}
