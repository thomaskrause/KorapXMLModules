/*
 * Copyright 2017 Humboldt-Universität zu Berlin.
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

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.corpus_tools.korapxmlmodules.KorapXMLExporterProperties;
import org.corpus_tools.salt.common.SDominanceRelation;
import org.corpus_tools.salt.common.SStructure;
import org.corpus_tools.salt.common.SStructuredNode;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;

/**
 *
 * @author Thomas Krause <thomaskrause@posteo.de>
 */
public class CoreNLP extends Foundry {

	@Override
	public void map(File textDir, Collection<SNode> nodes, STextualDS text, KorapXMLExporterProperties properties) {

		List<SStructure> layerStructs
				= nodes.parallelStream().filter(SStructure.class::isInstance)
						.map(n -> (SStructure) n)
						.collect(Collectors.toList());

		mapSpans(textDir, "corenlp", "constituency", layerStructs, text, properties);

	}

	@Override
	public void mapRelations(SStructuredNode node, XMLStreamWriter xml, KorapXMLExporterProperties props) throws XMLStreamException {
		List<SRelation> outRels = node.getOutRelations();

		if (outRels != null) {
			for (SRelation rel : outRels) {
				if(rel instanceof SDominanceRelation) {
					mapRelation(rel, "dominates", "morpho", xml, props);
				}
			}
		}
	}

}
