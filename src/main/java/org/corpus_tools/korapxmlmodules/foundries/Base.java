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
import org.corpus_tools.korapxmlmodules.KorapXMLExporterProperties;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.core.SNode;

/**
 *
 * @author Thomas Krause <thomaskrause@posteo.de>
 */
public class Base extends Foundry {

	@Override
	public void map(File textDir, Collection<SNode> nodes, STextualDS text, KorapXMLExporterProperties properties) {

		List<SSpan> layerSpans
				= nodes.parallelStream().filter(SSpan.class::isInstance)
						.map(n -> (SSpan) n)
						.collect(Collectors.toList());

		String sentenceAnnotationQName = properties.getBaseSentence();
		List<SSpan> sentenceSpans = layerSpans.parallelStream()
				.filter(span -> span.getAnnotation(sentenceAnnotationQName) != null)
				.collect(Collectors.toList());

		String paragraphAnnotationQName = properties.getBaseParagraph();
		List<SSpan> paragraphSpans = layerSpans.parallelStream()
				.filter(span -> span.getAnnotation(paragraphAnnotationQName) != null)
				.collect(Collectors.toList());

		// map all sentence spans
		mapSpans(textDir, "base", "sentences", sentenceSpans, text, properties);

		// map all paragraph spans
		mapSpans(textDir, "base", "paragraph", paragraphSpans, text, properties);
	}

}
