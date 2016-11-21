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

import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;
import org.corpus_tools.salt.util.SaltUtil;

/**
 *
 * @author Thomas Krause <thomaskrause@posteo.de>
 */
public class KorapXMLExporterProperties extends PepperModuleProperties
{

  public KorapXMLExporterProperties()
  {
    KorapXMLExporterProperties.this.addProperty(new PepperModuleProperty<>(
      "sentenceAnnotation", String.class,
      "The sentence annotation used as \"base#sentences\" layer in KorapXML",
      SaltUtil.SALT_NAMESPACE + "::" + SaltUtil.SEMANTICS_SENTENCE));
    
    KorapXMLExporterProperties.this.addProperty(new PepperModuleProperty<>(
      "paragraphAnnotation", String.class,
      "The paragraph annotation used as \"base#paragraph\" layer in KorapXML",
      SaltUtil.SALT_NAMESPACE + "::paragraph"));
  }

  public String getSentenceAnnotationQName()
  {
    return ((PepperModuleProperty<String>) getProperty("sentenceAnnotation")).getValue();
  }
  
  public String getParagraphAnnotationQName()
  {
    return ((PepperModuleProperty<String>) getProperty("paragraphAnnotation")).getValue();
  }

}
