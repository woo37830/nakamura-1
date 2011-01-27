/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.message;


import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessagingService;
import org.sakaiproject.nakamura.api.search.SearchPropertyProvider;

import java.util.Map;

import javax.jcr.Session;

/**
 * Provides properties to process the search
 */
@Component(immediate = true, label = "MessageSearchPropertyProvider", description = "Provides some message search properties.")
@Service
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Provides some message search properties."),
    @Property(name = "sakai.search.provider", value = "Message") })
public class MessageSearchPropertyProvider implements SearchPropertyProvider {

  @Reference
  protected transient MessagingService messagingService;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.SearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    String user = request.getRemoteUser();
    Session session = request.getResourceResolver().adaptTo(Session.class);
    propertiesMap.put(MessageConstants.SEARCH_PROP_MESSAGESTORE, ClientUtils
        .escapeQueryChars(messagingService.getFullPathToStore(user, session)));

    RequestParameter address = request.getRequestParameter("address");
    if (address != null && !address.getString().equals("")) {
      // resolve the address by finding the authorizables.
      String addressString = address.getString();
      String storePath = messagingService.getFullPathToStore(addressString, session);
      propertiesMap.put(MessageConstants.SEARCH_PROP_MESSAGESTORE, ClientUtils
              .escapeQueryChars(storePath));
    }

    RequestParameter usersParam = request.getRequestParameter("_from");
    if (usersParam != null && !usersParam.getString().equals("")) {
      String[] users = StringUtils.split(usersParam.getString(), ',');
    	
      StringBuilder solrQuery = new StringBuilder();
      String solrOrSyntax = " OR ";

      //build solr query 
      solrQuery.append("from:(");
      for (String u : users) {
    	  solrQuery.append(ClientUtils.escapeQueryChars('"' + u + '"'));
    	  solrQuery.append(solrOrSyntax);
      }
      // remove trailing "solr OR syntax"
      if (users.length > 0) {
    	  solrQuery.delete(solrQuery.length() - solrOrSyntax.length(), solrQuery.length());
      }
      solrQuery.append(")");
      
      propertiesMap.put("_from", solrQuery.toString());
    }
  }
}
