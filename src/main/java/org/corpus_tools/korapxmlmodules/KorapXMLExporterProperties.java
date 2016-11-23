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
package org.corpus_tools.korapxmlmodules;

import com.google.common.base.Splitter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.corpus_tools.korapxmlmodules.foundries.Foundry;
import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;
import org.corpus_tools.salt.util.SaltUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Thomas Krause <thomaskrause@posteo.de>
 */
public class KorapXMLExporterProperties extends PepperModuleProperties {

	private static final Logger log = LoggerFactory.getLogger(KorapXMLExporterProperties.class);
	
	public KorapXMLExporterProperties() {
		KorapXMLExporterProperties.this.addProperty(new PepperModuleProperty<>(
				"base.sentence", String.class,
				"The sentence annotation used as \"base#sentences\" layer in KorapXML",
				SaltUtil.SALT_NAMESPACE + "::" + SaltUtil.SEMANTICS_SENTENCE));

		KorapXMLExporterProperties.this.addProperty(new PepperModuleProperty<>(
				"base.paragraph", String.class,
				"The paragraph annotation used as \"base#paragraph\" layer in KorapXML",
				SaltUtil.SALT_NAMESPACE + "::paragraph"));

		KorapXMLExporterProperties.this.addProperty(new PepperModuleProperty<>(
				"foundryMapping", String.class,
				"Maps a layer to a foundry.", ""));
	}

	public String getSentenceAnnotationQName() {
		return ((PepperModuleProperty<String>) getProperty("base.sentence")).getValue();
	}

	public String getParagraphAnnotationQName() {
		return ((PepperModuleProperty<String>) getProperty("base.paragraph")).getValue();
	}
	
	public Map<String, Foundry> getFoundryMapping() {
		Map<String, Foundry> result = new TreeMap<>();
		
		String raw = ((PepperModuleProperty<String>) getProperty("foundryMapping")).getValue();
		if(raw != null && !raw.isEmpty()) {
			for(String def : Splitter.on(',').trimResults().omitEmptyStrings().split(raw)) {
				List<String> splittedDef = Splitter.on("->").trimResults().omitEmptyStrings().limit(2).splitToList(def);
				if(splittedDef.size() == 2) {
					try {
						Class<?> rawClass = Class.forName("org.corpus_tools.korapxmlmodules.foundries." + splittedDef.get(1));
						Class<? extends Foundry> foundryClass = rawClass.asSubclass(Foundry.class);
						result.put(splittedDef.get(0), foundryClass.newInstance());
						
					} catch (ClassNotFoundException | ClassCastException | InstantiationException | IllegalAccessException ex ) {
						log.warn("Can't find the foundry with name {}: {}", splittedDef.get(1), ex.getMessage());
					}
				}
			}
		}
		return result;
	}

}
