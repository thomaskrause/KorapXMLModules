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

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.corpus_tools.korapxmlmodules.KorapXMLExporterProperties;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SNode;

/**
 *
 * @author Thomas Krause <thomaskrause@posteo.de>
 */
public class TreeTagger extends Foundry {

	@Override
	public void map(File textDir, Collection<SNode> nodes, STextualDS text, KorapXMLExporterProperties properties) {

		String lemmaQName = properties.getTreeTaggerLemma();
		String posQName = properties.getTreeTaggerPOS();

		List<SToken> tokenWithAnno
				= nodes.parallelStream().filter(SToken.class::isInstance)
						.map(n -> (SToken) n)
						.filter(tok -> tok.getAnnotation(lemmaQName) != null || tok.getAnnotation(posQName) != null)
						.collect(Collectors.toList());

		mapSpans(textDir, "tree_tagger", "morpho", tokenWithAnno, text);

	}

	@Override
	public void mapAnnotations(Collection<SAnnotation> annotations, XMLStreamWriter xml) throws XMLStreamException {
		mapWrappedAnnotations(annotations, "lex", xml);
	}
	
	
}
