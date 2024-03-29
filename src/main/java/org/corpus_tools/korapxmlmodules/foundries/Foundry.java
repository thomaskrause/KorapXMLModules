/*
 * Copyright 2016 Humboldt-Universität zu Berlin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.corpus_tools.korapxmlmodules.foundries;

import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import static org.corpus_tools.korapxmlmodules.KorapXMLExporter.KorapXMLMapper.KORAP_VERSION;
import static org.corpus_tools.korapxmlmodules.KorapXMLExporter.KorapXMLMapper.NS_URI;
import static org.corpus_tools.korapxmlmodules.KorapXMLExporter.outputFactory;
import org.corpus_tools.korapxmlmodules.KorapXMLExporterProperties;
import org.corpus_tools.pepper.exceptions.PepperConvertException;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.common.SStructuredNode;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.util.DataSourceSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Thomas Krause <thomaskrause@posteo.de>
 */
public abstract class Foundry {

	private static final Logger log = LoggerFactory.getLogger(Foundry.class);

	public abstract void map(File textDir, Collection<SNode> nodes, STextualDS text, KorapXMLExporterProperties properties);

	protected String getDocID(STextualDS text) {
		String textName = text.getName();
		String[] docPath = text.getGraph().getPath().segments();

		if (docPath.length >= 2) {
			return docPath[0].replaceAll("[._]", "") + "_" + docPath[docPath.length - 1].replaceAll("[._]", "") + "." + textName.replaceAll("[._]", "");
		} else {
			throw new PepperConvertException("Can't generate a valid document ID because the corpus path is invalid.");
		}
	}

	protected void mapSpans(File textDir, String foundry, String annoName,
			Collection<? extends SStructuredNode> nodes,
			STextualDS text, KorapXMLExporterProperties props) {
		if (nodes == null || nodes.isEmpty()) {
			log.warn("Nothing to map for span layer \"" + foundry + "#" + annoName + "\" in text " + text.getId());
			return;
		}

		File foundryDir = new File(textDir, foundry);
		if (!foundryDir.exists()) {
			if (!foundryDir.exists() && !foundryDir.mkdirs()) {
				log.error("Can't create output folder for foundry \"" + foundry + "\".");
				return;

			}
		}
		File outFile = new File(foundryDir, annoName + ".xml");
		try (FileOutputStream tokenXMLStream = new FileOutputStream(outFile)) {
			XMLStreamWriter xml = outputFactory.createXMLStreamWriter(tokenXMLStream, "UTF-8");
			xml.writeStartDocument("UTF-8", "1.0");
			xml.setDefaultNamespace(NS_URI);

			indent(0, xml);
			xml.writeStartElement(NS_URI, "layer");
			xml.writeAttribute("docid", getDocID(text));
			xml.writeAttribute("version", KORAP_VERSION);

			indent(1, xml);
			xml.writeStartElement(NS_URI, "spanList");

			nodes.forEach((node)
					-> {

				List<DataSourceSequence> sequences
						= text.getGraph().getOverlappedDataSourceSequence(node, SALT_TYPE.SSPANNING_RELATION,
								SALT_TYPE.STEXT_OVERLAPPING_RELATION);

				if (sequences.size() == 1) {

					try {

						indent(2, xml);
						xml.writeStartElement(NS_URI, "span");
						xml.writeAttribute("id", node.getPath().fragment());
						xml.writeAttribute("from", "" + sequences.get(0).getStart());
						xml.writeAttribute("to", "" + sequences.get(0).getEnd());

						mapAnnotations(node.getAnnotations(), xml, props);
						mapRelations(node, xml, props);

						indent(2, xml);
						xml.writeEndElement(); // </span>
					} catch (XMLStreamException ex) {
						log.error("Could not map span " + node.getId(), ex);
					}
				} else {
					log.warn("Invalid size " + sequences.size() + " of data source sequences for span " + node.getId());
				}

			});

			indent(1, xml);
			xml.writeEndElement(); // end "spanList"
			indent(0, xml);
			xml.writeEndElement(); // end "layer"
			xml.writeEndDocument();

			xml.flush();
			xml.close();

		} catch (IOException | XMLStreamException ex) {
			log.error("Could not create file \"" + foundry + "/" + annoName + ".xml\" for document " + text.getGraph().getId(), ex);
		}

	}
	
	protected void mapRelation(SRelation<?,?> rel, 
			String label, String tokenFile,
			XMLStreamWriter xml, KorapXMLExporterProperties props)
		throws XMLStreamException {
		
		indent(3, xml);
		xml.writeStartElement(NS_URI, "rel");
		xml.writeAttribute("label", label);
		String target = rel.getTarget().getPath().fragment();
		if(rel.getTarget() instanceof SToken) {
			// the token in in another file
			target = tokenFile + "#" + target;
		}
		xml.writeAttribute("target", target);
		xml.writeEndElement();
	}
	
	public void mapRelations(SStructuredNode node, XMLStreamWriter xml, KorapXMLExporterProperties props)
		throws XMLStreamException {
		// default is not to map any relation
	}

	public void mapAnnotations(Collection<SAnnotation> annotations, XMLStreamWriter xml, KorapXMLExporterProperties props) throws XMLStreamException {
		mapDirectAnnotations(annotations, xml);
	}

	protected final void mapDirectAnnotations(Collection<SAnnotation> annotations, XMLStreamWriter xml) throws XMLStreamException {
		if (xml != null && annotations != null && !annotations.isEmpty()) {

			// group the annotations by their namespace (this will become the type of the feature structure)
			Multimap<String, SAnnotation> annosByNamspace
					= Multimaps.index(annotations, anno -> anno.getNamespace() == null ? "" : anno.getNamespace());
			// write a feature structure for each namespace
			for (Map.Entry<String, Collection<SAnnotation>> entry : annosByNamspace.asMap().entrySet()) {

				indent(3, xml);
				xml.writeStartElement(NS_URI, "fs");
				xml.writeAttribute("type", entry.getKey());
				for (SAnnotation anno : entry.getValue()) {
					indent(4, xml);
					xml.writeStartElement(NS_URI, "f");
					xml.writeAttribute("name", anno.getName());
					xml.writeCharacters(anno.getValue_STEXT());
					xml.writeEndElement(); // </f>
				}
				indent(3, xml);
				xml.writeEndElement(); // </fs>
			}

		}
	}

	protected final void mapWrappedAnnotations(Collection<SAnnotation> annotations, String type, XMLStreamWriter xml) throws XMLStreamException {
		if (xml != null && annotations != null && !annotations.isEmpty()) {

			// group the annotations by their namespace (this will become the type of the feature structure)
			Multimap<String, SAnnotation> annosByNamspace
					= Multimaps.index(annotations, anno -> anno.getNamespace() == null ? "" : anno.getNamespace());
			// write a feature structure for each namespace
			for (Map.Entry<String, Collection<SAnnotation>> entry : annosByNamspace.asMap().entrySet()) {

				indent(3, xml);
				xml.writeStartElement(NS_URI, "fs");
				xml.writeAttribute("type", type);

				indent(4, xml);
				xml.writeStartElement(NS_URI, "f");
				xml.writeAttribute("name", type);

				indent(5, xml);
				xml.writeStartElement(NS_URI, "fs");
				xml.writeAttribute("type", entry.getKey());
				for (SAnnotation anno : entry.getValue()) {
					indent(6, xml);
					xml.writeStartElement(NS_URI, "f");
					xml.writeAttribute("name", anno.getName());
					xml.writeCharacters(anno.getValue_STEXT());
					xml.writeEndElement(); // </f>
				}
				indent(5, xml);
				xml.writeEndElement(); // </fs>
				indent(4, xml);
				xml.writeEndElement(); // </f>
				indent(3, xml);
				xml.writeEndElement(); // </fs>
			}

		}
	}

	protected final void indent(int nr, XMLStreamWriter xml) {
		try {
			xml.writeCharacters("\n" + Strings.repeat("\t", nr));
		} catch (XMLStreamException ex) {
			java.util.logging.Logger.getLogger(Foundry.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
